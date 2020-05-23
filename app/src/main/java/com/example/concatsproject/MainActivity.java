package com.example.concatsproject;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private String TAG="PHONES1";
    private int PERMISSIONS_REQUEST_READ_CONTACTS=101;
    private int PERMISSIONS_REQUEST_WRITE_CONTACTS=102;

    List<Contact> list = new ArrayList<Contact>();
    Button button;
    RecyclerView recyclerView;
    RecyclerViewAdapter adapter;
    TextView nameView;
    TextView phoneView;

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

        nameView = (TextView) findViewById(R.id.nameView);
        phoneView = (TextView) findViewById(R.id.phoneView);

        Button addButton0 = (Button) findViewById(R.id.addButton0);
        addButton0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askForWritePermission();
            }
        });
        notificationManager = NotificationManagerCompat.from(this);
        button = (Button) findViewById(R.id.button2);
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
                askForReadPermission();
            }
        });

        adapter.setOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                // При клике вызываем уведомление
                NotificationManagerCompat notificationManager =
                        NotificationManagerCompat.from(MainActivity.this);
                // В android Oreo появились каналы. Если версия >= Oreo и нет канала, то создаем
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && notificationManager.getNotificationChannel(CHANNEL_ID)==null){
                    notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT));
                }
                // Создаем builder. Можно было бы и через кучу точек.
                NotificationCompat.Builder b = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);
                b.setAutoCancel(true);
                b.setContentTitle("Вы жмякнули на контакт");
                b.setContentText(list.get(position).getName() + " " + list.get(position).getPhone());
                b.setSmallIcon(android.R.drawable.sym_def_app_icon);
                // Создаем Intent для открытия новой активности
                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                // Оборачиваем в PandingIntent, чтобы это могло выполняться вне активности,
                // собственно при клике на уведомление
                PendingIntent pendingIntent = PendingIntent.getActivity(MainActivity.this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
                // Добавляем intent в builder
                b.setContentIntent(pendingIntent);
                // Билдим, создаем уведомление
                notificationManager.notify(position, b.build());
            }
        });
        askForReadPermission();
    }

    public void askForReadPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            readContacts();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission
                                    .READ_CONTACTS},
                    PERMISSIONS_REQUEST_READ_CONTACTS);
        }
    }
    public void askForWritePermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_CONTACTS) ==
                PackageManager.PERMISSION_GRANTED) {
            String name = nameView.getText().toString();
            String phone = phoneView.getText().toString();
            addContatct(name, phone);
            nameView.setText("");
            phoneView.setText("");

        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission
                                    .WRITE_CONTACTS},
                    PERMISSIONS_REQUEST_WRITE_CONTACTS);
        }



    }



    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions,
                                           int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_READ_CONTACTS) {
            // При отмене запроса массив результатов пустой
            if ((grantResults.length > 0) &&
                    (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                readContacts();
            }
            return;
        }

        if (requestCode == PERMISSIONS_REQUEST_WRITE_CONTACTS){
            if ((grantResults.length > 0) &&
                    (grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED)) {
                addContatct(nameView.getText().toString(), phoneView.getText().toString());
            }
            return;
        }
    }



    public void readContacts() {
        // Удаляем старые контакты, загружаем новые
        list.clear();
        adapter.notifyDataSetChanged();
        Contact contact;
        // contentResolver - добрый волшебник, который поможет нам связаться с content provider,
        // т.e. с ContactsContract
        // Получаем uri всех контактов
        Cursor cursor = getContentResolver().query(
                ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        // Бежим по uri
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {

                // записываем в список класс Contact, хранящий id, имя, номер
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
                    // В теории может быть несколько контактов. Если так, то они будут записаны через \n
                    // Лучше хранить список, но мне очень-очень лень. Все равно Contact используется только для визуализации
                    Cursor pCur;
                    pCur = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id},
                            null);
                    int k = 0;
                    String phones = "";
                    while (pCur.moveToNext()) {
                        k++;
                        phones += pCur.getString(
                                pCur.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER)) + "\n";
                    }
                    contact.setPhone(phones);
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

    public void addContatct(String name, String phone){
        if (name.equals("")) return;
        // Добавляем новый контакт, получаем его Uri
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, new ContentValues());
        // Получаем id контакта
        long rawContactId =  ContentUris.parseId(rawContactUri);
        // Internet Media Types, также MIME-типы — типы данных,
        // которые могут быть переданы посредством сети Интернет с применением стандарта MIME.
        // Базовые типы данных MIME:
        //     application;
        //    audio;
        //    example;
        //    image;
        //    message;
        //    model;
        //    multipart;
        //    text;
        //    video.


        list.add(new Contact(name, phone));
        adapter.notifyItemInserted(list.size());


        // Создаем новую строчку таблицы
        // Записываем данные, которые потом передадим в провайдер
        ContentValues values = new ContentValues();
        // Добавляем id контакта
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        // Добавляем тип данных
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        // Добавляем имя
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);

        // contentResolver - добрый волшебник, который поможет нам связаться с content provider,
        // т.e. с ContactsContract
        // Мы хотим добавить наши сохраненные в values данные. Собственно, добавляе
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);
        values.clear();

        // Все то же самое для номера
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone);
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);


    }


}
