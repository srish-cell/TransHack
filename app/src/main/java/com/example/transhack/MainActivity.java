package com.example.transhack;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.ui.AppBarConfiguration;
import android.provider.Telephony;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.example.transhack.R;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {


    private ArrayList<String> smsList = new ArrayList<>();
    private ListView listView;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, smsList);
        listView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            readSms();
        }
    }


    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY));

                // Check if the SMS indicates a financial transaction
                if (isFinancialTransaction(body)) {
                    smsList.add("Sender: " + address + "\nMessage: " + body);
                }
            } while (cursor.moveToNext());

            // Notify the adapter that the data has changed
            ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
            adapter.notifyDataSetChanged();
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    private boolean isFinancialTransaction(String messageBody) {
        // Convert the message body to lowercase for case-insensitive matching
        String lowercaseBody = messageBody.toLowerCase();

        // Check for keywords associated with financial transactions
        return lowercaseBody.contains("transaction") ||
                lowercaseBody.contains("payment") ||
                lowercaseBody.contains("purchase") ||
                lowercaseBody.contains("withdrawal") ||
                lowercaseBody.contains("deposit") ||
                lowercaseBody.contains("transfer");
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readSms();
                ArrayAdapter<String> adapter = (ArrayAdapter<String>) listView.getAdapter();
                adapter.notifyDataSetChanged();
            }
        }
    }
}