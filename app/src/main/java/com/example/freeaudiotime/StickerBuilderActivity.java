package com.example.freeaudiotime;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.SweepGradient;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Freeform sticker designer. Artwork is created on a transparent 512x512 canvas
 * and exported as a PNG that can be placed in a recap.
 */
public class StickerBuilderActivity extends AppCompatActivity {

    public static final String EXTRA_STICKER_PATH = "extra_sticker_path";
    private static final int ART_SIZE = 512;

    private StickerDrawingView drawingView;
    private int currentColor = Color.rgb(183, 255, 0);
    private float currentStrokeWidth = 9f;
    private int selectedTextIndex = -1;
    private boolean movingSelectedText = false;
    private float lastTouchX, lastTouchY;

    private enum Tool { BRUSH, LINE, RECTANGLE, ELLIPSE, ERASER }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(10, 14, 11));
        getWindow().setNavigationBarColor(Color.rgb(10, 14, 11));
        getWindow().getDecorView().setSystemUiVisibility(0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(10, 14, 11));
        root.setPadding(dp(10), dp(8), dp(10), dp(10));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(4), dp(2), dp(4), dp(2));
        top.setBackgroundColor(Color.rgb(24, 31, 26));
        addButton(top, "Voltar", v -> finish());
        addButton(top, "Desfazer", v -> drawingView.undo());
        addButton(top, "Refazer", v -> drawingView.redo());
        addButton(top, "Limpar", v -> confirmClear());
        root.addView(top, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(48)));

        TextView hint = new TextView(this);
        hint.setText("CRIE SEU STICKER  •  FUNDO TRANSPARENTE");
        hint.setTextColor(Color.rgb(183, 255, 0));
        hint.setTextSize(11);
        hint.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, dp(8), 0, dp(8));
        root.addView(hint);

        drawingView = new StickerDrawingView();
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        canvasParams.setMargins(0, dp(4), 0, dp(8));
        root.addView(drawingView, canvasParams);

        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout tools = new LinearLayout(this);
        tools.setGravity(Gravity.CENTER_VERTICAL);
        addToolButton(tools, "Pincel", Tool.BRUSH);
        addToolButton(tools, "Linha", Tool.LINE);
        addToolButton(tools, "Retângulo", Tool.RECTANGLE);
        addToolButton(tools, "Círculo", Tool.ELLIPSE);
        addToolButton(tools, "Borracha", Tool.ERASER);
        toolsScroll.addView(tools);
        root.addView(toolsScroll);

        TextView colorLabel = new TextView(this);
        colorLabel.setText("CORES");
        colorLabel.setTextColor(Color.LTGRAY);
        colorLabel.setTextSize(10);
        colorLabel.setPadding(dp(2), dp(8), 0, dp(3));
        root.addView(colorLabel);

        LinearLayout colors = new LinearLayout(this);
        colors.setGravity(Gravity.CENTER_VERTICAL);
        int[] palette = {
                Color.rgb(183, 255, 0), Color.WHITE, Color.BLACK,
                Color.rgb(0, 229, 255), Color.rgb(255, 90, 165),
                Color.rgb(255, 213, 79), Color.rgb(151, 104, 255),
                Color.rgb(255, 112, 67)
        };
        for (int color : palette) {
            View swatch = new View(this);
            android.graphics.drawable.GradientDrawable shape =
                    new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            shape.setColor(color);
            shape.setStroke(dp(1), Color.rgb(100, 110, 102));
            swatch.setBackground(shape);
            LinearLayout.LayoutParams swatchParams = new LinearLayout.LayoutParams(dp(30), dp(30));
            swatchParams.setMargins(dp(4), dp(2), dp(4), dp(2));
            colors.addView(swatch, swatchParams);
            swatch.setOnClickListener(v -> {
                currentColor = color;
                drawingView.setColor(color);
            });
        }
        Button customColor = new Button(this);
        customColor.setText("Roda de cores");
        customColor.setTextSize(10);
        customColor.setOnClickListener(v -> showColorWheelDialog());
        colors.addView(customColor, new LinearLayout.LayoutParams(dp(112), dp(38)));
        root.addView(colors);

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView sizeLabel = new TextView(this);
        sizeLabel.setText("ESPESSURA");
        sizeLabel.setTextColor(Color.LTGRAY);
        sizeLabel.setTextSize(10);
        sizeRow.addView(sizeLabel);
        SeekBar size = new SeekBar(this);
        size.setMax(39);
        size.setProgress(8);
        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentStrokeWidth = progress + 1;
                drawingView.setStrokeWidth(currentStrokeWidth);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        sizeRow.addView(size, new LinearLayout.LayoutParams(0, dp(42), 1f));
        root.addView(sizeRow);

        LinearLayout textActions = new LinearLayout(this);
        textActions.setGravity(Gravity.CENTER_VERTICAL);
        Button addText = new Button(this);
        addText.setText("Adicionar texto");
        addText.setOnClickListener(v -> showTextDialog());
        textActions.addView(addText, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button editText = new Button(this);
        editText.setText("Editar");
        editText.setOnClickListener(v -> editSelectedText());
        textActions.addView(editText, new LinearLayout.LayoutParams(0, dp(44), 1f));
        Button fontText = new Button(this);
        fontText.setText("Fonte");
        fontText.setOnClickListener(v -> showFontPickerDialog());
        textActions.addView(fontText, new LinearLayout.LayoutParams(0, dp(44), 1f));
        root.addView(textActions);

        TextView textSizeLabel = new TextView(this);
        textSizeLabel.setText("TAMANHO DO TEXTO");
        textSizeLabel.setTextColor(Color.LTGRAY);
        textSizeLabel.setTextSize(10);
        root.addView(textSizeLabel);
        SeekBar textSize = new SeekBar(this);
        textSize.setMax(176);
        textSize.setProgress(40);
        textSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                drawingView.updateSelectedTextSize(24f + progress);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(textSize, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34)));

        TextView rotationLabel = new TextView(this);
        rotationLabel.setText("ROTAÇÃO DO TEXTO");
        rotationLabel.setTextColor(Color.LTGRAY);
        rotationLabel.setTextSize(10);
        root.addView(rotationLabel);
        SeekBar textRotation = new SeekBar(this);
        textRotation.setMax(360);
        textRotation.setProgress(180);
        textRotation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                drawingView.updateSelectedTextRotation(progress - 180f);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        root.addView(textRotation, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34)));

        Button save = new Button(this);
        save.setText("SALVAR STICKER");
        save.setTextColor(Color.rgb(10, 14, 11));
        save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(183, 255, 0)));
        save.setOnClickListener(v -> saveSticker());
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50));
        saveParams.topMargin = dp(6);
        root.addView(save, saveParams);

        setContentView(root);
    }

    private void addToolButton(LinearLayout row, String label, Tool tool) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setOnClickListener(v -> drawingView.setTool(tool));
        row.addView(button, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44)));
    }

    private void addButton(LinearLayout row, String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(10);
        button.setTextColor(Color.WHITE);
        button.setPadding(dp(3), 0, dp(3), 0);
        button.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(38, 49, 41)));
        button.setOnClickListener(listener);
        row.addView(button, new LinearLayout.LayoutParams(0, dp(44), 1f));
    }

    private void showTextDialog() {
        android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(false);
        input.setHint("Digite seu texto");
        new AlertDialog.Builder(this)
                .setTitle("Adicionar texto")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Adicionar", (dialog, which) -> {
                    String value = input.getText().toString().trim();
                    if (!value.isEmpty()) drawingView.addText(value, currentColor);
                })
                .show();
    }

    private void showColorWheelDialog() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(8));
        ColorWheelView wheel = new ColorWheelView();
        content.addView(wheel, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(250)));
        View preview = new View(this);
        android.graphics.drawable.GradientDrawable previewBg =
                new android.graphics.drawable.GradientDrawable();
        previewBg.setCornerRadius(dp(10));
        previewBg.setColor(currentColor);
        preview.setBackground(previewBg);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(34));
        previewParams.topMargin = dp(10);
        content.addView(preview, previewParams);
        wheel.setOnColorChangedListener(color -> {
            currentColor = color;
            previewBg.setColor(color);
            preview.invalidate();
        });
        new AlertDialog.Builder(this)
                .setTitle("Escolha uma cor")
                .setView(content)
                .setPositiveButton("Aplicar", (dialog, which) -> drawingView.setColor(currentColor))
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private class ColorWheelView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] hsv = {0f, 1f, 1f};
        private android.graphics.RectF bounds = new android.graphics.RectF();
        private ColorWheelListener listener;

        ColorWheelView() { super(StickerBuilderActivity.this); }
        void setOnColorChangedListener(ColorWheelListener callback) { listener = callback; }

        private Bitmap wheelBitmap;

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float radius = Math.min(getWidth(), getHeight()) * 0.43f;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            if (wheelBitmap == null) {
                int size = 512;
                int[] pixels = new int[size * size];
                float center = (size - 1) / 2f;
                float maxRadius = center;
                float[] colorHsv = new float[3];
                for (int y = 0; y < size; y++) {
                    for (int x = 0; x < size; x++) {
                        float dx = x - center, dy = y - center;
                        float distance = (float) Math.sqrt(dx * dx + dy * dy);
                        int index = y * size + x;
                        if (distance > maxRadius) {
                            pixels[index] = Color.TRANSPARENT;
                        } else {
                            colorHsv[0] = (float) ((Math.toDegrees(Math.atan2(dy, dx)) + 360f) % 360f);
                            colorHsv[1] = Math.min(1f, distance / maxRadius);
                            colorHsv[2] = 1f;
                            pixels[index] = Color.HSVToColor(colorHsv);
                        }
                    }
                }
                wheelBitmap = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888);
            }
            paint.setShader(null);
            paint.setFilterBitmap(true);
            canvas.drawBitmap(wheelBitmap, null,
                    new android.graphics.RectF(cx - radius, cy - radius, cx + radius, cy + radius), paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(currentColor);
            canvas.drawCircle(cx, cy, dp(10), paint);
        }

        @Override public boolean onTouchEvent(MotionEvent event) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                    || event.getActionMasked() == MotionEvent.ACTION_MOVE) {
                float dx = event.getX() - getWidth() / 2f;
                float dy = event.getY() - getHeight() / 2f;
                float radius = Math.min(getWidth(), getHeight()) * 0.43f;
                float angle = (float) Math.toDegrees(Math.atan2(dy, dx));
                hsv[0] = (angle + 360f) % 360f;
                hsv[1] = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy) / radius);
                hsv[2] = 1f;
                currentColor = Color.HSVToColor(hsv);
                if (listener != null) listener.onColorChanged(currentColor);
                invalidate();
                return true;
            }
            return true;
        }
    }

    private interface ColorWheelListener { void onColorChanged(int color); }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("Limpar desenho?")
                .setMessage("Todos os elementos desta criação serão removidos.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Limpar", (dialog, which) -> drawingView.clearArtwork())
                .show();
    }

    private void saveSticker() {
        if (!drawingView.hasArtwork()) {
            Toast.makeText(this, "Desenhe ou adicione algo antes de salvar.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Bitmap bitmap = drawingView.exportBitmap();
            File file = new File(getFilesDir(), "sticker_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream output = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
            }
            bitmap.recycle();

            android.content.Intent result = new android.content.Intent();
            result.putExtra(EXTRA_STICKER_PATH, file.getAbsolutePath());
            setResult(RESULT_OK, result);
            finish();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível salvar o sticker.", Toast.LENGTH_LONG).show();
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private class StickerDrawingView extends View {
        private final List<DrawCommand> commands = new ArrayList<>();
        private final Deque<List<DrawCommand>> undo = new ArrayDeque<>();
        private final Deque<List<DrawCommand>> redo = new ArrayDeque<>();
        private Tool tool = Tool.BRUSH;
        private int color = currentColor;
        private float strokeWidth = currentStrokeWidth;
        private float scale = 1f;
        private float offsetX;
        private float offsetY;
        private DrawCommand active;
        private String pendingText;

        StickerDrawingView() {
            super(StickerBuilderActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        void setTool(Tool next) { tool = next; pendingText = null; invalidate(); }
        void setColor(int next) { color = next; }
        void setStrokeWidth(float next) { strokeWidth = next; }

        boolean hasArtwork() { return !commands.isEmpty(); }

        void addText(String text, int textColor) {
            saveUndo();
            DrawCommand command = new DrawCommand();
            command.tool = Tool.BRUSH;
            command.kind = "TEXT";
            command.text = text;
            command.color = textColor;
            command.textSize = 64f;
            command.fontFamily = "SANS_SERIF";
            command.x1 = ART_SIZE / 2f;
            command.y1 = ART_SIZE / 2f;
            commands.add(command);
            selectedTextIndex = commands.size() - 1;
            redo.clear();
            invalidate();
        }

        void updateSelectedTextSize(float size) {
            DrawCommand command = getSelectedText();
            if (command == null) return;
            command.textSize = size;
            invalidate();
        }

        void updateSelectedTextRotation(float rotation) {
            DrawCommand command = getSelectedText();
            if (command == null) return;
            command.rotation = rotation;
            invalidate();
        }

        private DrawCommand getSelectedText() {
            if (selectedTextIndex < 0 || selectedTextIndex >= commands.size()) return null;
            DrawCommand command = commands.get(selectedTextIndex);
            return "TEXT".equals(command.kind) ? command : null;
        }

        void clearArtwork() {
            if (commands.isEmpty()) return;
            saveUndo();
            commands.clear();
            redo.clear();
            invalidate();
        }

        void undo() {
            if (undo.isEmpty()) return;
            redo.push(copyCommands(commands));
            commands.clear();
            commands.addAll(undo.pop());
            invalidate();
        }

        void redo() {
            if (redo.isEmpty()) return;
            undo.push(copyCommands(commands));
            commands.clear();
            commands.addAll(redo.pop());
            invalidate();
        }

        private void saveUndo() {
            undo.push(copyCommands(commands));
            while (undo.size() > 40) undo.removeLast();
        }

        private List<DrawCommand> copyCommands(List<DrawCommand> source) {
            List<DrawCommand> copy = new ArrayList<>();
            for (DrawCommand command : source) copy.add(command.copy());
            return copy;
        }

        Bitmap exportBitmap() {
            Bitmap bitmap = Bitmap.createBitmap(ART_SIZE, ART_SIZE, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.saveLayer(0, 0, ART_SIZE, ART_SIZE, null);
            for (DrawCommand command : commands) drawCommand(canvas, command);
            canvas.restore();
            return bitmap;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            scale = Math.min(getWidth(), getHeight()) / (float) ART_SIZE;
            offsetX = (getWidth() - ART_SIZE * scale) / 2f;
            offsetY = (getHeight() - ART_SIZE * scale) / 2f;
            drawCheckerboard(canvas);

            canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
            canvas.save();
            canvas.translate(offsetX, offsetY);
            canvas.scale(scale, scale);
            for (DrawCommand command : commands) drawCommand(canvas, command);
            if (active != null) drawCommand(canvas, active);
            canvas.restore();
            canvas.restore();
        }

        private void drawCheckerboard(Canvas canvas) {
            canvas.drawColor(Color.rgb(236, 238, 235));
            Paint tile = new Paint(Paint.ANTI_ALIAS_FLAG);
            tile.setColor(Color.rgb(205, 210, 205));
            int cell = dp(18);
            for (int y = 0; y < getHeight(); y += cell) {
                for (int x = 0; x < getWidth(); x += cell) {
                    if (((x / cell) + (y / cell)) % 2 == 0) {
                        canvas.drawRect(x, y, x + cell, y + cell, tile);
                    }
                }
            }
            Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
            border.setColor(Color.rgb(110, 120, 112));
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(dp(1));
            canvas.drawRect(offsetX, offsetY, offsetX + ART_SIZE * scale,
                    offsetY + ART_SIZE * scale, border);
        }

        private void drawCommand(Canvas canvas, DrawCommand command) {
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(command.color);
            paint.setStrokeWidth(command.width);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);

            if ("TEXT".equals(command.kind)) {
                paint.setStyle(Paint.Style.FILL);
                paint.setTypeface(createTypeface(command.fontFamily));
                paint.setTextSize(command.textSize);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.save();
                canvas.rotate(command.rotation, command.x1, command.y1);
                String[] lines = command.text.split("\\n");
                float lineHeight = command.textSize * 1.2f;
                float startY = command.y1 - (lines.length - 1) * lineHeight / 2f;
                for (int i = 0; i < lines.length; i++) {
                    canvas.drawText(lines[i], command.x1, startY + i * lineHeight, paint);
                }
                if (commands.indexOf(command) == selectedTextIndex) {
                    Paint selection = new Paint(Paint.ANTI_ALIAS_FLAG);
                    selection.setColor(Color.rgb(183, 255, 0));
                    selection.setStyle(Paint.Style.STROKE);
                    selection.setStrokeWidth(2f);
                    float widest = 0f;
                    for (String line : lines) widest = Math.max(widest, paint.measureText(line));
                    canvas.drawRect(command.x1 - widest / 2f - 8f,
                            command.y1 - command.textSize * lines.length * 0.65f,
                            command.x1 + widest / 2f + 8f,
                            command.y1 + command.textSize * lines.length * 0.65f, selection);
                }
                canvas.restore();
                return;
            }

            if (command.tool == Tool.ERASER) {
                paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                paint.setStyle(Paint.Style.STROKE);
            } else {
                paint.setStyle(Paint.Style.STROKE);
            }

            switch (command.tool) {
                case BRUSH:
                case ERASER:
                    if (command.path != null) canvas.drawPath(command.path, paint);
                    break;
                case LINE:
                    canvas.drawLine(command.x1, command.y1, command.x2, command.y2, paint);
                    break;
                case RECTANGLE:
                    canvas.drawRect(Math.min(command.x1, command.x2), Math.min(command.y1, command.y2),
                            Math.max(command.x1, command.x2), Math.max(command.y1, command.y2), paint);
                    break;
                case ELLIPSE:
                    canvas.drawOval(new RectF(Math.min(command.x1, command.x2), Math.min(command.y1, command.y2),
                            Math.max(command.x1, command.x2), Math.max(command.y1, command.y2)), paint);
                    break;
            }
            paint.setXfermode(null);
        }

        private DrawCommand findTextAt(float x, float y) {
            for (int i = commands.size() - 1; i >= 0; i--) {
                DrawCommand command = commands.get(i);
                if (!"TEXT".equals(command.kind)) continue;
                Paint measure = new Paint(Paint.ANTI_ALIAS_FLAG);
                measure.setTypeface(createTypeface(command.fontFamily));
                measure.setTextSize(command.textSize);
                float width = 0f;
                for (String line : command.text.split("\\n")) {
                    width = Math.max(width, measure.measureText(line));
                }
                float dx = x - command.x1, dy = y - command.y1;
                double radians = Math.toRadians(-command.rotation);
                float localX = (float) (dx * Math.cos(radians) - dy * Math.sin(radians));
                float localY = (float) (dx * Math.sin(radians) + dy * Math.cos(radians));
                if (Math.abs(localX) <= width / 2f + 12f
                        && Math.abs(localY) <= command.textSize * command.text.split("\\n").length * 0.7f) {
                    return command;
                }
            }
            return null;
        }

        private float[] toArtwork(float x, float y) {
            if (scale <= 0f) return new float[]{0f, 0f};
            return new float[]{
                    Math.max(0f, Math.min(ART_SIZE, (x - offsetX) / scale)),
                    Math.max(0f, Math.min(ART_SIZE, (y - offsetY) / scale))
            };
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            float[] point = toArtwork(event.getX(), event.getY());
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    DrawCommand hit = findTextAt(point[0], point[1]);
                    if (hit != null) {
                        selectedTextIndex = commands.indexOf(hit);
                        saveUndo();
                        movingSelectedText = true;
                        lastTouchX = point[0];
                        lastTouchY = point[1];
                        invalidate();
                        return true;
                    }
                    selectedTextIndex = -1;
                    movingSelectedText = false;
                    saveUndo();
                    active = new DrawCommand();
                    active.tool = tool;
                    active.color = color;
                    active.width = strokeWidth;
                    active.x1 = point[0];
                    active.y1 = point[1];
                    active.x2 = point[0];
                    active.y2 = point[1];
                    if (tool == Tool.BRUSH || tool == Tool.ERASER) {
                        active.path = new Path();
                        active.path.moveTo(point[0], point[1]);
                    }
                    invalidate();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (movingSelectedText) {
                        DrawCommand selected = getSelectedText();
                        if (selected != null) {
                            selected.x1 += point[0] - lastTouchX;
                            selected.y1 += point[1] - lastTouchY;
                            lastTouchX = point[0];
                            lastTouchY = point[1];
                            invalidate();
                        }
                        return true;
                    }
                    if (active != null) {
                        active.x2 = point[0];
                        active.y2 = point[1];
                        if (active.path != null) active.path.lineTo(point[0], point[1]);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (movingSelectedText) {
                        movingSelectedText = false;
                        redo.clear();
                        invalidate();
                        return true;
                    }
                    if (active != null) {
                        if (Math.abs(active.x2 - active.x1) < 1f
                                && Math.abs(active.y2 - active.y1) < 1f
                                && active.path != null) {
                            active.path.lineTo(active.x1 + 0.5f, active.y1 + 0.5f);
                        }
                        commands.add(active);
                        active = null;
                        redo.clear();
                        invalidate();
                    }
                    return true;
            }
            return true;
        }
    }

    private void editSelectedText() {
        DrawCommand selected = drawingView.getSelectedText();
        if (selected == null) {
            Toast.makeText(this, "Selecione um texto no canvas primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        android.widget.EditText input = new android.widget.EditText(this);
        input.setText(selected.text);
        input.setMinLines(2);
        new AlertDialog.Builder(this)
                .setTitle("Editar texto")
                .setView(input)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialog, which) -> {
                    String value = input.getText().toString();
                    if (!value.trim().isEmpty()) {
                        selected.text = value;
                        drawingView.invalidate();
                    }
                }).show();
    }

    private void showFontPickerDialog() {
        DrawCommand selected = drawingView.getSelectedText();
        if (selected == null) {
            Toast.makeText(this, "Selecione um texto no canvas primeiro.", Toast.LENGTH_SHORT).show();
            return;
        }
        java.util.ArrayList<String> names = new java.util.ArrayList<>(java.util.Arrays.asList(
                "Sans — padrão", "Serif — editorial", "Monospace — máquina",
                "Condensada — pôster", "Sans leve", "Sans fina", "Sans média",
                "Sans pesada", "Sans extra pesada", "Arredondada", "Cursiva — manuscrita",
                "Casual", "Fantasy", "Serif em negrito", "Serif itálica",
                "Sans negrito itálica", "Small caps", "Condensada negrito"
        ));
        java.util.ArrayList<String> codes = new java.util.ArrayList<>(java.util.Arrays.asList(
                "SANS_SERIF", "SERIF", "MONOSPACE", "CONDENSED", "SANS_LIGHT",
                "SANS_THIN", "SANS_MEDIUM", "SANS_BLACK", "SANS_EXTRA_BLACK",
                "SANS_ROUNDED", "CURSIVE", "CASUAL", "FANTASY", "SERIF_BOLD",
                "SERIF_ITALIC", "SANS_BOLD_ITALIC", "SMALL_CAPS", "CONDENSED_BOLD"
        ));
        File[] importedFonts = getFilesDir().listFiles((dir, name) ->
                name.startsWith("recap_font_") &&
                (name.endsWith(".ttf") || name.endsWith(".otf") || name.endsWith(".ttc")));
        if (importedFonts != null) {
            java.util.Arrays.sort(importedFonts, java.util.Comparator.comparing(File::getName));
            for (File fontFile : importedFonts) {
                codes.add("CUSTOM_FONT:" + fontFile.getName());
                names.add("Importada: " + fontFile.getName().replaceFirst("^recap_font_", ""));
            }
        }
        new AlertDialog.Builder(this).setTitle("Escolha uma fonte")
                .setItems(names.toArray(new String[0]), (dialog, which) -> {
                    selected.fontFamily = codes.get(which);
                    drawingView.invalidate();
                }).setNegativeButton("Cancelar", null).show();
    }

    private Typeface createTypeface(String family) {
        if (family != null && family.startsWith("CUSTOM_FONT:")) {
            File fontFile = new File(getFilesDir(), family.substring("CUSTOM_FONT:".length()));
            if (fontFile.exists()) {
                try { return Typeface.createFromFile(fontFile); }
                catch (RuntimeException ignored) { }
            }
        }
        if ("SERIF".equals(family)) return Typeface.create("serif", Typeface.NORMAL);
        if ("MONOSPACE".equals(family)) return Typeface.create("monospace", Typeface.NORMAL);
        if ("CONDENSED".equals(family)) return Typeface.create("sans-serif-condensed", Typeface.NORMAL);
        if ("SANS_LIGHT".equals(family)) return Typeface.create("sans-serif-light", Typeface.NORMAL);
        if ("SANS_THIN".equals(family)) return Typeface.create("sans-serif-thin", Typeface.NORMAL);
        if ("SANS_MEDIUM".equals(family)) return Typeface.create("sans-serif-medium", Typeface.NORMAL);
        if ("SANS_BLACK".equals(family)) return Typeface.create("sans-serif-black", Typeface.NORMAL);
        if ("SANS_EXTRA_BLACK".equals(family)) return Typeface.create("sans-serif-black", Typeface.BOLD);
        if ("SANS_ROUNDED".equals(family)) return Typeface.create("sans-serif-rounded", Typeface.NORMAL);
        if ("CURSIVE".equals(family)) return Typeface.create("cursive", Typeface.NORMAL);
        if ("CASUAL".equals(family)) return Typeface.create("casual", Typeface.NORMAL);
        if ("FANTASY".equals(family)) return Typeface.create("fantasy", Typeface.NORMAL);
        if ("SERIF_BOLD".equals(family)) return Typeface.create("serif", Typeface.BOLD);
        if ("SERIF_ITALIC".equals(family)) return Typeface.create("serif", Typeface.ITALIC);
        if ("SANS_BOLD_ITALIC".equals(family)) return Typeface.create("sans-serif", Typeface.BOLD_ITALIC);
        if ("SMALL_CAPS".equals(family)) return Typeface.create("sans-serif-smallcaps", Typeface.NORMAL);
        if ("CONDENSED_BOLD".equals(family)) return Typeface.create("sans-serif-condensed", Typeface.BOLD);
        return Typeface.create("sans-serif", Typeface.BOLD);
    }

    private static class DrawCommand {
        Tool tool;
        String kind = "SHAPE";
        int color;
        float width = 8f;
        float x1, y1, x2, y2;
        float textSize;
        float rotation;
        String fontFamily = "SANS_SERIF";
        String text;
        Path path;

        DrawCommand copy() {
            DrawCommand copy = new DrawCommand();
            copy.tool = tool;
            copy.kind = kind;
            copy.color = color;
            copy.width = width;
            copy.x1 = x1;
            copy.y1 = y1;
            copy.x2 = x2;
            copy.y2 = y2;
            copy.textSize = textSize;
            copy.rotation = rotation;
            copy.fontFamily = fontFamily;
            copy.text = text;
            copy.path = path == null ? null : new Path(path);
            return copy;
        }
    }
}
