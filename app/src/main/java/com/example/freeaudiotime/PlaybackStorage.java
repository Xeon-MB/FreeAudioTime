
package com.example.freeaudiotime;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PlaybackStorage {

    private static final String TAG = "AudioTime";
    private static final String FILE_NAME = "playback_history.json";
    private static final String PREFS = "storage_preferences";
    private static final String TREE_URI = "folder_uri";


    public static synchronized void setFolder(
            Context context, Uri treeUri) {

        try {
            int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

            context.getContentResolver()
                    .takePersistableUriPermission(treeUri, flags);

            SharedPreferences prefs =
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            prefs.edit().putString(TREE_URI, treeUri.toString()).apply();

            DocumentFile root =
                    DocumentFile.fromTreeUri(context, treeUri);

            if (root == null || !root.canWrite()) {
                throw new Exception("Pasta sem permissão de escrita.");
            }

            DocumentFile folder = getTargetFolder(root, true);

            if (folder == null) {
                throw new Exception("Não foi possível acessar a pasta FreeAudioTime.");
            }

            DocumentFile historyFile = findHistoryFile(folder);

            if (historyFile == null) {

                historyFile = folder.createFile(
                        "application/json",
                        FILE_NAME
                );

                if (historyFile == null) {
                    throw new Exception("Não foi possível criar o histórico.");
                }

                JSONObject oldHistory = loadInternal(context);

                writeFile(context, historyFile, oldHistory);

                Log.d(TAG, "Novo histórico criado: " + historyFile.getUri());

            } else {
                Log.d(TAG, "Histórico existente reutilizado: "
                        + historyFile.getUri());
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao selecionar pasta", e);
        }
    }

    public static synchronized boolean hasFolder(Context context) {
        Uri uri = getFolderUri(context);

        if (uri == null) {
            return false;
        }

        DocumentFile folder = DocumentFile.fromTreeUri(context, uri);

        return folder != null && folder.canWrite();
    }

    public static synchronized void save(
            Context context, JSONObject data) {

        try {
            DocumentFile file = getHistoryFile(context);

            if (file == null) {
                Log.w(TAG, "Pasta não selecionada. Salvando internamente.");
                saveInternal(context, data);
                return;
            }

            writeFile(context, file, data);

            Log.d(TAG, "Histórico salvo: " + file.getUri());

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar histórico", e);
        }
    }

    public static synchronized JSONObject load(Context context) {

        try {
            DocumentFile file = getHistoryFile(context);

            if (file == null) {
                Log.d(TAG, "Pasta não selecionada. Carregando histórico interno.");
                return loadInternal(context);
            }

            try (InputStream input =
                         context.getContentResolver().openInputStream(file.getUri())) {

                if (input == null) {
                    return new JSONObject();
                }

                byte[] bytes = input.readAllBytes();

                JSONObject history = new JSONObject(
                        new String(bytes, StandardCharsets.UTF_8)
                );

                Log.d(TAG, "Histórico carregado: " + file.getUri());

                return history;
            }

        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar histórico", e);
            return new JSONObject();
        }
    }


    private static DocumentFile getHistoryFile(Context context) {

        Uri treeUri = getFolderUri(context);

        if (treeUri == null) {
            return null;
        }

        DocumentFile root = DocumentFile.fromTreeUri(context, treeUri);

        if (root == null || !root.canWrite()) {
            Log.e(TAG, "Pasta selecionada inválida ou sem permissão.");
            return null;
        }

        DocumentFile folder = getTargetFolder(root, false);

        if (folder == null) {
            Log.e(TAG, "Pasta FreeAudioTime não encontrada.");
            return null;
        }

        DocumentFile file = findHistoryFile(folder);

        if (file == null) {
            Log.e(TAG, "Histórico não encontrado. Nenhum arquivo será criado automaticamente.");
            return null;
        }

        return file;
    }


    private static DocumentFile findHistoryFile(DocumentFile folder) {

        DocumentFile selected = null;

        for (DocumentFile file : folder.listFiles()) {

            if (file.isFile() && FILE_NAME.equals(file.getName())) {

                Log.d(TAG, "JSON encontrado: " + file.getUri()
                        + " | Modificado: " + file.lastModified());

                if (selected == null
                        || file.lastModified() > selected.lastModified()) {
                    selected = file;
                }
            }
        }

        if (selected != null) {
            Log.d(TAG, "Arquivo escolhido: " + selected.getUri());
        }

        return selected;
    }

    private static Uri getFolderUri(Context context) {

        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String uri = prefs.getString(TREE_URI, null);

        return uri == null ? null : Uri.parse(uri);
    }

    private static void writeFile(
            Context context, DocumentFile file, JSONObject data)
            throws Exception {

        try (OutputStream output =
                     context.getContentResolver()
                             .openOutputStream(file.getUri(), "wt")) {

            if (output == null) {
                throw new Exception("Não foi possível abrir o arquivo.");
            }

            output.write(data.toString(4).getBytes(StandardCharsets.UTF_8));
            output.flush();
        }
    }

    private static void saveInternal(
            Context context, JSONObject data) {

        File file = new File(
                context.getFilesDir(),
                FILE_NAME
        );

        try (OutputStream output =
                     new java.io.FileOutputStream(file)) {

            output.write(data.toString(4).getBytes(StandardCharsets.UTF_8));

        } catch (Exception e) {
            Log.e(TAG, "Erro ao salvar internamente", e);
        }
    }

    private static JSONObject loadInternal(Context context) {

        File file = new File(
                context.getFilesDir(),
                FILE_NAME
        );

        if (!file.exists()) {
            return new JSONObject();
        }

        try (InputStream input = new FileInputStream(file)) {

            return new JSONObject(new String(
                    input.readAllBytes(),
                    StandardCharsets.UTF_8
            ));

        } catch (Exception e) {
            Log.e(TAG, "Erro ao carregar histórico interno", e);
            return new JSONObject();
        }
    }


    private static DocumentFile getTargetFolder(
            DocumentFile root, boolean create) {

        if ("FreeAudioTime".equals(root.getName())) {
            return root;
        }

        for (DocumentFile file : root.listFiles()) {

            if (file.isDirectory()
                    && "FreeAudioTime".equals(file.getName())) {
                return file;
            }
        }

        if (create) {
            return root.createDirectory("FreeAudioTime");
        }

        return null;
    }

}
