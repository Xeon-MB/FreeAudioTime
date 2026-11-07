package com.example.freeaudiotime;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

/**
 * Template Library Activity: Browse, create, duplicate, rename, delete templates,
 * and set category defaults.
 */
public class TemplateLibraryActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "extra_category_id";
    public static final String EXTRA_DATE_STRING = "extra_date_string";
    public static final String RESULT_TEMPLATE_ID = "result_template_id";

    private String categoryId;
    private String dateString;
    private RecapCategory category;

    private LinearLayout templateListContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applySavedTheme(this);
        super.onCreate(savedInstanceState);

        categoryId = getIntent().getStringExtra(EXTRA_CATEGORY_ID);
        dateString = getIntent().getStringExtra(EXTRA_DATE_STRING);
        category = RecapCategory.fromId(categoryId);

        // Build layout programmatically
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0D110E"));

        // Top bar
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#151C17"));
        topBar.setPadding(dp(12), dp(12), dp(12), dp(12));
        topBar.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView backBtn = new TextView(this);
        backBtn.setText("← VOLTAR");
        backBtn.setTextColor(Color.parseColor("#F3F1E7"));
        backBtn.setTextSize(13f);
        backBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        backBtn.setPadding(0, 0, dp(16), 0);
        backBtn.setOnClickListener(v -> finish());

        TextView titleView = new TextView(this);
        titleView.setText("BIBLIOTECA DE MODELOS");
        titleView.setTextColor(Color.parseColor("#B7FF00"));
        titleView.setTextSize(14f);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        titleView.setLayoutParams(titleParams);

        Button btnNewTemplate = new Button(this);
        btnNewTemplate.setText("+ NOVO");
        btnNewTemplate.setTextSize(12f);
        btnNewTemplate.setTextColor(Color.parseColor("#080A08"));
        btnNewTemplate.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B7FF00")));
        btnNewTemplate.setOnClickListener(v -> createNewTemplate());

        topBar.addView(backBtn);
        topBar.addView(titleView);
        topBar.addView(btnNewTemplate);

        // Category info banner
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setBackgroundColor(Color.parseColor("#1B2B1E"));
        banner.setPadding(dp(16), dp(10), dp(16), dp(10));

        TextView bannerTitle = new TextView(this);
        bannerTitle.setText("Modelos para: " + category.getDisplayName().toUpperCase());
        bannerTitle.setTextColor(Color.parseColor("#B7FF00"));
        bannerTitle.setTextSize(12f);
        bannerTitle.setTypeface(null, android.graphics.Typeface.BOLD);

        String currentDefaultId = TemplateStorage.getDefaultTemplateIdForCategory(this, category);
        TextView bannerSub = new TextView(this);
        bannerSub.setText("Padrão atual: selecione um modelo e use \"Definir Padrão\"");
        bannerSub.setTextColor(Color.parseColor("#A0B0A0"));
        bannerSub.setTextSize(11f);

        banner.addView(bannerTitle);
        banner.addView(bannerSub);

        // Scrollable template list
        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        scroll.setLayoutParams(scrollParams);
        scroll.setBackgroundColor(Color.parseColor("#0D110E"));

        templateListContainer = new LinearLayout(this);
        templateListContainer.setOrientation(LinearLayout.VERTICAL);
        templateListContainer.setPadding(dp(12), dp(8), dp(12), dp(8));
        scroll.addView(templateListContainer);

        root.addView(topBar);
        root.addView(banner);
        root.addView(scroll);

        setContentView(root);

        loadTemplates();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTemplates();
    }

    private void loadTemplates() {
        templateListContainer.removeAllViews();
        List<RecapTemplate> templates = TemplateStorage.getAllTemplates(this);
        String currentDefaultId = TemplateStorage.getDefaultTemplateIdForCategory(this, category);

        for (RecapTemplate template : templates) {
            addTemplateCard(template, currentDefaultId);
        }
    }

    private void addTemplateCard(RecapTemplate template, String currentDefaultId) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.parseColor("#151C17"));
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardParams);

        // Name row
        LinearLayout nameRow = new LinearLayout(this);
        nameRow.setOrientation(LinearLayout.HORIZONTAL);
        nameRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

        TextView nameView = new TextView(this);
        nameView.setText(template.getName());
        nameView.setTextColor(Color.parseColor("#F3F1E7"));
        nameView.setTextSize(14f);
        nameView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        nameView.setLayoutParams(nameParams);

        boolean isDefault = template.getId().equals(currentDefaultId);
        if (isDefault) {
            TextView defaultBadge = new TextView(this);
            defaultBadge.setText("✓ PADRÃO");
            defaultBadge.setTextColor(Color.parseColor("#080A08"));
            defaultBadge.setBackgroundColor(Color.parseColor("#B7FF00"));
            defaultBadge.setTextSize(10f);
            defaultBadge.setTypeface(null, android.graphics.Typeface.BOLD);
            defaultBadge.setPadding(dp(6), dp(2), dp(6), dp(2));
            nameRow.addView(nameView);
            nameRow.addView(defaultBadge);
        } else {
            nameRow.addView(nameView);
        }

        // Type badge
        TextView typeView = new TextView(this);
        typeView.setText(template.isSystemPreset() ? "PRESET DO SISTEMA" : "MODELO DO USUÁRIO");
        typeView.setTextColor(template.isSystemPreset() ? Color.parseColor("#6A9A6A") : Color.parseColor("#9A7A5A"));
        typeView.setTextSize(10f);
        LinearLayout.LayoutParams typeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        typeParams.setMargins(0, dp(2), 0, dp(8));
        typeView.setLayoutParams(typeParams);

        // Action buttons row
        LinearLayout actionsRow = new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);

        Button btnEdit = makeSmallButton("✏ EDITAR", "#2C3D31", "#F3F1E7");
        btnEdit.setOnClickListener(v -> openEditor(template));

        Button btnSetDefault = makeSmallButton("★ DEF. PADRÃO", "#1A2B4A", "#7AAAFF");
        btnSetDefault.setOnClickListener(v -> setAsDefault(template));

        Button btnDuplicate = makeSmallButton("⧉ DUPLICAR", "#2C3D31", "#F3F1E7");
        btnDuplicate.setOnClickListener(v -> duplicateTemplate(template));

        actionsRow.addView(btnEdit);
        actionsRow.addView(btnSetDefault);
        actionsRow.addView(btnDuplicate);

        if (!template.isSystemPreset()) {
            Button btnRename = makeSmallButton("✎ RENOMEAR", "#2C3D31", "#F3F1E7");
            btnRename.setOnClickListener(v -> renameTemplate(template));
            actionsRow.addView(btnRename);

            Button btnDelete = makeSmallButton("✕ EXCLUIR", "#552222", "#FF8888");
            btnDelete.setOnClickListener(v -> deleteTemplate(template));
            actionsRow.addView(btnDelete);
        }

        card.addView(nameRow);
        card.addView(typeView);
        card.addView(actionsRow);
        templateListContainer.addView(card);
    }

    private Button makeSmallButton(String text, String bgHex, String textHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(10f);
        btn.setTextColor(Color.parseColor(textHex));
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(bgHex)));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(32));
        p.setMargins(0, 0, dp(6), 0);
        btn.setLayoutParams(p);
        btn.setPadding(dp(8), 0, dp(8), 0);
        return btn;
    }

    private void openEditor(RecapTemplate template) {
        Intent intent = new Intent(this, RecapEditorActivity.class);
        intent.putExtra(RecapEditorActivity.EXTRA_TEMPLATE_ID, template.getId());
        intent.putExtra(RecapEditorActivity.EXTRA_CATEGORY_ID, categoryId);
        if (dateString != null) intent.putExtra(RecapEditorActivity.EXTRA_DATE_STRING, dateString);
        startActivity(intent);
    }

    private void setAsDefault(RecapTemplate template) {
        TemplateStorage.setDefaultTemplateForCategory(this, category, template.getId());
        Toast.makeText(this, "\"" + template.getName() + "\" definido como padrão para " + category.getDisplayName(), Toast.LENGTH_SHORT).show();
        loadTemplates();
    }

    private void duplicateTemplate(RecapTemplate template) {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setText(template.getName() + " (Cópia)");
        nameInput.setTextColor(Color.WHITE);
        new AlertDialog.Builder(this)
                .setTitle("Duplicar Modelo")
                .setMessage("Nome para a cópia:")
                .setView(nameInput)
                .setPositiveButton("Duplicar", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    RecapTemplate copy = template.duplicate(name.isEmpty() ? null : name);
                    if (TemplateStorage.saveTemplate(this, copy)) {
                        Toast.makeText(this, "Modelo duplicado!", Toast.LENGTH_SHORT).show();
                        loadTemplates();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void renameTemplate(RecapTemplate template) {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setText(template.getName());
        nameInput.setTextColor(Color.WHITE);
        new AlertDialog.Builder(this)
                .setTitle("Renomear Modelo")
                .setView(nameInput)
                .setPositiveButton("Salvar", (d, w) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        template.setName(newName);
                        if (TemplateStorage.saveTemplate(this, template)) {
                            Toast.makeText(this, "Renomeado!", Toast.LENGTH_SHORT).show();
                            loadTemplates();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteTemplate(RecapTemplate template) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Modelo")
                .setMessage("Excluir \"" + template.getName() + "\"? Essa ação não pode ser desfeita.")
                .setPositiveButton("Excluir", (d, w) -> {
                    if (TemplateStorage.deleteTemplate(this, template.getId())) {
                        Toast.makeText(this, "Modelo excluído.", Toast.LENGTH_SHORT).show();
                        loadTemplates();
                    } else {
                        Toast.makeText(this, "Não foi possível excluir.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void createNewTemplate() {
        android.widget.EditText nameInput = new android.widget.EditText(this);
        nameInput.setHint("Nome do novo modelo");
        nameInput.setTextColor(Color.WHITE);
        new AlertDialog.Builder(this)
                .setTitle("Novo Modelo")
                .setMessage("Informe o nome do novo modelo:")
                .setView(nameInput)
                .setPositiveButton("Criar e Editar", (d, w) -> {
                    String name = nameInput.getText().toString().trim();
                    // Create a new user template based on the default brutalist
                    RecapTemplate base = DefaultTemplatesFactory.createDefaultBrutalist();
                    RecapTemplate newTemplate = base.duplicate(name.isEmpty() ? "Novo Modelo" : name);
                    if (TemplateStorage.saveTemplate(this, newTemplate)) {
                        openEditor(newTemplate);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private int dp(int dpValue) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dpValue * density);
    }
}
