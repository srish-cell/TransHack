package com.example.transhack;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.example.transhack.R;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ArrayList<HashMap<String, String>> transactionList = new ArrayList<>();
    private ArrayList<String> smsList = new ArrayList<>();
    private ListView listView;
    private static final int READ_SMS_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);
        listView = findViewById(R.id.listView);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView.setAdapter(adapter);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_SMS}, READ_SMS_PERMISSION_CODE);
        } else {
            new ReadSmsTask().execute();
        }
    }

    private class ReadSmsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            readSms();
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            updateListView();
            generateAndUploadCsv();
        }
    }

    private void readSms() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String address = cursor.getString(cursor.getColumnIndexOrThrow("address"));
                String body = cursor.getString(cursor.getColumnIndexOrThrow("body"));

                if (isFinancialTransaction(body)) {
                    HashMap<String, String> transaction = extractTransactionDetails(body);
                    transactionList.add(transaction);
                    smsList.add("Sender: "+address +"\nMessage: " + body);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }
    }

    private boolean isFinancialTransaction(String messageBody) {
        // Implement your logic to identify a financial transaction based on keywords
        return  messageBody.toLowerCase().contains("credited") ||
                messageBody.toLowerCase().contains("withdrawal") ||
                messageBody.toLowerCase().contains("debited") ;
    }

    private HashMap<String, String> extractTransactionDetails(String messageBody) {
        HashMap<String, String> transactionDetails = new HashMap<>();

        // Implement your logic to extract transaction details based on keywords
        // Modify the following lines based on the actual format of your messages
        transactionDetails.put("TransactionDate", getCurrentDateTime(messageBody));
        transactionDetails.put("Amount", extractAmountFromMessage(messageBody));
        transactionDetails.put("Receiver", extractReceiverFromMessage(messageBody));
        transactionDetails.put("TransactionType", extractTransactionTypeFromMessage(messageBody));

        return transactionDetails;
    }

    private String extractAmountFromMessage(String messageBody) {
        Pattern patternAmount = Pattern.compile("\"\\\\bRs.\\\\s*(\\\\d+(\\\\.\\\\d{1,2})?)\\\\b\"");

        Matcher matcher = patternAmount.matcher(messageBody);

        if (matcher.find()) {
            while (matcher.find()) {
                String amountString = matcher.group(1);
                double amount = Double.parseDouble(amountString.replace(",", ""));
                return String.valueOf(amount);// Return the first match as the amount
            }
        }
        return "N/A";
    }


    private String extractReceiverFromMessage(String messageBody) {
        Pattern patternReceiver = Pattern.compile("to (\\w+\\s\\w+)|Payment of \\$(\\d+)");
        Matcher mr= patternReceiver.matcher(messageBody);
        if (mr.find()) {
            return mr.group(); // Return the first match as the amount
        } else {
            return "N/A"; // If no amount is found
        }
    }

    private String extractTransactionTypeFromMessage(String messageBody) {
            // Implement your logic to extract the transaction type from the message
            // Modify this logic based on the patterns in your messages
            // Example: Check for keywords like "debit" or "credit"
            if (messageBody.toLowerCase().contains("debit")) {
                return "Debit";
            } else if (messageBody.toLowerCase().contains("credit")) {
                return "Credit";
            } else {
                return "N/A"; // If no transaction type is found
            }
    }

    private String getCurrentDateTime(String messageBody) {
        Pattern patternDate = Pattern.compile("on (\\d{2}-\\d{2}-\\d{2})");
        Matcher md= patternDate.matcher(messageBody);
        if (md.find()) {
            return String.valueOf(md.group(1)); // Return the first match as the amount
        } else {
            return "N/A"; // If no amount is found
        }
    }

    private void updateListView() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, getTransactionListString());
        listView.setAdapter(adapter);
    }

    private ArrayList<String> getTransactionListString() {
        ArrayList<String> transactionStrings = new ArrayList<>();
        for (String transaction : smsList) {
            transactionStrings.add("Transaction Details: " + transaction);
        }
        return transactionStrings;
    }

    private void generateAndUploadCsv() {
        StringBuilder csvContent = new StringBuilder("TransactionDate,Amount,Receiver,TransactionType\n");
        for (HashMap<String, String> transaction : transactionList) {
            csvContent.append(transaction.get("TransactionDate"))
                    .append(",")
                    .append(transaction.get("Amount"))
                    .append(",")
                    .append(transaction.get("Receiver"))
                    .append(",")
                    .append(transaction.get("TransactionType"))
                    .append("\n");
        }

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference();
        String csvFileName = "financial_transactions.csv";
        StorageReference csvRef = storageRef.child(csvFileName);

        csvRef.putBytes(csvContent.toString().getBytes())
                .addOnSuccessListener(taskSnapshot -> Log.d("UploadCSV", "File successfully uploaded"))
                .addOnFailureListener(exception -> Log.e("UploadCSV", "Error uploading file", exception));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                new ReadSmsTask().execute();
            }
        }
    }
}
