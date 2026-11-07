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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

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

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    saveCustomImageElement(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recap_editor);

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

        // 2. Load Template (Deep copied to prevent modifying original unless explicitly saved)
        RecapTemplate baseTemplate = TemplateStorage.getTemplateById(this, templateId);
        currentTemplate = baseTemplate.duplicate(baseTemplate.getName());
        editorTitle.setText(currentTemplate.getName().toUpperCase());

        // 3. Resolve Dynamic Data
        JSONObject history = PlaybackStorage.load(this);
        recapData = RecapDataResolver.resolve(this, history, category, selectedDate);
        canvasView.setRecapData(recapData);
        canvasView.setTemplate(currentTemplate);

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
        findViewById(R.id.btnAddImage).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        findViewById(R.id.btnAddSticker).setOnClickListener(v -> showAddStickerDialog());
        findViewById(R.id.btnBackground).setOnClickListener(v -> showBackgroundCustomizer());
        findViewById(R.id.btnLayersManage).setOnClickListener(v -> showLayersDialog());
        findViewById(R.id.btnExportDirect).setOnClickListener(v -> exportRecapAsImage());

        // Contextual Buttons
        findViewById(R.id.btnEditText).setOnClickListener(v -> showEditTextDialog());
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

        EditText input = new EditText(this);
        String currentText = elem.getBinding() == RecapElementBinding.CUSTOM_STATIC_TEXT
                ? elem.getCustomText()
                : recapData.getValue(elem.getBinding());
        input.setText(currentText != null ? currentText : "");
        input.setTextColor(Color.WHITE);

        new AlertDialog.Builder(this)
                .setTitle("Editar Conteúdo do Texto")
                .setView(input)
                .setPositiveButton("Aplicar", (dialog, which) -> {
                    undoRedoManager.pushState(currentTemplate);
                    elem.setCustomText(input.getText().toString());
                    elem.setBinding(RecapElementBinding.CUSTOM_STATIC_TEXT);
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showAddStickerDialog() {
        String[] stickers = {"💿 Disco de Vinil", "🎵 Nota Musical", "🌊 Onda Sonora", "⭐ Estrela", "❤️ Coração", "🔥 Fogo", "🏆 Troféu", "👑 Coroa", "🎧 Fones"};
        String[] codes = {"DISCO", "NOTA", "ONDA", "STAR", "HEART", "FIRE", "TROPHY", "CROWN", "HEADPHONES"};

        new AlertDialog.Builder(this)
                .setTitle("Escolher Sticker")
                .setItems(stickers, (dialog, which) -> {
                    undoRedoManager.pushState(currentTemplate);
                    RecapElementModel sticker = new RecapElementModel(
                            RecapElementBinding.CUSTOM_STATIC_IMAGE,
                            0.35f, 0.4f, 0.25f, 0.15f
                    );
                    sticker.setStickerType(codes[which]);
                    sticker.setFontColorHex("#B7FF00");
                    sticker.setZIndex(currentTemplate.getElements().size());
                    currentTemplate.getElements().add(sticker);
                    canvasView.setSelectedElementId(sticker.getId());
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
                "Cor Sólida (Preto #080A08)",
                "Cor Sólida (Verde Escuro #101D17)",
                "Cor Sólida (Cinza Minimalista #181818)",
                "Gradiente Linear (Verde → Preto)",
                "Gradiente Radial (Centro Iluminado)",
                "Padrão de Pontos (Dots)",
                "Padrão de Grade (Grid)"
        };

        new AlertDialog.Builder(this)
                .setTitle("Estilo de Fundo do Canvas")
                .setItems(options, (dialog, which) -> {
                    undoRedoManager.pushState(currentTemplate);
                    switch (which) {
                        case 0:
                            currentTemplate.setBgStyle("SOLID");
                            currentTemplate.setBackgroundColorHex("#080A08");
                            break;
                        case 1:
                            currentTemplate.setBgStyle("SOLID");
                            currentTemplate.setBackgroundColorHex("#101D17");
                            break;
                        case 2:
                            currentTemplate.setBgStyle("SOLID");
                            currentTemplate.setBackgroundColorHex("#181818");
                            break;
                        case 3:
                            currentTemplate.setBgStyle("GRADIENT_LINEAR");
                            currentTemplate.setBackgroundColorHex("#1A2D23");
                            currentTemplate.setBgSecondaryColorHex("#080A08");
                            break;
                        case 4:
                            currentTemplate.setBgStyle("GRADIENT_RADIAL");
                            currentTemplate.setBackgroundColorHex("#050806");
                            currentTemplate.setBgSecondaryColorHex("#213D2D");
                            break;
                        case 5:
                            currentTemplate.setBgStyle("PATTERN_DOTS");
                            currentTemplate.setBackgroundColorHex("#080A08");
                            currentTemplate.setBgSecondaryColorHex("#304033");
                            break;
                        case 6:
                            currentTemplate.setBgStyle("PATTERN_GRID");
                            currentTemplate.setBackgroundColorHex("#080A08");
                            currentTemplate.setBgSecondaryColorHex("#28392B");
                            break;
                    }
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showColorPickerDialog() {
        RecapElementModel elem = canvasView.getSelectedElement();
        if (elem == null) return;

        String[] colors = {"#B7FF00 (Verde Neon)", "#F0EBDD (Creme)", "#FFFFFF (Branco)", "#1DB954 (Spotify Green)", "#FF5722 (Laranja)", "#E91E63 (Rosa)", "#2196F3 (Azul)", "#080A08 (Preto)"};
        new AlertDialog.Builder(this)
                .setTitle("Escolher Cor")
                .setItems(colors, (dialog, which) -> {
                    undoRedoManager.pushState(currentTemplate);
                    elem.setFontColorHex(colors[which].split(" ")[0]);
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void showFontPickerDialog() {
        RecapElementModel elem = canvasView.getSelectedElement();
        if (elem == null) return;

        String[] fonts = {"Sans-Serif", "Sans-Serif Condensed", "Serif Clássico", "Monospace"};
        String[] codes = {"SANS_SERIF", "CONDENSED", "SERIF", "MONOSPACE"};

        new AlertDialog.Builder(this)
                .setTitle("Tipografia")
                .setItems(fonts, (dialog, which) -> {
                    undoRedoManager.pushState(currentTemplate);
                    elem.setFontFamily(codes[which]);
                    canvasView.invalidate();
                    updateUndoRedoButtons();
                })
                .setNegativeButton("Cancelar", null)
                .show();
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
