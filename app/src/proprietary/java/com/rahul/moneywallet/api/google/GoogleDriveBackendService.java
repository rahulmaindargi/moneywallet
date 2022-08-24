/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.rahul.moneywallet.api.google;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;
import com.rahul.moneywallet.R;
import com.rahul.moneywallet.api.AbstractBackendServiceDelegate;
import com.rahul.moneywallet.api.BackendException;
import com.rahul.moneywallet.api.BackendServiceFactory;
import com.rahul.moneywallet.ui.view.theme.ThemedDialog;

/**
 * Created by andrea on 21/11/18.
 */
public class GoogleDriveBackendService extends AbstractBackendServiceDelegate {

    private final GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> launcher;

    public GoogleDriveBackendService(Context context, BackendServiceStatusListener listener) {
        super(listener);
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER), new Scope(Scopes.DRIVE_FILE))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, signInOptions);
    }

    @Override
    public String getId() {
        return BackendServiceFactory.SERVICE_ID_GOOGLE_DRIVE;
    }

    @Override
    public int getName() {
        return R.string.service_backup_google_drive;
    }

    @Override
    public int getBackupCoverMessage() {
        return R.string.cover_message_backup_google_drive_title;
    }

    @Override
    public int getBackupCoverAction() {
        return R.string.cover_message_backup_google_drive_button;
    }

    @Override
    public boolean isServiceEnabled(Context context) {

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(context);
        return signInAccount != null && GoogleSignIn.hasPermissions(signInAccount, new Scope(Scopes.DRIVE_APPFOLDER), new Scope(Scopes.DRIVE_FILE));
    }

    @Override
    public void setup(ComponentActivity activity) throws BackendException {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        launcher.launch(signInIntent);
    }

    @Override
    public void teardown(final ComponentActivity activity) {
        ThemedDialog.buildMaterialDialog(activity)
                .title(R.string.title_warning)
                .content(R.string.message_backup_service_google_drive_disconnect)
                .positiveText(android.R.string.yes)
                .negativeText(android.R.string.no)
                .onPositive((dialog, which) -> signOutFromGoogle(activity))
                .show();
    }

    private void signOutFromGoogle(Activity activity) {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER), new Scope(Scopes.DRIVE_FILE))
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(activity, signInOptions);
        Task<Void> signOutTask = googleSignInClient.signOut();
        signOutTask.addOnCompleteListener(task -> setBackendServiceEnabled(false));
    }


    @Override
    public void registerForActivityResult(Fragment fragment, Activity activity) {
        launcher = fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    try {

                        Task<GoogleSignInAccount> getAccountTask = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        setBackendServiceEnabled(getAccountTask.isSuccessful());

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("GoogleAuth", " Sign in failed " + e.getMessage(), e);
                        setBackendServiceEnabled(false);
                    }

                }
        );
    }
}