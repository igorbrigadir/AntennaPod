package de.danoeh.antennapod.activity;

import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.core.preferences.UserPreferences;
import de.danoeh.antennapod.core.storage.PodDBAdapter;

/**
 * Displays the 'import/export' screen
 */
public class ImportExportActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_RESTORE = 43;
    private static final int REQUEST_CODE_BACKUP_DOCUMENT = 44;
    private static final String EXPORT_FILENAME = "AntennaPodBackup.db";
    private static final String TAG = ImportExportActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(UserPreferences.getTheme());
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
        }
        setContentView(R.layout.import_export_activity);

        findViewById(R.id.button_export).setOnClickListener(view -> backup());
        findViewById(R.id.button_import).setOnClickListener(view -> restore());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    private void backup() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/x-sqlite3")
                    .putExtra(Intent.EXTRA_TITLE, EXPORT_FILENAME);

            startActivityForResult(intent, REQUEST_CODE_BACKUP_DOCUMENT);
        } else {
            try {
                File sd = Environment.getExternalStorageDirectory();
                File backupDB = new File(sd, EXPORT_FILENAME);
                writeBackupTo(new FileOutputStream(backupDB));
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
                Snackbar.make(findViewById(R.id.import_export_layout), e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    private void restore() {
        if (Build.VERSION.SDK_INT >= 19) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_RESTORE);
        } else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent,
                    getString(R.string.import_select_file)), REQUEST_CODE_RESTORE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (resultCode != RESULT_OK || resultData == null) {
            return;
        }
        Uri uri = resultData.getData();

        if (requestCode == REQUEST_CODE_RESTORE) {
            restoreFrom(uri);
        } else if (requestCode == REQUEST_CODE_BACKUP_DOCUMENT) {
            backupToDocument(uri);
        }
    }

    private void restoreFrom(Uri inputUri) {
        InputStream inputStream = null;
        try {
            if (!validateDB(inputUri)) {
                displayBadFileDialog();
                return;
            }

            File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);
            inputStream = getContentResolver().openInputStream(inputUri);
            FileUtils.copyInputStreamToFile(inputStream, currentDB);
            displayImportSuccessDialog();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Snackbar.make(findViewById(R.id.import_export_layout), e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private static final byte[] SQLITE3_MAGIC = "SQLite format 3\0".getBytes();
    private boolean validateDB(Uri inputUri) throws IOException {
        try (InputStream inputStream = getContentResolver().openInputStream(inputUri)) {
            byte[] magicBuf = new byte[SQLITE3_MAGIC.length];
            if (inputStream.read(magicBuf) == magicBuf.length) {
                return Arrays.equals(SQLITE3_MAGIC, magicBuf);
            }
        }

        return false;
    }

    private void displayBadFileDialog() {
        AlertDialog.Builder d = new AlertDialog.Builder(ImportExportActivity.this);
        d.setMessage(R.string.import_bad_file)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, ((dialogInterface, i) -> {
                    // do nothing
                }))
                .show();
    }

    private void displayImportSuccessDialog() {
        AlertDialog.Builder d = new AlertDialog.Builder(ImportExportActivity.this);
        d.setMessage(R.string.import_ok);
        d.setCancelable(false);
        d.setPositiveButton(android.R.string.ok, (dialogInterface, i) -> {
            Intent intent = new Intent(getApplicationContext(), SplashActivity.class);
            ComponentName cn = intent.getComponent();
            Intent mainIntent = Intent.makeRestartActivityTask(cn);
            startActivity(mainIntent);
        });
        d.show();
    }

    private void backupToDocument(Uri uri) {
        ParcelFileDescriptor pfd = null;
        FileOutputStream fileOutputStream = null;
        try {
            pfd = getContentResolver().openFileDescriptor(uri, "w");
            fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
            writeBackupTo(fileOutputStream);

            Snackbar.make(findViewById(R.id.import_export_layout),
                    R.string.export_ok, Snackbar.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Snackbar.make(findViewById(R.id.import_export_layout), e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
        } finally {
            IOUtils.closeQuietly(fileOutputStream);

            if (pfd != null) {
                try {
                    pfd.close();
                } catch (IOException e) {
                    Log.d(TAG, "Unable to close ParcelFileDescriptor");
                }
            }
        }
    }

    private void writeBackupTo(FileOutputStream outFileStream) {
        FileChannel src = null;
        FileChannel dst = null;
        try {
            File currentDB = getDatabasePath(PodDBAdapter.DATABASE_NAME);

            if (currentDB.exists()) {
                src = new FileInputStream(currentDB).getChannel();
                dst = outFileStream.getChannel();
                dst.transferFrom(src, 0, src.size());

                Snackbar.make(findViewById(R.id.import_export_layout),
                        R.string.export_ok, Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(R.id.import_export_layout),
                        "Can not access current database", Snackbar.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            Snackbar.make(findViewById(R.id.import_export_layout), e.getLocalizedMessage(), Snackbar.LENGTH_SHORT).show();
        } finally {
            IOUtils.closeQuietly(src);
            IOUtils.closeQuietly(dst);
        }
    }
}
