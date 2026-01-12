package com.example.birthdayapp;
import android.Manifest;
import android.app.PendingIntent;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds;
import android.telephony.SmsManager;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.work.*;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        Button btnAdd = new Button(this);
        btnAdd.setText("1. Додати контакти (Грудень)");

        Button btnScan = new Button(this);
        btnScan.setText("2. Знайти іменинників");

        ListView listView = new ListView(this);

        layout.addView(btnAdd);
        layout.addView(btnScan);
        layout.addView(listView);
        setContentView(layout);

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.SEND_SMS
        }, 1);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(context, "SMS успішно відправлено!", Toast.LENGTH_SHORT).show();
            }
        }, new IntentFilter("SMS_SENT"));

        btnAdd.setOnClickListener(v -> {
            addContact("Oleg Dec", "555111", "1990-12-05");
            addContact("Anna Xmas", "555222", "1995-12-25");
            Toast.makeText(this, "Контакти додано", Toast.LENGTH_SHORT).show();

            WorkManager.getInstance(this).enqueue(new OneTimeWorkRequest.Builder(BirthdayWorker.class).build());
        });

        btnScan.setOnClickListener(v -> {
            ArrayList<String> list = new ArrayList<>();
            Cursor cur = getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                    new String[]{CommonDataKinds.Event.START_DATE, "display_name"},
                    ContactsContract.Data.MIMETYPE + "=? AND " + CommonDataKinds.Event.TYPE + "=?",
                    new String[]{CommonDataKinds.Event.CONTENT_ITEM_TYPE, String.valueOf(CommonDataKinds.Event.TYPE_BIRTHDAY)}, null);

            if (cur != null) {
                while (cur.moveToNext()) {
                    String date = cur.getString(0);
                    String name = cur.getString(1);
                    if (date != null && (date.contains("-12-") || date.startsWith("12-"))) {
                        list.add(name + "\n" + date);
                    }
                }
                cur.close();
            }
            listView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list));
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE);
            SmsManager.getDefault().sendTextMessage("5555", null, "Happy Birthday!", pi, null);
        });
    }

    void addContact(String name, String phone, String date) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.StructuredName.DISPLAY_NAME, name).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Phone.NUMBER, phone).build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE, CommonDataKinds.Event.CONTENT_ITEM_TYPE)
                .withValue(CommonDataKinds.Event.START_DATE, date)
                .withValue(CommonDataKinds.Event.TYPE, CommonDataKinds.Event.TYPE_BIRTHDAY).build());
        try { getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops); } catch (Exception e) {}
    }
}