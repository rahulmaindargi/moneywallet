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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.FileList;
import com.rahul.moneywallet.api.AbstractBackendServiceAPI;
import com.rahul.moneywallet.api.BackendException;
import com.rahul.moneywallet.model.GoogleDriveFile;
import com.rahul.moneywallet.model.IFile;
import com.rahul.moneywallet.utils.ProgressInputStream;
import com.rahul.moneywallet.utils.ProgressOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by andrea on 24/11/18.
 */
public class GoogleDriveBackendServiceAPI extends AbstractBackendServiceAPI<GoogleDriveFile> {

    private static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    private final Drive googleDriveService;

    public GoogleDriveBackendServiceAPI(Context context) throws BackendException {
        super(GoogleDriveFile.class);
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(context);
        if (googleAccount != null) {

            Log.d("DriveHandler", "Signed in as " + googleAccount.getEmail());

            // Use the authenticated account to sign in to the Drive service.
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(googleAccount.getAccount());
            googleDriveService = new Drive.Builder(new NetHttpTransport(), new GsonFactory(), credential)
                    .setApplicationName("Money Wallet")
                    .build();

        } else {
            Log.e("DriveHandler", "Unable to sign in.");
            throw new BackendException("GoogleDrive backend cannot be initialized: account is null");
        }

    }

    @Override
    public GoogleDriveFile upload(GoogleDriveFile folder, File file, ProgressInputStream.UploadProgressListener listener) throws BackendException {
        //InputStream inputStream = new ProgressInputStream(file, listener)
        try {
            com.google.api.services.drive.model.File gfile = new com.google.api.services.drive.model.File();
            //gfile.setParents(Collections.singletonList("appDataFolder"));
            gfile.setParents(Collections.singletonList(folder.getDriveId()));
            gfile.setName(file.getName());
            Log.d("GOOGLEUPLOAD", "Upload Parent " + folder);
            FileContent fileContent = new FileContent("application/octet-stream", file);
            com.google.api.services.drive.model.File uploadedFile = googleDriveService.files().create(gfile, fileContent).setFields("id,size,name,capabilities").execute();
            listener.onUploadProgressUpdate(100);
            return new GoogleDriveFile(uploadedFile);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("GOOGLEUPLOAD", "Upload Parent Failed" + folder, e);
            throw new BackendException(e.getMessage(), isRecoverable(e));
        }
    }

    @Override
    public File download(File folder, @NonNull GoogleDriveFile file, ProgressOutputStream.DownloadProgressListener listener) throws BackendException {
        File destination = new File(folder, file.getName());
        try (OutputStream outputStream = new ProgressOutputStream(destination, file.getSize(), listener)) {
            Log.d("GOOGLEUPLOAD", "Download " + file.getDriveId());
            Log.d("GOOGLEUPLOAD", "Download " + file);
            googleDriveService.files().get(file.getDriveId()).executeMediaAndDownloadTo(outputStream);
        } catch (Exception e) {
            Log.e("GOOGLEUPLOAD", "Download Failed" + folder, e);
            throw new BackendException(e.getMessage(), isRecoverable(e));
        }
        //destination = new File(folder, file.getName());
        return destination;
    }

    @Override
    public List<IFile> list(GoogleDriveFile folder) throws BackendException {

        try {
            Drive.Files.List folderList = googleDriveService.files().list();
            // folderList.setSpaces("appDataFolder");
            if (folder != null) {
                folderList.setQ("parents in '" + folder.getDriveId() + "'");
            }
            FileList fileList = folderList.setFields("files(id, name, size, capabilities, isAppAuthorized)").execute();
            List<IFile> returnList = fileListToIfileList(fileList);
            if (returnList.isEmpty() && folder == null) {
                Log.d("GOOGLEUPLOAD", "Folder is null and no accessing files so creating app folder");
                newFolder(null, "Money Wallet");
                folderList = googleDriveService.files().list();
                //folderList.setQ("parents in '"+appFolderId+"'");
                fileList = folderList.setFields("files(id, name, size, capabilities, isAppAuthorized)").execute();
                returnList = fileListToIfileList(fileList);
            }
            return returnList;

        } catch (Exception e) {
            Log.e("GOOGLEUPLOAD", "List Failed" + folder, e);
            throw new BackendException(e.getMessage(), isRecoverable(e));
        }
    }

    @NonNull
    private List<IFile> fileListToIfileList(FileList fileList) {
        return fileList.getFiles().parallelStream().filter(com.google.api.services.drive.model.File::getIsAppAuthorized).map(GoogleDriveFile::new).collect(Collectors.toList());
    }

    @Override
    protected GoogleDriveFile newFolder(GoogleDriveFile parent, String name) throws BackendException {
        try {
            com.google.api.services.drive.model.File gfile = new com.google.api.services.drive.model.File();
            //gfile.setParents(Collections.singletonList("appDataFolder"));
            if (parent != null) {
                gfile.setParents(Collections.singletonList(parent.getDriveId()));
            }
            gfile.setName(name);
            gfile.setMimeType(MIME_TYPE_FOLDER);
            Log.d("GOOGLEUPLOAD", "Create folder Parent " + parent);
            com.google.api.services.drive.model.File uploadedFile = googleDriveService.files().create(gfile)
                    .setFields("id, size, name, capabilities")
                    .execute();
            Log.d("GOOGLEUPLOAD", "Create folder Parent " + uploadedFile.getName() + " " + uploadedFile.getId());
            return new GoogleDriveFile(uploadedFile);
        } catch (Exception e) {
            Log.e("GOOGLEUPLOAD", "Folder create failed " + e.getMessage(), e);
            throw new BackendException(e.getMessage(), isRecoverable(e));
        }
    }


    private boolean isRecoverable(Exception e) {
        return e instanceof IOException;
    }
}