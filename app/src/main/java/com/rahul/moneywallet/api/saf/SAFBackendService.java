package com.rahul.moneywallet.api.saf;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rahul.moneywallet.R;
import com.rahul.moneywallet.api.AbstractBackendServiceDelegate;
import com.rahul.moneywallet.api.BackendServiceFactory;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

/**
 * Backend service used to access files over the android storage access framework.
 */
public class SAFBackendService extends AbstractBackendServiceDelegate {

    /**
     * Flag containing both {@link Intent#FLAG_GRANT_READ_URI_PERMISSION} and
     * {@link Intent#FLAG_GRANT_WRITE_URI_PERMISSION}.
     */
    private static final int FLAG_URI_READ_WRITE = Intent.FLAG_GRANT_READ_URI_PERMISSION |
    Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
    private static final String PREFERENCE_FILE = "storage_access_framework";
    /**
     * Preference key were the root uri is stored.
     */
    private static final String URI = "uri";
    private ActivityResultLauncher<Uri> launcher;

    public SAFBackendService(BackendServiceStatusListener listener) {
        super(listener);
    }

    /**
     * Extension of the {@link OpenDocumentTree} contract including
     * extra {@link #FLAG_URI_READ_WRITE} and {@link Intent#FLAG_GRANT_PERSISTABLE_URI_PERMISSION}
     * flags.
     */
    private static class DocumentTreeContract extends OpenDocumentTree {
        @NonNull
        @Override
        public Intent createIntent(@NonNull Context context, @Nullable Uri input) {
            Intent intent = super.createIntent(context, input);
            intent.setFlags(FLAG_URI_READ_WRITE |
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            return intent;
        }
    }

    @Override
    public String getId() {
        return BackendServiceFactory.SERVICE_ID_SAF;
    }

    @Override
    public int getName() {
        return R.string.service_backup_storage_access_framework;
    }

    @Override
    public int getBackupCoverMessage() {
        return R.string.cover_message_backup_storage_access_framework_title;
    }

    @Override
    public int getBackupCoverAction() {
        return R.string.cover_message_backup_storage_access_framework_button;
    }

    @Override
    public boolean isServiceEnabled(Context context) {
        return getUri(context) != null;
    }

    @Override
    public void setup(final ComponentActivity activity) {
        launcher.launch(null);
    }

    @Override
    public void teardown(final ComponentActivity activity) {
        final Uri uri = getUri(activity);
        if (uri == null) {
            return;
        }
        ThemedDialog.buildMaterialDialog(activity)
                .title(R.string.title_warning)
                .content(R.string.message_backup_service_storage_access_framework_disconnect)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive((dialog, which) -> {
                    activity.getContentResolver()
                            .releasePersistableUriPermission(uri, FLAG_URI_READ_WRITE);
                    clearUri(activity);
                    setBackendServiceEnabled(false);
                })
                .show();
    }

    /*package private*/ static Uri getUri(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_FILE, Context.MODE_PRIVATE
        );
        String accessToken = preferences.getString(URI, null);
        if (accessToken == null) {
            return null;
        }
        return Uri.parse(accessToken);
    }

    /*package private*/ static void clearUri(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_FILE, Context.MODE_PRIVATE
        );
        preferences.edit().clear().apply();
    }

    /*package private*/ static void storeUri(Context context, Uri uri) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_FILE, Context.MODE_PRIVATE
        );
        preferences.edit().putString(URI, uri.toString()).apply();
    }

    @Override
    public void registerForActivityResult(Fragment fragment, Activity activity) {
        launcher = fragment.registerForActivityResult(
                new DocumentTreeContract(),
                uri -> {
                    boolean enabled = false;
                    if (uri != null) {
                        activity.getContentResolver().takePersistableUriPermission(
                                uri,
                                FLAG_URI_READ_WRITE
                        );
                        storeUri(activity, uri);
                        enabled = true;
                    }
                    setBackendServiceEnabled(enabled);
                });
    }
}