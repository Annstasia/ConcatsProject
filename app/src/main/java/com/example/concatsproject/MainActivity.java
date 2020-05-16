package com.example.concatsproject;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import static com.example.concatsproject.NotificationHelper.CHANNEL_1_ID;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG="PHONES1";
    private int PERMISSIONS_REQUEST_READ_CONTACTS=101;
    List<Contact> list = new ArrayList<Contact>();
    Button button;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
//    private static final int NOTIFY_ID = 666;
    private static final int NOTIFY_PENDING = 666;


    // Идентификатор канала
    private static String CHANNEL_ID = "Phone channel";
    private static String CHANNEL_NAME = "Phone";

    NotificationManagerCompat notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        notificationManager = NotificationManagerCompat.from(this);
        button = (Button) findViewById(R.id.button);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        adapter = new RecyclerViewAdapter(list);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(itemAnimator);




        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForPermission();
            }
        });

        adapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(MainActivity.this);

//                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(CHANNEL_ID)==null){
                    notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
                }
                NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
                b.setAutoCancel(true);
                b.setContentTitle("Вы жмякнули на контакт");
                b.setContentText(list.get(position).getName() + " " + list.get(position).getPhone());
                b.setSmallIcon(android.R.drawable.sym_def_app_icon);
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                b.setContentIntent(pendingIntent);
                notificationManager.notify(position, b.build());

            }
        });


        askForPermission();
    }

    public void askForPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            readContacts(this);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission
                                    .READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            // При отмене запроса массив результатов пустой
            if ((grantResults.length > 0) &&
                    (grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED)) {
                // Разрешения получены
                Log.d(TAG, "Permission was granted!");

                // Чтение контактов
                readContacts(this);
            } else {
                // Разрешения НЕ получены.
                Log.d(TAG, "Permission denied!");
            }
            return;
        }
    }



    public void readContacts(Context context) {
        list.clear();
        adapter.notifyDataSetChanged();
        Contact contact;
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                contact = new Contact();
                String id = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts._ID));
                contact.setId(id);

                String name = cursor.getString(
                        cursor.getColumnIndex(
                                ContactsContract.Contacts.DISPLAY_NAME));
                contact.setName(name);

                String has_phone = cursor.getString(
                        cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                if (Integer.parseInt(has_phone) > 0) {
                    // extract phone number
                    Cursor pCur;
                    pCur = context.getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    int k = 0;
                    while (pCur.moveToNext()) {
                        k++;
                        String phone = pCur.getString(
                                pCur.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                        Log.d("Contact2", ""+k);
                        contact.setPhone(phone);
                    }
                    k = 0;
                    pCur.close();
                }
                list.add(contact);
                Log.d(TAG, "Contact id=" + contact.getId() +
                        ", name=" + contact.getName() +
                        ", phone=" + contact.getPhone());
            }
            adapter.notifyDataSetChanged();
        }

    }


}
