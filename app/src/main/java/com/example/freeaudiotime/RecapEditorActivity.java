    package com.example.freeaudiotime;
    
    import android.app.AlertDialog;
    import android.content.ContentValues;
    import android.content.Intent;
    import android.graphics.Bitmap;
    import android.graphics.BitmapFactory;
    import android.graphics.Color;
    import android.net.Uri;
    import android.os.Build;
    import android.os.Bundle;
    import android.os.Environment;
    import android.provider.MediaStore;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
    import android.widget.TextView;
    import android.widget.Toast;
    import androidx.core.graphics.Insets;
    import androidx.core.view.ViewCompat;
    import androidx.core.view.WindowCompat;
    import androidx.core.view.WindowInsetsCompat;
    
    import androidx.activity.result.ActivityResultLauncher;
    import androidx.activity.result.contract.ActivityResultContracts;
    import androidx.appcompat.app.AppCompatActivity;
    
    import org.json.JSONArray;
import org.json.JSONObject;
    
    import java.io.File;
    import java.io.FileOutputStream;
    import java.io.InputStream;
    import java.io.OutputStream;
    import java.time.LocalDate;
    import java.util.Collections;
    import java.util.Comparator;
    import java.util.List;
    import java.util.UUID;
    
    /**
     * Advanced Canva-like Visual Recap Editor Activity.
     * Enables users to:
     * - Select, move, resize, rotate, and reposition elements.
     * - Add, edit, remove custom texts and phrases.
     * - Customize fonts (Sans, Serif, Monospace, Condensed), sizes, colors, and alignments.
     * - Add custom images from device storage and stickers (disco, nota, onda, estrela, trofeu, etc.).
     * - Customize canvas backgrounds (Solid, Linear Gradient, Radial Gradient, Dots, Grid).
     * - Rearrange and manage element layers/stacking order.
     * - Undo and Redo operations.
     * - Save changes to template only when explicitly requested.
     */
    public class RecapEditorActivity extends AppCompatActivity {
    
        public static final String EXTRA_TEMPLATE_ID = "extra_template_id";
        public static final String EXTRA_CATEGORY_ID = "extra_category_id";
        public static final String EXTRA_DATE_STRING = "extra_date_string";
    
        private RecapCanvasView canvasView;
        private HorizontalScrollView elementControlsScroll;
        private TextView editorTitle;
        private Button btnUndo;
        private Button btnRedo;
    
        private RecapTemplate currentTemplate;
        private DynamicRecapData recapData;
        private Bitmap artistImage;
        private int dynamicBgColor = 0;
        private int dynamicAccentColor = 0;
    
        private final UndoRedoManager undoRedoManager = new UndoRedoManager();

        private final ActivityResultLauncher<Intent> stickerBuilderLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String stickerPath = result.getData().getStringExtra(StickerBuilderActivity.EXTRA_STICKER_PATH);
                        if (stickerPath != null && !stickerPath.isEmpty()) {
                            showCreatedStickerNameDialog(stickerPath);
                        }
                    }
                });

        private final ActivityResultLauncher<String> stickerImportLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        saveCustomStickerElement(uri);
                    }
                });

        private final ActivityResultLauncher<String> imagePickerLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        RecapElementModel selected = canvasView != null ? canvasView.getSelectedElement() : null;
                        if (selected != null && selected.getBinding() == RecapElementBinding.TOP_TRACK_ARTWORK) {
                            replaceDynamicTrackArtwork(uri, selected);
                        } else {
                            saveCustomImageElement(uri);
                        }
                    }
                });

        private final ActivityResultLauncher<String> fontPickerLauncher =
                registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                    if (uri != null) {
                        importCustomFont(uri);
                    }
                });
@Override
        protected void onCreate(Bundle savedInstanceState) {
            ThemeManager.applySavedTheme(this);
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_recap_editor);

            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

            View root = findViewById(R.id.editorRoot);

            ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
                Insets systemBars = insets.getInsets(
                        WindowInsetsCompat.Type.systemBars()
                );

                view.setPadding(
                        0,
                        systemBars.top,
                        0,
                        systemBars.bottom
                );

                return insets;
            });

            ViewCompat.requestApplyInsets(root);
    
            canvasView = findViewById(R.id.recapCanvasView);
            elementControlsScroll = findViewById(R.id.elementControlsScroll);
            editorTitle = findViewById(R.id.editorTitle);
            btnUndo = findViewById(R.id.btnUndo);
            btnRedo = findViewById(R.id.btnRedo);
    
            // 1. Load Intent Arguments
            String templateId = getIntent().getStringExtra(EXTRA_TEMPLATE_ID);
            String categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
            String dateString = getIntent().getStringExtra(EXTRA_DATE_STRING);
    
            RecapCategory category = RecapCategory.fromId(categoryId);
            LocalDate selectedDate = dateString != null && !dateString.isEmpty() ? LocalDate.parse(dateString) : LocalDate.now();
    
    // 2. Criar canvas vazio ou carregar uma cópia do modelo selecionado
            if (templateId == null || templateId.trim().isEmpty()) {
                currentTemplate = new RecapTemplate();
                currentTemplate.setName("Meu Recap");
            } else {
                RecapTemplate baseTemplate =
                        TemplateStorage.getTemplateById(this, templateId);
    
                currentTemplate = baseTemplate.duplicate(baseTemplate.getName());
            }
    
            editorTitle.setText(currentTemplate.getName().toUpperCase());
    
            // 3. Resolve Dynamic Data
            JSONObject history = PlaybackStorage.load(this);
            recapData = RecapDataResolver.resolve(this, history, category, selectedDate);
            canvasView.setRecapData(recapData);
            canvasView.setTemplate(currentTemplate);
            loadMissingTopTrackCovers();
    
            // 4. Fetch Top Artist Image for Dynamic Palette
            String topArtist = recapData.getValue(RecapElementBinding.TOP_ARTIST_NAME);
            if (topArtist != null && !topArtist.equals("NENHUM REGISTRO")) {
                DeezerService.buscarFotoArtista(this, topArtist, bmp -> {
                    if (bmp != null) {
                        artistImage = bmp;
                        canvasView.setArtistImage(bmp);
                        androidx.palette.graphics.Palette.from(bmp).generate(palette -> {
                            if (palette != null) {
                                dynamicBgColor = palette.getDarkMutedColor(Color.rgb(16, 29, 23));
                                dynamicAccentColor = palette.getVibrantColor(palette.getDarkVibrantColor(Color.rgb(183, 255, 0)));
                                canvasView.setDynamicColors(dynamicBgColor, dynamicAccentColor);
                            }
                        });
                    }
                });
            }
    
            // Setup Listeners
            setupToolbarActions();
            setupElementSelectionListener();
            setupCanvasChangeTracking();
            updateUndoRedoButtons();
        }
    
        private void setupCanvasChangeTracking() {
            canvasView.setOnTemplateChangeListener(() -> {
                undoRedoManager.pushState(currentTemplate);
                updateUndoRedoButtons();
            });
        }
    
        private void setupElementSelectionListener() {
            canvasView.setOnElementSelectionListener(element -> {
                if (element != null) {
                    elementControlsScroll.setVisibility(View.VISIBLE);
                    updateElementControlsForType(element);
                } else {
                    elementControlsScroll.setVisibility(View.GONE);
                }
            });
        }
    
        private void updateElementControlsForType(RecapElementModel element) {
            Button btnToggleVis = findViewById(R.id.btnToggleVisibility);
            btnToggleVis.setText(element.isVisible() ? "OCULTAR" : "EXIBIR");

            boolean isShape = isShapeElement(element);
            boolean isDynamicList = isEditableDynamicList(element.getBinding());
            boolean isTrackArtwork = element.getBinding() == RecapElementBinding.TOP_TRACK_ARTWORK;
            Button editContentButton = findViewById(R.id.btnEditText);
            editContentButton.setText(isDynamicList ? "SEPARAR ITENS"
                    : isTrackArtwork ? "TROCAR CAPA" : "EDITAR TEXTO");
            editContentButton.setVisibility(isShape ? View.GONE : View.VISIBLE);
            findViewById(R.id.btnFont).setVisibility(isShape || isTrackArtwork ? View.GONE : View.VISIBLE);
            findViewById(R.id.btnAlign).setVisibility(isShape || isTrackArtwork ? View.GONE : View.VISIBLE);
            findViewById(R.id.btnColor).setVisibility(isTrackArtwork ? View.GONE : View.VISIBLE);
        }

        private boolean isShapeElement(RecapElementModel element) {
            return element != null
                    && element.getBinding() == RecapElementBinding.CUSTOM_STATIC_IMAGE
                    && element.getStickerType() != null
                    && element.getStickerType().startsWith("SHAPE_");
        }
    
        private void setupToolbarActions() {
            findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    
            btnUndo.setOnClickListener(v -> {
                RecapTemplate prev = undoRedoManager.undo(currentTemplate);
                if (prev != null) {
                    currentTemplate = prev;
                    canvasView.setTemplate(currentTemplate);
                    updateUndoRedoButtons();
                }
            });
    
            btnRedo.setOnClickListener(v -> {
                RecapTemplate next = undoRedoManager.redo(currentTemplate);
                if (next != null) {
                    currentTemplate = next;
                    canvasView.setTemplate(currentTemplate);
                    updateUndoRedoButtons();
                }
            });
    
            findViewById(R.id.btnSaveTemplate).setOnClickListener(v -> showSaveDialog());
    
            // Bottom Action Buttons
            findViewById(R.id.btnAddText).setOnClickListener(v -> showAddTextDialog());
            findViewById(R.id.btnAddData).setOnClickListener(v -> showAddDynamicDataDialog());
            findViewById(R.id.btnAddImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
            findViewById(R.id.btnAddSticker).setOnClickListener(v -> showAddStickerDialog());
            findViewById(R.id.btnAddShape).setOnClickListener(v -> showShapesDialog());
            findViewById(R.id.btnCreateSticker).setOnClickListener(v -> showCreateStickerDialog());
            findViewById(R.id.btnImportSticker).setOnClickListener(v -> stickerImportLauncher.launch("image/*"));
            findViewById(R.id.btnBackground).setOnClickListener(v -> showBackgroundCustomizer());
            findViewById(R.id.btnLayersManage).setOnClickListener(v -> showLayersDialog());
            findViewById(R.id.btnExportDirect).setOnClickListener(v -> exportRecapAsImage());
    
            // Contextual Buttons
            findViewById(R.id.btnEditText).setOnClickListener(v -> {
                RecapElementModel selected = canvasView.getSelectedElement();
                if (selected != null && isEditableDynamicList(selected.getBinding())) {
                    splitSelectedDynamicList();
                } else if (selected != null && selected.getBinding() == RecapElementBinding.TOP_TRACK_ARTWORK) {
                    imagePickerLauncher.launch("image/*");
                } else {
                    showEditTextDialog();
                }
            });
            findViewById(R.id.btnColor).setOnClickListener(v -> showColorPickerDialog());
            findViewById(R.id.btnFont).setOnClickListener(v -> showFontPickerDialog());
            findViewById(R.id.btnAlign).setOnClickListener(v -> toggleAlignment());
            findViewById(R.id.btnRotate).setOnClickListener(v -> rotateSelectedElement());
            findViewById(R.id.btnLayerUp).setOnClickListener(v -> moveLayer(1));
            findViewById(R.id.btnLayerDown).setOnClickListener(v -> moveLayer(-1));
            findViewById(R.id.btnToggleVisibility).setOnClickListener(v -> toggleVisibility());
            findViewById(R.id.btnDeleteElement).setOnClickListener(v -> deleteSelectedElement());
        }
    
    
    
        private void updateUndoRedoButtons() {
            btnUndo.setEnabled(undoRedoManager.canUndo());
            btnUndo.setAlpha(undoRedoManager.canUndo() ? 1.0f : 0.4f);
    
            btnRedo.setEnabled(undoRedoManager.canRedo());
            btnRedo.setAlpha(undoRedoManager.canRedo() ? 1.0f : 0.4f);
        }
    
        // ELEMENT EDITING ACTIONS
    
        private void showAddDynamicDataDialog() {
    
            RecapElementBinding[] bindings = {
                    RecapElementBinding.APP_BRAND,
                    RecapElementBinding.VOLUME_TAG,
                    RecapElementBinding.PERIOD_TITLE,
                    RecapElementBinding.PERIOD_RANGE,
    
                    RecapElementBinding.TOTAL_TIME_LABEL,
                    RecapElementBinding.TOTAL_TIME_FORMATTED,
                    RecapElementBinding.TOTAL_MINUTES_SUB,
    
                    RecapElementBinding.TOP_ARTIST_TAG,
                    RecapElementBinding.TOP_ARTIST_NAME,
                    RecapElementBinding.TOP_ARTIST_TIME,
                    RecapElementBinding.TOP_ARTIST_IMAGE,
    
                    RecapElementBinding.DISCOVERY_LABEL,
                    RecapElementBinding.UNIQUE_ARTISTS_COUNT,
                    RecapElementBinding.UNIQUE_ARTISTS_LABEL,
    
                    RecapElementBinding.ALSO_IN_ROTATION_TITLE,
                    RecapElementBinding.SECONDARY_ARTISTS_LIST,
    
                    RecapElementBinding.BEST_DAY_TITLE,
                    RecapElementBinding.BEST_DAY_NAME,
                    RecapElementBinding.BEST_DAY_TIME,
    
                    RecapElementBinding.ACHIEVEMENTS_TITLE,
                    RecapElementBinding.ACHIEVEMENTS_LIST,
    
                    RecapElementBinding.TOP_TRACKS_TITLE,
                    RecapElementBinding.TOP_TRACKS_LIST,
    
                    RecapElementBinding.FOOTER_LINE_1,
                    RecapElementBinding.FOOTER_LINE_2
            };
    
            String[] descriptions = {
                    "Nome da aplicação",
                    "Número ou edição da retrospectiva",
                    "Título do período selecionado",
                    "Datas inicial e final do período",
    
                    "Legenda que identifica o tempo total",
                    "Tempo total de reprodução em horas e minutos",
                    "Tempo total convertido em minutos",
    
                    "Identificação do artista mais ouvido",
                    "Nome do artista mais ouvido",
                    "Tempo que você ouviu esse artista",
                    "Imagem do artista mais ouvido",
    
                    "Título da seção de descobertas",
                    "Quantidade de artistas diferentes ouvidos",
                    "Legenda da quantidade de artistas",
    
                    "Título da seção de outros artistas",
                    "Lista dos artistas que ficaram em segundo e terceiro lugar",
    
                    "Título da seção de melhor momento",
                    "Dia ou mês com maior tempo de reprodução",
                    "Tempo registrado no melhor dia ou mês",
    
                    "Título da seção de conquistas",
                    "Lista de conquistas da retrospectiva",
    
                    "Título da seção de músicas mais ouvidas",
                    "Lista das músicas mais reproduzidas",
    
                    "Primeira linha de texto do rodapé",
                    "Segunda linha de texto do rodapé"
            };
    
            android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
    
            android.widget.LinearLayout container =
                    new android.widget.LinearLayout(this);
    
            container.setOrientation(android.widget.LinearLayout.VERTICAL);
            container.setPadding(24, 12, 24, 12);
    
            scrollView.addView(container);
    
            final AlertDialog[] dialogRef = new AlertDialog[1];
    
            for (int i = 0; i < bindings.length; i++) {
    
                final RecapElementBinding binding = bindings[i];
    
                String value = getDynamicDataPreview(binding);
    
                android.widget.LinearLayout item =
                        new android.widget.LinearLayout(this);
    
                item.setOrientation(android.widget.LinearLayout.VERTICAL);
                item.setPadding(18, 16, 18, 16);
    
                item.setBackgroundColor(Color.rgb(30, 43, 34));
    
                android.widget.LinearLayout.LayoutParams params =
                        new android.widget.LinearLayout.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                        );
    
                params.setMargins(0, 0, 0, 12);
    
                item.setLayoutParams(params);
    
                TextView title = new TextView(this);
                title.setText(binding.getDisplayName());
                title.setTextColor(Color.rgb(183, 255, 0));
                title.setTextSize(15);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
    
                TextView description = new TextView(this);
                description.setText(descriptions[i]);
                description.setTextColor(Color.WHITE);
                description.setTextSize(13);
    
                TextView preview = new TextView(this);
                preview.setText("DADO ATUAL: " + value);
                preview.setTextColor(Color.LTGRAY);
                preview.setTextSize(12);
                preview.setPadding(0, 8, 0, 0);
    
                item.addView(title);
                item.addView(description);
                item.addView(preview);
    
                item.setOnClickListener(v -> {
    
                    undoRedoManager.pushState(currentTemplate);
    
                    float x = 0.10f;
                    float y = 0.40f;
                    float width = 0.80f;
                    float height = 0.08f;
                    switch (binding) {
                        case TOP_ARTIST_IMAGE:
                            x = 0.35f; y = 0.12f; width = 0.30f; height = 0.17f; break;
                        case SECONDARY_ARTISTS_LIST:
                            y = 0.52f; height = 0.32f; break;
                        case ACHIEVEMENTS_LIST:
                            x = 0.08f; y = 0.25f; width = 0.84f; height = 0.48f; break;
                        case TOP_TRACKS_LIST:
                            x = 0.05f; y = 0.20f; width = 0.90f; height = 0.52f; break;
                    }
                    RecapElementModel element = new RecapElementModel(binding, x, y, width, height);
    
                    element.setFontSizeSp(20f);
                    element.setFontColorHex("#F0EBDD");
                    element.setBold(true);
                    element.setZIndex(currentTemplate.getElements().size());
    
                    currentTemplate.getElements().add(element);
    
                    canvasView.setSelectedElementId(element.getId());
                    canvasView.invalidate();
    
                    updateUndoRedoButtons();
    
                    if (dialogRef[0] != null) {
                        dialogRef[0].dismiss();
                    }
                });
    
                container.addView(item);
            }
    
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Escolha uma informação")
                    .setMessage("Selecione o dado que deseja adicionar à retrospectiva.")
                    .setView(scrollView)
                    .setNegativeButton("Cancelar", null)
                    .create();
            dialogRef[0] = dialog;
            dialog.show();
        }
    
    
    
                private String getDynamicDataPreview(RecapElementBinding binding) {
            if (recapData == null) return "Dados indisponíveis";
            StringBuilder preview = new StringBuilder();
            switch (binding) {
                case TOP_ARTIST_IMAGE:
                    String artistName = recapData.getValue(RecapElementBinding.TOP_ARTIST_NAME);
                    if (artistImage != null && !artistImage.isRecycled()) {
                        return "Foto carregada: " + artistImage.getWidth() + " × " + artistImage.getHeight() + " px";
                    }
                    return artistName != null && !artistName.isEmpty()
                            && !"NENHUM REGISTRO".equalsIgnoreCase(artistName)
                            ? "Foto de " + artistName + " (carregando ou aguardando rede)"
                            : "Nenhum artista encontrado neste período";
                case SECONDARY_ARTISTS_LIST:
                    for (DynamicRecapData.TopArtistItem artist : recapData.getSecondaryArtists()) {
                        preview.append(artist.rank).append(". ").append(artist.name);
                        if (artist.timeFormatted != null && !artist.timeFormatted.isEmpty()) {
                            preview.append(" — ").append(artist.timeFormatted);
                        }
                        preview.append("\n");
                    }
                    break;
                case ACHIEVEMENTS_LIST:
                    for (DynamicRecapData.AchievementItem achievement : recapData.getUnlockedAchievements()) {
                        preview.append("• ").append(achievement.title)
                                .append(" (").append(achievement.formattedDate).append(")\n");
                    }
                    break;
                case TOP_TRACKS_LIST:
                    for (DynamicRecapData.TopTrackItem track : recapData.getTopTracks()) {
                        preview.append("• ").append(track.title).append(" — ").append(track.artist)
                                .append(track.artworkPath == null || track.artworkPath.isEmpty()
                                        ? " (capa será buscada)" : " (capa disponível)")
                                .append("\n");
                    }
                    break;
                default:
                    String value = recapData.getValue(binding);
                    if (value != null && !value.trim().isEmpty() && !"—".equals(value.trim())) return value;
                    return "Sem informação disponível";
            }
            return preview.length() == 0
                    ? "Nenhum registro encontrado para este período"
                    : preview.toString().trim();
        }

        private void loadMissingTopTrackCovers() {
            if (recapData == null) return;
            for (DynamicRecapData.TopTrackItem track : recapData.getTopTracks()) {
                boolean needsCover = track.artworkPath == null || track.artworkPath.trim().isEmpty();
                if (!needsCover) {
                    File existing = new File(track.artworkPath);
                    if (!existing.isAbsolute()) existing = new File(getFilesDir(), track.artworkPath);
                    needsCover = !existing.isFile();
                }
                if (!needsCover) continue;
                DeezerService.buscarCapaMusica(this, track.title, track.artist, (bitmap, artworkPath) -> {
                    if (!isFinishing() && artworkPath != null && !artworkPath.isEmpty()) {
                        track.artworkPath = artworkPath;
                        canvasView.invalidate();
                    }
                    if (bitmap != null && !bitmap.isRecycled()) bitmap.recycle();
                });
            }
        }

        private void showAddTextDialog() {
            EditText input = new EditText(this);
            input.setHint("Digite o texto...");
            input.setTextColor(Color.WHITE);
    
            new AlertDialog.Builder(this)
                    .setTitle("Adicionar Texto")
                    .setView(input)
                    .setPositiveButton("Adicionar", (dialog, which) -> {
                        String text = input.getText().toString().trim();
                        if (!text.isEmpty()) {
                            undoRedoManager.pushState(currentTemplate);
                            RecapElementModel newElem = new RecapElementModel(
                                    RecapElementBinding.CUSTOM_STATIC_TEXT,
                                    0.1f, 0.4f, 0.8f, 0.06f
                            );
                            newElem.setCustomText(text);
                            newElem.setFontSizeSp(20f);
                            newElem.setFontColorHex("#B7FF00");
                            newElem.setBold(true);
                            newElem.setZIndex(currentTemplate.getElements().size());
                            currentTemplate.getElements().add(newElem);
                            canvasView.setSelectedElementId(newElem.getId());
                            canvasView.invalidate();
                            updateUndoRedoButtons();
                        }
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    
        private void showEditTextDialog() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
            if (isEditableDynamicList(elem.getBinding())) {
                splitSelectedDynamicList();
                return;
            }
            if (elem.getBinding() == RecapElementBinding.TOP_TRACK_ARTWORK) {
                imagePickerLauncher.launch("image/*");
                return;
            }

            boolean dynamicItemText = isDynamicItemTextBinding(elem.getBinding());
            EditText input = new EditText(this);
            String currentText = elem.getBinding() == RecapElementBinding.CUSTOM_STATIC_TEXT
                    || dynamicItemText ? (dynamicItemText ? getDynamicItemCurrentText(elem) : elem.getCustomText())
                    : recapData != null ? recapData.getValue(elem.getBinding()) : "";
            input.setText(currentText != null ? currentText : "");
            input.setTextColor(Color.WHITE);

            new AlertDialog.Builder(this)
                    .setTitle("Editar Conteúdo do Texto")
                    .setView(input)
                    .setPositiveButton("Aplicar", (dialog, which) -> {
                        undoRedoManager.pushState(currentTemplate);
                        elem.setCustomText(input.getText().toString());
                        if (!dynamicItemText) {
                            elem.setBinding(RecapElementBinding.CUSTOM_STATIC_TEXT);
                        }
                        canvasView.invalidate();
                        updateUndoRedoButtons();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private boolean isDynamicItemTextBinding(RecapElementBinding binding) {
            if (binding == null) return false;
            switch (binding) {
                case SECONDARY_ARTIST_RANK:
                case SECONDARY_ARTIST_NAME:
                case SECONDARY_ARTIST_TIME:
                case TOP_TRACK_ITEM_TITLE:
                case TOP_TRACK_ITEM_ARTIST:
                case TOP_TRACK_ITEM_DURATION:
                case ACHIEVEMENT_ITEM_TITLE:
                case ACHIEVEMENT_ITEM_DATE:
                    return true;
                default:
                    return false;
            }
        }

        private String getDynamicItemCurrentText(RecapElementModel elem) {
            if (elem.getCustomText() != null && !elem.getCustomText().isEmpty()) return elem.getCustomText();
            if (recapData == null || elem.getDynamicItemIndex() < 0) return "";
            int index = elem.getDynamicItemIndex();
            switch (elem.getBinding()) {
                case SECONDARY_ARTIST_RANK:
                    return index < recapData.getSecondaryArtists().size()
                            ? String.format(java.util.Locale.ROOT, "%02d", recapData.getSecondaryArtists().get(index).rank) : "";
                case SECONDARY_ARTIST_NAME:
                    return index < recapData.getSecondaryArtists().size() ? recapData.getSecondaryArtists().get(index).name : "";
                case SECONDARY_ARTIST_TIME:
                    return index < recapData.getSecondaryArtists().size() ? recapData.getSecondaryArtists().get(index).timeFormatted : "";
                case TOP_TRACK_ITEM_TITLE:
                    return index < recapData.getTopTracks().size() ? recapData.getTopTracks().get(index).title : "";
                case TOP_TRACK_ITEM_ARTIST:
                    return index < recapData.getTopTracks().size() ? recapData.getTopTracks().get(index).artist : "";
                case TOP_TRACK_ITEM_DURATION:
                    return index < recapData.getTopTracks().size() ? recapData.getTopTracks().get(index).duration : "";
                case ACHIEVEMENT_ITEM_TITLE:
                    return index < recapData.getUnlockedAchievements().size()
                            ? "🏆 " + recapData.getUnlockedAchievements().get(index).title : "";
                case ACHIEVEMENT_ITEM_DATE:
                    return index < recapData.getUnlockedAchievements().size()
                            ? recapData.getUnlockedAchievements().get(index).formattedDate : "";
                default:
                    return "";
            }
        }

        private void splitSelectedDynamicList() {
            RecapElementModel group = canvasView.getSelectedElement();
            if (group == null || !isEditableDynamicList(group.getBinding())) return;

            RecapElementBinding binding = group.getBinding();
            JSONArray overrideItems = getSavedDynamicListItems(group, binding);
            int count;
            if (overrideItems != null) {
                count = overrideItems.length();
            } else if (recapData != null) {
                switch (binding) {
                    case SECONDARY_ARTISTS_LIST: count = recapData.getSecondaryArtists().size(); break;
                    case TOP_TRACKS_LIST: count = recapData.getTopTracks().size(); break;
                    case ACHIEVEMENTS_LIST: count = recapData.getUnlockedAchievements().size(); break;
                    default: count = 0;
                }
            } else {
                count = 0;
            }

            float availableDesignHeight = group.getHeight() * 640f;
            int visibleRows;
            switch (binding) {
                case SECONDARY_ARTISTS_LIST: visibleRows = Math.max(0, (int) ((availableDesignHeight - 72f) / 48f)); break;
                case TOP_TRACKS_LIST: visibleRows = Math.max(0, (int) ((availableDesignHeight - 70f) / 60f)); break;
                case ACHIEVEMENTS_LIST: visibleRows = Math.max(0, (int) ((availableDesignHeight - 72f) / 46f)); break;
                default: visibleRows = count;
            }
            count = Math.min(count, visibleRows);

            if (count == 0) {
                Toast.makeText(this, "Aumente a altura da lista para separar os itens.", Toast.LENGTH_SHORT).show();
                return;
            }

            undoRedoManager.pushState(currentTemplate);
            List<RecapElementModel> elements = currentTemplate.getElements();
            int insertionIndex = Math.max(0, elements.indexOf(group));
            int baseLayer = group.getZIndex();
            float baseX = group.getX() * 360f;
            float baseY = group.getY() * 640f;
            float groupWidth = group.getWidth() * 360f;
            elements.remove(group);
            int firstAddedIndex = elements.size();

            RecapElementModel firstCreated = null;
            for (int i = 0; i < count; i++) {
                JSONObject override = overrideItems != null ? overrideItems.optJSONObject(i) : null;
                if (binding == RecapElementBinding.SECONDARY_ARTISTS_LIST) {
                    float rowY = baseY + 72f + i * 48f;
                    firstCreated = addSplitTextElement(elements, RecapElementBinding.SECONDARY_ARTIST_RANK,
                            i, baseX + 10f, rowY - 17f, 32f, 22f, 14f, "#B7FF00", true,
                            override != null ? String.format(java.util.Locale.ROOT, "%02d", override.optInt("rank", i + 2)) : null, baseLayer);
                    addSplitTextElement(elements, RecapElementBinding.SECONDARY_ARTIST_NAME,
                            i, baseX + 50f, rowY - 17f, groupWidth * 0.52f, 22f, 12f, "#F0EBDD", true,
                            override != null ? override.optString("name", "") : null, baseLayer + 1);
                    addSplitTextElement(elements, RecapElementBinding.SECONDARY_ARTIST_TIME,
                            i, baseX + groupWidth * 0.70f, rowY - 17f, groupWidth * 0.28f, 22f, 10f, "#B7FF00", false,
                            override != null ? override.optString("time", "") : null, baseLayer + 2);
                } else if (binding == RecapElementBinding.TOP_TRACKS_LIST) {
                    float rowY = baseY + 70f + i * 60f;
                    RecapElementModel artwork = new RecapElementModel(RecapElementBinding.TOP_TRACK_ARTWORK,
                            (baseX) / 360f, (rowY + 5f) / 640f, 50f / 360f, 50f / 640f);
                    artwork.setDynamicItemIndex(i);
                    artwork.setZIndex(baseLayer + i * 4);
                    if (override != null) artwork.setImagePath(override.optString("artworkPath", ""));
                    elements.add(artwork);
                    firstCreated = firstCreated == null ? artwork : firstCreated;
                    addSplitTextElement(elements, RecapElementBinding.TOP_TRACK_ITEM_TITLE,
                            i, baseX + 68f, rowY + 3f, groupWidth - 70f, 20f, 11f, "#FFFFFF", true,
                            override != null ? override.optString("title", "") : null, baseLayer + i * 4 + 1);
                    addSplitTextElement(elements, RecapElementBinding.TOP_TRACK_ITEM_ARTIST,
                            i, baseX + 68f, rowY + 27f, groupWidth * 0.55f, 18f, 10f, "#F0EBDD", false,
                            override != null ? override.optString("artist", "") : null, baseLayer + i * 4 + 2);
                    addSplitTextElement(elements, RecapElementBinding.TOP_TRACK_ITEM_DURATION,
                            i, baseX + groupWidth - 52f, rowY + 27f, 50f, 18f, 9f, "#B7FF00", false,
                            override != null ? override.optString("duration", "") : null, baseLayer + i * 4 + 3);
                } else if (binding == RecapElementBinding.ACHIEVEMENTS_LIST) {
                    float rowY = baseY + 72f + i * 46f;
                    firstCreated = addSplitTextElement(elements, RecapElementBinding.ACHIEVEMENT_ITEM_TITLE,
                            i, baseX, rowY - 17f, groupWidth * 0.68f, 22f, 11f, "#F0EBDD", false,
                            override != null ? "🏆 " + override.optString("title", "") : null, baseLayer + i * 2);
                    addSplitTextElement(elements, RecapElementBinding.ACHIEVEMENT_ITEM_DATE,
                            i, baseX + groupWidth * 0.70f, rowY - 17f, groupWidth * 0.30f, 22f, 9f, "#B7FF00", false,
                            override != null ? override.optString("date", "") : null, baseLayer + i * 2 + 1);
                }
            }

            List<RecapElementModel> createdElements = new java.util.ArrayList<>(
                    elements.subList(firstAddedIndex, elements.size()));
            elements.subList(firstAddedIndex, elements.size()).clear();
            elements.addAll(Math.min(insertionIndex, elements.size()), createdElements);

            if (firstCreated != null) canvasView.setSelectedElementId(firstCreated.getId());
            canvasView.setTemplate(currentTemplate);
            canvasView.invalidate();
            updateUndoRedoButtons();
            Toast.makeText(this, "Itens separados. Agora cada parte pode ser editada individualmente.", Toast.LENGTH_LONG).show();
        }

        private RecapElementModel addSplitTextElement(List<RecapElementModel> elements,
                RecapElementBinding binding, int itemIndex, float x, float y, float width, float height,
                float fontSize, String color, boolean bold, String customText, int zIndex) {
            RecapElementModel element = new RecapElementModel(binding,
                    x / 360f, y / 640f, width / 360f, height / 640f);
            element.setDynamicItemIndex(itemIndex);
            element.setFontSizeSp(fontSize);
            element.setFontColorHex(color);
            element.setBold(bold);
            element.setZIndex(zIndex);
            if (customText != null) element.setCustomText(customText);
            elements.add(element);
            return element;
        }

        private void replaceDynamicTrackArtwork(Uri uri, RecapElementModel element) {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) return;
                File destination = new File(getFilesDir(), "track_cover_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream output = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = input.read(buffer)) > 0) output.write(buffer, 0, length);
                }
                undoRedoManager.pushState(currentTemplate);
                element.setImagePath(destination.getAbsolutePath());
                canvasView.invalidate();
                updateUndoRedoButtons();
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível trocar a capa.", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isEditableDynamicList(RecapElementBinding binding) {
            return binding == RecapElementBinding.SECONDARY_ARTISTS_LIST
                    || binding == RecapElementBinding.TOP_TRACKS_LIST
                    || binding == RecapElementBinding.ACHIEVEMENTS_LIST;
        }

        private void showEditDynamicListDialog(RecapElementModel elem) {
            RecapElementBinding binding = elem.getBinding();
            EditText input = new EditText(this);
            input.setTextColor(Color.WHITE);
            input.setTextSize(14);
            input.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
            input.setMinLines(5);
            input.setMaxLines(12);
            input.setPadding(dp(12), dp(10), dp(12), dp(10));
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                    | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            input.setText(getDynamicListEditorText(elem));

            String title;
            String instructions;
            switch (binding) {
                case SECONDARY_ARTISTS_LIST:
                    title = "Editar Top 2 e Top 3 artistas";
                    instructions = "Uma linha por artista: Nome | tempo. A posição segue a ordem das linhas.";
                    break;
                case TOP_TRACKS_LIST:
                    title = "Editar Top músicas";
                    instructions = "Uma música por linha: Música | Artista | duração. As capas existentes são preservadas pela posição da linha.";
                    break;
                case ACHIEVEMENTS_LIST:
                    title = "Editar conquistas";
                    instructions = "Uma conquista por linha: Nome | data.";
                    break;
                default:
                    return;
            }

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(instructions)
                    .setView(input)
                    .setPositiveButton("Aplicar", (dialog, which) -> {
                        undoRedoManager.pushState(currentTemplate);
                        saveDynamicListOverride(elem, input.getText().toString());
                        canvasView.invalidate();
                        updateUndoRedoButtons();
                    })
                    .setNeutralButton("Restaurar automático", (dialog, which) -> {
                        undoRedoManager.pushState(currentTemplate);
                        elem.setCustomText("");
                        canvasView.invalidate();
                        updateUndoRedoButtons();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private String getDynamicListEditorText(RecapElementModel elem) {
            RecapElementBinding binding = elem.getBinding();
            StringBuilder text = new StringBuilder();
            try {
                if (elem.getCustomText() != null && !elem.getCustomText().trim().isEmpty()) {
                    JSONObject saved = new JSONObject(elem.getCustomText());
                    JSONArray items = saved.optJSONArray("items");
                    if (items != null && binding.getKey().equals(saved.optString("type"))) {
                        for (int i = 0; i < items.length(); i++) {
                            JSONObject item = items.optJSONObject(i);
                            if (item == null) continue;
                            if (text.length() > 0) text.append("\n");
                            appendEditableListLine(text, binding, item);
                        }
                        return text.toString();
                    }
                }
            } catch (Exception ignored) { }

            if (recapData == null) return "";
            switch (binding) {
                case SECONDARY_ARTISTS_LIST:
                    for (DynamicRecapData.TopArtistItem artist : recapData.getSecondaryArtists()) {
                        if (text.length() > 0) text.append("\n");
                        text.append(artist.name);
                        if (artist.timeFormatted != null && !artist.timeFormatted.isEmpty()) {
                            text.append(" | ").append(artist.timeFormatted);
                        }
                    }
                    break;
                case TOP_TRACKS_LIST:
                    for (DynamicRecapData.TopTrackItem track : recapData.getTopTracks()) {
                        if (text.length() > 0) text.append("\n");
                        text.append(track.title).append(" | ").append(track.artist);
                        if (track.duration != null && !track.duration.isEmpty()) {
                            text.append(" | ").append(track.duration);
                        }
                    }
                    break;
                case ACHIEVEMENTS_LIST:
                    for (DynamicRecapData.AchievementItem achievement : recapData.getUnlockedAchievements()) {
                        if (text.length() > 0) text.append("\n");
                        text.append(achievement.title);
                        if (achievement.formattedDate != null && !achievement.formattedDate.isEmpty()) {
                            text.append(" | ").append(achievement.formattedDate);
                        }
                    }
                    break;
                default: break;
            }
            return text.toString();
        }

        private void appendEditableListLine(StringBuilder out, RecapElementBinding binding, JSONObject item) {
            switch (binding) {
                case SECONDARY_ARTISTS_LIST:
                    out.append(item.optString("name", ""));
                    if (!item.optString("time", "").isEmpty()) out.append(" | ").append(item.optString("time"));
                    break;
                case TOP_TRACKS_LIST:
                    out.append(item.optString("title", "")).append(" | ").append(item.optString("artist", ""));
                    if (!item.optString("duration", "").isEmpty()) out.append(" | ").append(item.optString("duration"));
                    break;
                case ACHIEVEMENTS_LIST:
                    out.append(item.optString("title", ""));
                    if (!item.optString("date", "").isEmpty()) out.append(" | ").append(item.optString("date"));
                    break;
                default: break;
            }
        }

        private void saveDynamicListOverride(RecapElementModel elem, String rawText) {
            RecapElementBinding binding = elem.getBinding();
            JSONArray items = new JSONArray();
            String[] lines = rawText.split("\\r?\\n");
            JSONArray previousItems = getSavedDynamicListItems(elem, binding);
            for (String rawLine : lines) {
                String line = rawLine.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|", -1);
                for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
                try {
                    JSONObject item = new JSONObject();
                    int index = items.length();
                    switch (binding) {
                        case SECONDARY_ARTISTS_LIST:
                            item.put("rank", index + 2);
                            item.put("name", parts[0]);
                            item.put("time", parts.length > 1 ? parts[1] : "");
                            break;
                        case TOP_TRACKS_LIST:
                            item.put("title", parts[0]);
                            item.put("artist", parts.length > 1 ? parts[1] : "");
                            item.put("duration", parts.length > 2 ? parts[2] : "");
                            String artwork = findTrackArtworkPath(previousItems,
                                    parts[0], parts.length > 1 ? parts[1] : "", index);
                            item.put("artworkPath", artwork);
                            break;
                        case ACHIEVEMENTS_LIST:
                            JSONObject oldAchievement = previousItems != null && index < previousItems.length()
                                    ? previousItems.optJSONObject(index) : null;
                            item.put("id", oldAchievement != null
                                    ? oldAchievement.optString("id", UUID.randomUUID().toString())
                                    : getDefaultAchievementId(index));
                            item.put("title", parts[0]);
                            item.put("date", parts.length > 1 ? parts[1] : "");
                            break;
                        default: continue;
                    }
                    items.put(item);
                } catch (Exception ignored) { }
            }
            try {
                JSONObject root = new JSONObject();
                root.put("type", binding.getKey());
                root.put("items", items);
                elem.setCustomText(root.toString());
            } catch (Exception ignored) {
                Toast.makeText(this, "Não foi possível salvar essa lista.", Toast.LENGTH_SHORT).show();
            }
        }

        private JSONArray getSavedDynamicListItems(RecapElementModel elem, RecapElementBinding binding) {
            try {
                JSONObject root = new JSONObject(elem.getCustomText());
                if (binding.getKey().equals(root.optString("type"))) return root.optJSONArray("items");
            } catch (Exception ignored) { }
            return null;
        }

        private String findTrackArtworkPath(JSONArray previousItems, String title,
                                            String artist, int fallbackIndex) {
            if (previousItems != null) {
                for (int i = 0; i < previousItems.length(); i++) {
                    JSONObject old = previousItems.optJSONObject(i);
                    if (old == null) continue;
                    if (title.equalsIgnoreCase(old.optString("title", "").trim())
                            && artist.equalsIgnoreCase(old.optString("artist", "").trim())) {
                        return old.optString("artworkPath", "");
                    }
                }
                JSONObject oldAtPosition = previousItems.optJSONObject(fallbackIndex);
                if (oldAtPosition != null) return oldAtPosition.optString("artworkPath", "");
            }
            if (recapData != null) {
                for (DynamicRecapData.TopTrackItem track : recapData.getTopTracks()) {
                    if (title.equalsIgnoreCase(track.title.trim())
                            && artist.equalsIgnoreCase(track.artist.trim())) {
                        return track.artworkPath != null ? track.artworkPath : "";
                    }
                }
            }
            return getDefaultTrackArtworkPath(fallbackIndex);
        }

        private String getDefaultTrackArtworkPath(int index) {
            if (recapData == null || index < 0 || index >= recapData.getTopTracks().size()) return "";
            String path = recapData.getTopTracks().get(index).artworkPath;
            return path != null ? path : "";
        }

        private String getDefaultAchievementId(int index) {
            if (recapData == null || index < 0 || index >= recapData.getUnlockedAchievements().size()) {
                return UUID.randomUUID().toString();
            }
            return recapData.getUnlockedAchievements().get(index).id;
        }

        /**
         * Opens the dedicated geometric-shapes section. Shapes are stored as normal
         * template elements, so position, size, rotation, color, opacity and layers
         * remain editable and persist with the template.
         */
        private void showShapesDialog() {
            String[] labels = {
                    "Retângulo", "Retângulo arredondado", "Círculo",
                    "Oval", "Triângulo", "Losango",
                    "Pentágono", "Hexágono", "Estrela"
            };
            String[] codes = {
                    "SHAPE_RECTANGLE", "SHAPE_ROUNDED_RECTANGLE", "SHAPE_CIRCLE",
                    "SHAPE_OVAL", "SHAPE_TRIANGLE", "SHAPE_DIAMOND",
                    "SHAPE_PENTAGON", "SHAPE_HEXAGON", "SHAPE_STAR"
            };

            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.VERTICAL);
            grid.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(grid);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Formas geométricas")
                    .setMessage("Escolha uma forma preenchida. Depois, selecione-a no canvas para alterar cor, tamanho, rotação e camada.")
                    .setView(scroll)
                    .setNegativeButton("Fechar", null)
                    .create();

            for (int start = 0; start < codes.length; start += 3) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(8);
                row.setLayoutParams(rowParams);

                for (int column = 0; column < 3; column++) {
                    int index = start + column;
                    if (index >= codes.length) {
                        row.addView(new View(this), new LinearLayout.LayoutParams(0,
                                dp(112), 1f));
                        continue;
                    }

                    String shapeCode = codes[index];
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(android.view.Gravity.CENTER);
                    card.setPadding(dp(5), dp(8), dp(5), dp(8));

                    android.graphics.drawable.GradientDrawable cardBackground =
                            new android.graphics.drawable.GradientDrawable();
                    cardBackground.setColor(Color.rgb(25, 34, 28));
                    cardBackground.setCornerRadius(dp(12));
                    cardBackground.setStroke(dp(1), Color.rgb(57, 78, 61));
                    card.setBackground(cardBackground);

                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            0, dp(112), 1f);
                    cardParams.setMargins(dp(4), dp(2), dp(4), dp(2));
                    card.setLayoutParams(cardParams);

                    ShapePreviewView preview = new ShapePreviewView(this, shapeCode);
                    card.addView(preview, new LinearLayout.LayoutParams(dp(76), dp(64)));

                    TextView label = new TextView(this);
                    label.setText(labels[index]);
                    label.setTextColor(Color.rgb(240, 235, 221));
                    label.setTextSize(10);
                    label.setGravity(android.view.Gravity.CENTER);
                    label.setMaxLines(2);
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    labelParams.topMargin = dp(4);
                    card.addView(label, labelParams);

                    card.setOnClickListener(v -> {
                        addShapeElement(shapeCode);
                        dialog.dismiss();
                    });
                    row.addView(card);
                }
                grid.addView(row);
            }

            dialog.show();
        }

        private void addShapeElement(String shapeCode) {
            undoRedoManager.pushState(currentTemplate);

            RecapElementModel shape = new RecapElementModel(
                    RecapElementBinding.CUSTOM_STATIC_IMAGE,
                    0.24f, 0.34f, 0.52f, 0.24f
            );
            shape.setStickerType(shapeCode);
            shape.setFontColorHex("#B7FF00");
            shape.setOpacity(1f);
            shape.setZIndex(currentTemplate.getElements().size());

            currentTemplate.getElements().add(shape);
            canvasView.setSelectedElementId(shape.getId());
            canvasView.invalidate();
            updateUndoRedoButtons();
        }

        private class ShapePreviewView extends View {
            private final String shapeCode;

            ShapePreviewView(android.content.Context context, String shapeCode) {
                super(context);
                this.shapeCode = shapeCode;
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                RecapCanvasRenderer.drawShapePreview(
                        canvas,
                        new android.graphics.RectF(dp(4), dp(4), getWidth() - dp(4), getHeight() - dp(4)),
                        shapeCode,
                        "#B7FF00"
                );
            }
        }

        private void showAddStickerDialog() {
            String[] names = {
                    "Disco", "Nota musical", "Onda sonora", "Estrela", "Coração",
                    "Fogo", "Troféu", "Coroa", "Fones", "Fita cassete",
                    "Microfone", "Equalizador", "Brilho", "Caixa de som"
            };
            String[] codes = {
                    "DISCO", "NOTA", "ONDA", "STAR", "HEART", "FIRE",
                    "TROPHY", "CROWN", "HEADPHONES", "CASSETTE", "MIC",
                    "EQ", "SPARKLE", "SPEAKER"
            };

            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            LinearLayout grid = new LinearLayout(this);
            grid.setOrientation(LinearLayout.VERTICAL);
            grid.setPadding(dp(8), dp(8), dp(8), dp(8));
            scroll.addView(grid);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Galeria de stickers")
                    .setView(scroll)
                    .setNegativeButton("Cancelar", null)
                    .create();

            for (int start = 0; start < codes.length; start += 3) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(8);
                row.setLayoutParams(rowParams);

                for (int column = 0; column < 3; column++) {
                    int index = start + column;
                    if (index >= codes.length) {
                        row.addView(new View(this), new LinearLayout.LayoutParams(0,
                                dp(112), 1f));
                        continue;
                    }

                    String stickerCode = codes[index];
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(android.view.Gravity.CENTER);
                    card.setPadding(dp(5), dp(8), dp(5), dp(8));

                    android.graphics.drawable.GradientDrawable cardBackground =
                            new android.graphics.drawable.GradientDrawable();
                    cardBackground.setColor(Color.rgb(25, 34, 28));
                    cardBackground.setCornerRadius(dp(12));
                    cardBackground.setStroke(dp(1), Color.rgb(57, 78, 61));
                    card.setBackground(cardBackground);

                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            0, dp(112), 1f);
                    cardParams.setMargins(dp(4), dp(2), dp(4), dp(2));
                    card.setLayoutParams(cardParams);

                    StickerPreviewView preview = new StickerPreviewView(this, stickerCode);
                    card.addView(preview, new LinearLayout.LayoutParams(dp(64), dp(64)));

                    TextView label = new TextView(this);
                    label.setText(names[index]);
                    label.setTextColor(Color.rgb(240, 235, 221));
                    label.setTextSize(11);
                    label.setGravity(android.view.Gravity.CENTER);
                    label.setMaxLines(2);
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    labelParams.topMargin = dp(4);
                    card.addView(label, labelParams);

                    card.setOnClickListener(v -> {
                        undoRedoManager.pushState(currentTemplate);
                        RecapElementModel sticker = new RecapElementModel(
                                RecapElementBinding.CUSTOM_STATIC_IMAGE,
                                0.35f, 0.4f, 0.25f, 0.15f
                        );
                        sticker.setStickerType(stickerCode);
                        sticker.setFontColorHex("#B7FF00");
                        sticker.setZIndex(currentTemplate.getElements().size());
                        currentTemplate.getElements().add(sticker);
                        canvasView.setSelectedElementId(sticker.getId());
                        canvasView.invalidate();
                        updateUndoRedoButtons();
                        dialog.dismiss();
                    });
                    row.addView(card);
                }
                grid.addView(row);
            }

            addSavedStickerCards(grid, dialog);
            dialog.show();
        }

        private void addSavedStickerCards(LinearLayout grid, AlertDialog dialog) {
            List<StickerStorage.StickerItem> savedStickers =
                    StickerStorage.getStickers(this);
            if (savedStickers.isEmpty()) return;

            TextView sectionTitle = new TextView(this);
            sectionTitle.setText("MEUS STICKERS");
            sectionTitle.setTextColor(Color.rgb(183, 255, 0));
            sectionTitle.setTextSize(12);
            sectionTitle.setTypeface(android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.BOLD);
            sectionTitle.setPadding(dp(8), dp(14), dp(8), dp(8));
            grid.addView(sectionTitle);

            for (int start = 0; start < savedStickers.size(); start += 3) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = dp(8);
                row.setLayoutParams(rowParams);

                for (int column = 0; column < 3; column++) {
                    int index = start + column;
                    if (index >= savedStickers.size()) {
                        row.addView(new View(this), new LinearLayout.LayoutParams(0,
                                dp(112), 1f));
                        continue;
                    }

                    StickerStorage.StickerItem item = savedStickers.get(index);
                    LinearLayout card = new LinearLayout(this);
                    card.setOrientation(LinearLayout.VERTICAL);
                    card.setGravity(android.view.Gravity.CENTER);
                    card.setPadding(dp(5), dp(8), dp(5), dp(8));

                    android.graphics.drawable.GradientDrawable background =
                            new android.graphics.drawable.GradientDrawable();
                    background.setColor(Color.rgb(25, 34, 28));
                    background.setCornerRadius(dp(12));
                    background.setStroke(dp(1), Color.rgb(57, 78, 61));
                    card.setBackground(background);

                    LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                            0, dp(112), 1f);
                    cardParams.setMargins(dp(4), dp(2), dp(4), dp(2));
                    card.setLayoutParams(cardParams);

                    android.widget.ImageView preview = new android.widget.ImageView(this);
                    Bitmap bitmap = BitmapFactory.decodeFile(item.getPath());
                    if (bitmap != null) {
                        preview.setImageBitmap(bitmap);
                    }
                    preview.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
                    preview.setBackgroundColor(Color.rgb(15, 20, 16));
                    card.addView(preview, new LinearLayout.LayoutParams(dp(64), dp(64)));

                    TextView label = new TextView(this);
                    label.setText(item.getName());
                    label.setTextColor(Color.rgb(240, 235, 221));
                    label.setTextSize(11);
                    label.setGravity(android.view.Gravity.CENTER);
                    label.setMaxLines(2);
                    LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    labelParams.topMargin = dp(4);
                    card.addView(label, labelParams);

                    card.setOnClickListener(v -> {
                        addSavedStickerElement(item);
                        Toast.makeText(this, item.getName() + " adicionado ao recap!",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    });
                    row.addView(card);
                }
                grid.addView(row);
            }
        }

        private void addSavedStickerElement(StickerStorage.StickerItem item) {
            undoRedoManager.pushState(currentTemplate);
            RecapElementModel sticker = new RecapElementModel(
                    RecapElementBinding.CUSTOM_STATIC_IMAGE,
                    0.31f, 0.36f, 0.38f, 0.25f
            );
            sticker.setImagePath(item.getPath());
            sticker.setZIndex(currentTemplate.getElements().size());
            currentTemplate.getElements().add(sticker);
            canvasView.setSelectedElementId(sticker.getId());
            canvasView.invalidate();
            updateUndoRedoButtons();
        }

        private int dp(float value) {
            return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
        }

        private class StickerPreviewView extends View {
            private final String stickerCode;

            StickerPreviewView(android.content.Context context, String stickerCode) {
                super(context);
                this.stickerCode = stickerCode;
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                RecapCanvasRenderer.drawStickerPreview(
                        canvas,
                        new android.graphics.RectF(2, 2, getWidth() - 2, getHeight() - 2),
                        stickerCode,
                        "#B7FF00"
                );
            }
        }
    
        private void showCreateStickerDialog() {
            stickerBuilderLauncher.launch(new Intent(this, StickerBuilderActivity.class));
        }

        private void showCreatedStickerNameDialog(String temporaryPath) {
            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setHint("Nome do sticker");

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Salvar na galeria")
                    .setMessage("Escolha um nome para encontrar este sticker na aba + STICKER.")
                    .setView(input)
                    .setPositiveButton("Salvar", null)
                    .setNegativeButton("Cancelar", (d, which) -> {
                        new File(temporaryPath).delete();
                    })
                    .create();

            dialog.setOnShowListener(ignored ->
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) {
                            input.setError("Digite um nome");
                            return;
                        }
                        try {
                            StickerStorage.StickerItem saved =
                                    StickerStorage.saveSticker(this, name, new File(temporaryPath));
                            new File(temporaryPath).delete();
                            addCreatedStickerElement(saved);
                            dialog.dismiss();
                        } catch (Exception e) {
                            Toast.makeText(this, "Não foi possível salvar o sticker na galeria.",
                                    Toast.LENGTH_LONG).show();
                        }
                    }));
            dialog.show();
        }

        private void addCreatedStickerElement(StickerStorage.StickerItem item) {
            addSavedStickerElement(item);
            Toast.makeText(this, "Sticker salvo na galeria e adicionado ao recap!",
                    Toast.LENGTH_SHORT).show();
        }

        private void saveCustomStickerElement(Uri uri) {
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException("Unable to open image");
                File destination = new File(getFilesDir(),
                        "custom_sticker_" + System.currentTimeMillis() + ".png");
                try (FileOutputStream output = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }

                undoRedoManager.pushState(currentTemplate);
                RecapElementModel sticker = new RecapElementModel(
                        RecapElementBinding.CUSTOM_STATIC_IMAGE,
                        0.32f, 0.38f, 0.36f, 0.20f
                );
                sticker.setImagePath(destination.getAbsolutePath());
                sticker.setZIndex(currentTemplate.getElements().size());
                currentTemplate.getElements().add(sticker);
                canvasView.setSelectedElementId(sticker.getId());
                canvasView.invalidate();
                updateUndoRedoButtons();
                Toast.makeText(this, "Sticker importado para o canvas!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível importar o sticker.", Toast.LENGTH_SHORT).show();
            }
        }

        private void saveCustomImageElement(Uri uri) {
            try (InputStream is = getContentResolver().openInputStream(uri)) {
                if (is == null) return;
                String fileName = "custom_img_" + System.currentTimeMillis() + ".png";
                File dest = new File(getFilesDir(), fileName);
                try (FileOutputStream fos = new FileOutputStream(dest)) {
                    byte[] buf = new byte[8192];
                    int len;
                    while ((len = is.read(buf)) > 0) {
                        fos.write(buf, 0, len);
                    }
                }
    
                undoRedoManager.pushState(currentTemplate);
                RecapElementModel imgElem = new RecapElementModel(
                        RecapElementBinding.CUSTOM_STATIC_IMAGE,
                        0.2f, 0.35f, 0.6f, 0.25f
                );
                imgElem.setImagePath(dest.getAbsolutePath());
                imgElem.setZIndex(currentTemplate.getElements().size());
                currentTemplate.getElements().add(imgElem);
                canvasView.setSelectedElementId(imgElem.getId());
                canvasView.invalidate();
                updateUndoRedoButtons();
                Toast.makeText(this, "Imagem adicionada ao canvas!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Falha ao carregar imagem.", Toast.LENGTH_SHORT).show();
            }
        }
    
        private void showBackgroundCustomizer() {
            String[] options = {
                    "Cor sólida (escolher na roda)",
                    "Gradiente linear",
                    "Gradiente radial",
                    "Padrão de pontos",
                    "Padrão de grade",
                    "Personalizar cor principal...",
                    "Personalizar segunda cor...",
                    "Personalizar cor de destaque...",
                    "Personalizar cor padrão dos textos...",
                    "Personalizar cor dos painéis..."
            };

            new AlertDialog.Builder(this)
                    .setTitle("Estilo e cores do canvas")
                    .setItems(options, (dialog, which) -> {
                        if (which >= 1 && which <= 4) {
                            undoRedoManager.pushState(currentTemplate);
                        }

                        switch (which) {
                            case 0:
                                showColorWheelDialog("Cor do fundo",
                                        parseColorOrDefault(currentTemplate.getBackgroundColorHex(), Color.BLACK),
                                        color -> {
                                            currentTemplate.setBgStyle("SOLID");
                                            currentTemplate.setBackgroundColorHex(toColorHex(color));
                                            currentTemplate.setUseDynamicPalette(false);
                                            canvasView.invalidate();
                                        });
                                return;
                            case 1:
                                currentTemplate.setBgStyle("GRADIENT_LINEAR");
                                currentTemplate.setBackgroundColorHex("#1A2D23");
                                currentTemplate.setBgSecondaryColorHex("#080A08");
                                currentTemplate.setUseDynamicPalette(false);
                                break;
                            case 2:
                                currentTemplate.setBgStyle("GRADIENT_RADIAL");
                                currentTemplate.setBackgroundColorHex("#050806");
                                currentTemplate.setBgSecondaryColorHex("#213D2D");
                                currentTemplate.setUseDynamicPalette(false);
                                break;
                            case 3:
                                currentTemplate.setBgStyle("PATTERN_DOTS");
                                currentTemplate.setBackgroundColorHex("#080A08");
                                currentTemplate.setBgSecondaryColorHex("#304033");
                                currentTemplate.setUseDynamicPalette(false);
                                break;
                            case 4:
                                currentTemplate.setBgStyle("PATTERN_GRID");
                                currentTemplate.setBackgroundColorHex("#080A08");
                                currentTemplate.setBgSecondaryColorHex("#28392B");
                                currentTemplate.setUseDynamicPalette(false);
                                break;
                            case 5:
                                showColorWheelDialog("Cor principal do fundo",
                                        parseColorOrDefault(currentTemplate.getBackgroundColorHex(), Color.BLACK),
                                        color -> {
                                            currentTemplate.setBackgroundColorHex(toColorHex(color));
                                            currentTemplate.setUseDynamicPalette(false);
                                            canvasView.invalidate();
                                        });
                                return;
                            case 6:
                                showColorWheelDialog("Segunda cor do fundo",
                                        parseColorOrDefault(currentTemplate.getBgSecondaryColorHex(), Color.DKGRAY),
                                        color -> {
                                            currentTemplate.setBgSecondaryColorHex(toColorHex(color));
                                            currentTemplate.setUseDynamicPalette(false);
                                            canvasView.invalidate();
                                        });
                                return;
                            case 7:
                                showColorWheelDialog("Cor de destaque",
                                        parseColorOrDefault(currentTemplate.getAccentColorHex(), Color.GREEN),
                                        color -> {
                                            currentTemplate.setAccentColorHex(toColorHex(color));
                                            currentTemplate.setUseDynamicPalette(false);
                                            canvasView.invalidate();
                                        });
                                return;
                            case 8:
                                showColorWheelDialog("Cor padrão dos textos",
                                        parseColorOrDefault(currentTemplate.getTextColorHex(), Color.WHITE),
                                        color -> {
                                            currentTemplate.setTextColorHex(toColorHex(color));
                                            canvasView.invalidate();
                                        });
                                return;
                            case 9:
                                showColorWheelDialog("Cor dos painéis",
                                        parseColorOrDefault(currentTemplate.getSecondaryBgColorHex(), Color.DKGRAY),
                                        color -> {
                                            currentTemplate.setSecondaryBgColorHex(toColorHex(color));
                                            canvasView.invalidate();
                                        });
                                return;
                        }

                        canvasView.invalidate();
                        updateUndoRedoButtons();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private interface ColorSelectionListener {
            void onColorSelected(int color);
        }

        private int parseColorOrDefault(String color, int fallback) {
            try {
                return Color.parseColor(color);
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private String toColorHex(int color) {
            return String.format(java.util.Locale.US, "#%06X", 0xFFFFFF & color);
        }

        private void showColorWheelDialog(String title, int initialColor,
                                          ColorSelectionListener onSelected) {
            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            int padding = (int) (16 * getResources().getDisplayMetrics().density);
            container.setPadding(padding, padding, padding, padding);

            ColorWheelView wheel = new ColorWheelView(this);
            wheel.setColor(initialColor);
            // Start every color selection at full brightness, regardless of the saved color.
            wheel.setBrightness(1f);
            container.addView(wheel, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (280 * getResources().getDisplayMetrics().density)
            ));

            View preview = new View(this);
            preview.setBackgroundColor(wheel.getColor());
            LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    (int) (38 * getResources().getDisplayMetrics().density)
            );
            previewParams.topMargin = padding / 2;
            container.addView(preview, previewParams);

            TextView brightnessLabel = new TextView(this);
            brightnessLabel.setText("Brilho");
            brightnessLabel.setTextColor(Color.WHITE);
            brightnessLabel.setPadding(0, padding / 2, 0, 0);
            container.addView(brightnessLabel);

            SeekBar brightness = new SeekBar(this);
            float[] hsv = new float[3];
            Color.colorToHSV(initialColor, hsv);
            brightness.setMax(100);
            brightness.setProgress(100);
            brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    wheel.setBrightness(Math.max(8, progress) / 100f);
                    preview.setBackgroundColor(wheel.getColor());
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) { }
                @Override public void onStopTrackingTouch(SeekBar seekBar) { }
            });
            wheel.setOnColorChangedListener(preview::setBackgroundColor);
            container.addView(brightness, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));

            new AlertDialog.Builder(this)
                    .setTitle(title)
                    .setView(container)
                    .setPositiveButton("Aplicar", (dialog, which) -> {
                        undoRedoManager.pushState(currentTemplate);
                        onSelected.onColorSelected(wheel.getColor());
                        updateUndoRedoButtons();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }

        private void showColorPickerDialog() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;

            int initialColor = parseColorOrDefault(elem.getFontColorHex(), Color.WHITE);
            showColorWheelDialog("Cor do elemento", initialColor, color -> {
                elem.setFontColorHex(toColorHex(color));
                canvasView.invalidate();
            });
        }
    
        private void showFontPickerDialog() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;

            java.util.ArrayList<String> fontNames = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "Sans — padrão", "Serif — editorial", "Monospace — máquina",
                    "Condensada — pôster", "Sans leve", "Sans fina",
                    "Sans média", "Sans pesada", "Sans extra pesada",
                    "Arredondada", "Cursiva — manuscrita", "Casual",
                    "Fantasy", "Serif em negrito", "Serif itálica",
                    "Sans negrito itálica", "Small caps", "Condensada negrito"
            ));
            java.util.ArrayList<String> fontCodes = new java.util.ArrayList<>(java.util.Arrays.asList(
                    "SANS_SERIF", "SERIF", "MONOSPACE", "CONDENSED",
                    "SANS_LIGHT", "SANS_THIN", "SANS_MEDIUM", "SANS_BLACK",
                    "SANS_EXTRA_BLACK", "SANS_ROUNDED", "CURSIVE", "CASUAL",
                    "FANTASY", "SERIF_BOLD", "SERIF_ITALIC",
                    "SANS_BOLD_ITALIC", "SMALL_CAPS", "CONDENSED_BOLD"
            ));

            File[] importedFonts = getFilesDir().listFiles((dir, name) ->
                    name.startsWith("recap_font_") &&
                    (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc")));
            if (importedFonts != null) {
                java.util.Arrays.sort(importedFonts, java.util.Comparator.comparing(File::getName));
                for (File fontFile : importedFonts) {
                    String code = "CUSTOM_FONT:" + fontFile.getName();
                    fontCodes.add(code);
                    fontNames.add("Importada: " + fontFile.getName().replaceFirst("^recap_font_", ""));
                }
            }

            int padding = dp(12);
            android.widget.ScrollView scroll = new android.widget.ScrollView(this);
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            list.setPadding(padding, padding / 2, padding, padding / 2);
            scroll.addView(list);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setTitle("Escolha uma fonte")
                    .setView(scroll)
                    .setPositiveButton("Importar fonte", null)
                    .setNegativeButton("Cancelar", null)
                    .create();

            for (int i = 0; i < fontNames.size(); i++) {
                final String fontCode = fontCodes.get(i);
                boolean selected = fontCode.equals(elem.getFontFamily());
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER_VERTICAL);
                row.setPadding(padding, padding, padding, padding);
                row.setBackgroundColor(selected ? Color.rgb(44, 61, 49) : Color.rgb(24, 31, 26));

                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowParams.bottomMargin = padding / 2;
                row.setLayoutParams(rowParams);

                TextView name = new TextView(this);
                name.setText(fontNames.get(i) + (selected ? "  ✓" : ""));
                name.setTextColor(selected ? Color.rgb(183, 255, 0) : Color.WHITE);
                name.setTextSize(13);
                name.setTypeface(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD);

                TextView preview = new TextView(this);
                preview.setText("Aa Gg 0123");
                preview.setTextColor(Color.rgb(240, 235, 221));
                preview.setTextSize(19);
                preview.setTypeface(createTypefaceForFamily(fontCode));

                row.addView(name, new LinearLayout.LayoutParams(0,
                        LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                row.addView(preview);

                row.setOnClickListener(v -> {
                    undoRedoManager.pushState(currentTemplate);
                    elem.setFontFamily(fontCode);
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                    dialog.dismiss();
                });
                list.addView(row);
            }

            dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        dialog.dismiss();
                        fontPickerLauncher.launch("*/*");
                    }));
            dialog.show();
        }

        private android.graphics.Typeface createTypefaceForFamily(String family) {
            if (family != null && family.startsWith("CUSTOM_FONT:")) {
                File fontFile = new File(getFilesDir(), family.substring("CUSTOM_FONT:".length()));
                if (fontFile.exists()) {
                    try { return android.graphics.Typeface.createFromFile(fontFile); }
                    catch (RuntimeException ignored) { }
                }
            }
            String familyName;
            int style = android.graphics.Typeface.NORMAL;
            switch (family == null ? "SANS_SERIF" : family) {
                case "SERIF": familyName = "serif"; break;
                case "MONOSPACE": familyName = "monospace"; break;
                case "CONDENSED": familyName = "sans-serif-condensed"; break;
                case "CONDENSED_BOLD": familyName = "sans-serif-condensed"; style = android.graphics.Typeface.BOLD; break;
                case "SANS_LIGHT": familyName = "sans-serif-light"; break;
                case "SANS_THIN": familyName = "sans-serif-thin"; break;
                case "SANS_MEDIUM": familyName = "sans-serif-medium"; break;
                case "SANS_BLACK": familyName = "sans-serif-black"; break;
                case "SANS_EXTRA_BLACK": familyName = "sans-serif-black"; style = android.graphics.Typeface.BOLD; break;
                case "SANS_ROUNDED": familyName = "sans-serif-rounded"; break;
                case "CURSIVE": familyName = "cursive"; break;
                case "CASUAL": familyName = "casual"; break;
                case "FANTASY": familyName = "fantasy"; break;
                case "SERIF_BOLD": familyName = "serif"; style = android.graphics.Typeface.BOLD; break;
                case "SERIF_ITALIC": familyName = "serif"; style = android.graphics.Typeface.ITALIC; break;
                case "SANS_BOLD_ITALIC": familyName = "sans-serif"; style = android.graphics.Typeface.BOLD_ITALIC; break;
                case "SMALL_CAPS": familyName = "sans-serif-smallcaps"; break;
                default: familyName = "sans-serif"; break;
            }
            return android.graphics.Typeface.create(familyName, style);
        }

        private void importCustomFont(Uri uri) {
            String extension = ".ttf";
            android.database.Cursor cursor = getContentResolver().query(
                    uri, new String[]{android.provider.OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        String sourceName = cursor.getString(0);
                        if (sourceName != null && sourceName.toLowerCase(java.util.Locale.ROOT).endsWith(".otf")) {
                            extension = ".otf";
                        } else if (sourceName != null && sourceName.toLowerCase(java.util.Locale.ROOT).endsWith(".ttc")) {
                            extension = ".ttc";
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
            try (InputStream input = getContentResolver().openInputStream(uri)) {
                if (input == null) throw new java.io.IOException("Não foi possível abrir o arquivo");
                String fileName = "recap_font_" + System.currentTimeMillis() + extension;
                File destination = new File(getFilesDir(), fileName);
                try (FileOutputStream output = new FileOutputStream(destination)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                }
                android.graphics.Typeface.createFromFile(destination);
                Toast.makeText(this, "Fonte importada! Abra o seletor novamente para usá-la.", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível importar essa fonte. Use um arquivo TTF ou OTF válido.", Toast.LENGTH_LONG).show();
            }
        }

        private void toggleAlignment() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
    
            undoRedoManager.pushState(currentTemplate);
            if ("LEFT".equalsIgnoreCase(elem.getAlignment())) {
                elem.setAlignment("CENTER");
            } else if ("CENTER".equalsIgnoreCase(elem.getAlignment())) {
                elem.setAlignment("RIGHT");
            } else {
                elem.setAlignment("LEFT");
            }
            canvasView.invalidate();
            updateUndoRedoButtons();
        }
    
        private void rotateSelectedElement() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
    
            undoRedoManager.pushState(currentTemplate);
            elem.setRotation((elem.getRotation() + 15f) % 360f);
            canvasView.invalidate();
            updateUndoRedoButtons();
        }
    
        private void moveLayer(int delta) {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
    
            List<RecapElementModel> list = currentTemplate.getElements();
            int index = list.indexOf(elem);
            int newIndex = index + delta;
            if (newIndex >= 0 && newIndex < list.size()) {
                undoRedoManager.pushState(currentTemplate);
                Collections.swap(list, index, newIndex);
                for (int i = 0; i < list.size(); i++) {
                    list.get(i).setZIndex(i);
                }
                canvasView.invalidate();
                updateUndoRedoButtons();
            }
        }
    
        private void toggleVisibility() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
    
            undoRedoManager.pushState(currentTemplate);
            elem.setVisible(!elem.isVisible());
            canvasView.invalidate();
            updateElementControlsForType(elem);
            updateUndoRedoButtons();
        }
    
        private void deleteSelectedElement() {
            RecapElementModel elem = canvasView.getSelectedElement();
            if (elem == null) return;
    
            new AlertDialog.Builder(this)
                    .setTitle("Excluir Elemento")
                    .setMessage("Deseja remover este elemento da composição?")
                    .setPositiveButton("Excluir", (dialog, which) -> {
                        undoRedoManager.pushState(currentTemplate);
                        currentTemplate.getElements().remove(elem);
                        canvasView.setSelectedElementId(null);
                        canvasView.invalidate();
                        updateUndoRedoButtons();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    
        private void showLayersDialog() {
            List<RecapElementModel> list = currentTemplate.getElements();
            String[] items = new String[list.size()];
            for (int i = 0; i < list.size(); i++) {
                RecapElementModel m = list.get(i);
                String name = m.getBinding() != null ? m.getBinding().getDisplayName() : "Elemento";
                if (m.getBinding() == RecapElementBinding.CUSTOM_STATIC_TEXT && !m.getCustomText().isEmpty()) {
                    name = "\"" + m.getCustomText() + "\"";
                }
                items[i] = (i + 1) + ". " + name + (m.isVisible() ? "" : " (Oculto)");
            }
    
            new AlertDialog.Builder(this)
                    .setTitle("Gerenciar Camadas")
                    .setItems(items, (dialog, which) -> {
                        canvasView.setSelectedElementId(list.get(which).getId());
                    })
                    .setPositiveButton("Fechar", null)
                    .show();
        }
    
        private void showSaveDialog() {
            EditText input = new EditText(this);
            input.setText(currentTemplate.getName());
            input.setTextColor(Color.WHITE);
    
            new AlertDialog.Builder(this)
                    .setTitle("Salvar Modelo")
                    .setMessage("Deseja salvar as alterações neste modelo ou criar uma nova cópia?")
                    .setView(input)
                    .setPositiveButton("Salvar", (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            currentTemplate.setName(name);
                        }
                        boolean saved = TemplateStorage.saveTemplate(this, currentTemplate);
                        if (saved) {
                            editorTitle.setText(currentTemplate.getName().toUpperCase());
                            Toast.makeText(this, "Modelo salvo com sucesso!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Falha ao salvar o modelo.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNeutralButton("Salvar como Novo", (dialog, which) -> {
                        String name = input.getText().toString().trim();
                        RecapTemplate copy = currentTemplate.duplicate(name.isEmpty() ? currentTemplate.getName() + " (Cópia)" : name);
                        TemplateStorage.saveTemplate(this, copy);
                        Toast.makeText(this, "Salvo como novo modelo!", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        }
    
        private void exportRecapAsImage() {
            if (Build.VERSION.SDK_INT < 29) {
                Toast.makeText(this, "Exportação requer Android 10+.", Toast.LENGTH_LONG).show();
                return;
            }
    
            Bitmap exported = RecapCanvasRenderer.renderToBitmap(
                    this,
                    currentTemplate,
                    recapData,
                    artistImage,
                    1080,
                    1920,
                    dynamicBgColor,
                    dynamicAccentColor
            );
    
            String fileName = "FreeAudioTime_Custom_" + System.currentTimeMillis() + ".png";
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FreeAudioTime");
    
            Uri uri = null;
            try {
                uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (uri == null) throw new Exception("Não foi possível criar arquivo na galeria.");
    
                try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                    if (os == null || !exported.compress(Bitmap.CompressFormat.PNG, 100, os)) {
                        throw new Exception("Falha ao gravar PNG.");
                    }
                }
                Toast.makeText(this, "Recap salvo na galeria com sucesso!", Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                if (uri != null) {
                    getContentResolver().delete(uri, null, null);
                }
                Toast.makeText(this, "Erro ao exportar imagem.", Toast.LENGTH_SHORT).show();
            } finally {
                exported.recycle();
            }
        }
    }
