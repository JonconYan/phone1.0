package com.example.phone_test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    private static final String TAG = "";
    private Button btnthjl;
    private Button btnlxr;
    private Button btnAdd;
    private ListView lvPhones;
    private TextView tvPhoneName;
    private TextView tvPhoneNumber;
    private EditText edtPhone;

    private List<Map<String, Object>> ContactsList;  //存储所有通讯录信息
    private List<Map<String,Object>> Calllog;

    //获取系统自定义字符串
//    @Override
//    public Resources getResources() {
//        return super.getResources();
//    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        checkReadPermission(Manifest.permission.READ_CONTACTS, 1);
        checkReadPermission(Manifest.permission.CALL_PHONE, 2);
        checkReadPermission(Manifest.permission.WRITE_CONTACTS, 3);
        checkReadPermission(Manifest.permission.SEND_SMS, 4);
        checkReadPermission(Manifest.permission.READ_SMS, 5);
        btnAdd = (Button) findViewById(R.id.btnAdd);      //添加
        lvPhones = (ListView) findViewById(R.id.lvPhones);//显示
        btnlxr = (Button)findViewById(R.id.tongxunlu);//通讯录
        btnthjl=(Button)findViewById(R.id.tonghuajilu);//通话记录
        InitData();

        //添加联系人
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getResources().getString(R.string.addContact));
                //    通过LayoutInflater来加载一个xml的布局文件作为一个View对象
                View view1 = LayoutInflater.from(MainActivity.this).inflate(R.layout.contact_add, null);

                builder.setView(view1);

                final EditText edtName = (EditText) view1.findViewById(R.id.edtName);
                final EditText edtPhone = (EditText) view1.findViewById(R.id.edtPhone);

                //确定操作
                builder.setPositiveButton(getResources().getString(R.string.btnOK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String strName = edtName.getText().toString().trim();
                        String strPhone = edtPhone.getText().toString().trim();
                        if (strPhone.isEmpty()) {
                            Toast.makeText(getApplicationContext(), "电话号码为空，添加失败!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String telRegex = "((\\d{11})|^((\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})|(\\d{4}|\\d{3})-(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1})|(\\d{7,8})-(\\d{4}|\\d{3}|\\d{2}|\\d{1}))$)";  //
                        if (!edtPhone.getText().toString().matches(telRegex)) {
                            Toast.makeText(getApplicationContext(), "请重新输入正确的电话号码，添加失败!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        writeContacts(strName, strPhone);    //添加联系人
                        InitData();
                    }
                });

                //取消操作
                builder.setNegativeButton(getResources().getString(R.string.btnCancel), null);

                builder.show();

            }
        });

        btnthjl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.calllog);
                //取消操作
                btnlxr=(Button)findViewById(R.id.tongxunlu1);
                List<Map<String,Object>> list = getCalllog();
                String from[]={"name","number","data","duration","type"};
                int[] to = { R.id.tv_name, R.id.tv_number, R.id.tv_date,
                        R.id.tv_duration,R.id.tv_type };

                SimpleAdapter adapter = new SimpleAdapter(MainActivity.this, list,
                        R.layout.simple_calllog, from, to);
                ListView calllog=(ListView)findViewById(R.id.calllog);
                calllog.setAdapter(adapter);
                btnlxr.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setContentView(R.layout.activity_main);
                        btnthjl=(Button)findViewById(R.id.tonghuajilu);
                        InitData();
                    }
                });
 //               setContentView(R.layout.calllog);

//                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
//                builder.setTitle(getResources().getString(R.string.tonghuajilu));
//                View view2 = LayoutInflater.from(MainActivity.this).inflate(R.layout.calllog, null);
//                builder.setView(view2);
//
//                builder.setNegativeButton(getResources().getString(R.string.btnCancel), null);
//
//                builder.show();
            }


        });
        btnlxr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setContentView(R.layout.activity_main);
                InitData();
            }
        });
        //为ListView的列表项选中事件绑定事件监听器
        lvPhones.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //取得电话号码
                final String phoneNumber = ContactsList.get(i).get("phoneNumber").toString();

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(getResources().getString(R.string.qxz));
                final String[] contactFun = new String[]{getResources().getString(R.string.callPhone), getResources().getString(R.string.sendMessage)};
                builder.setItems(contactFun, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String strFun = contactFun[i];
                        if (strFun.equals(getResources().getString(R.string.callPhone))) {
                            Intent phoneIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNumber));
                            startActivity(phoneIntent);
                        } else {
                            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phoneNumber + ""));
                            intent.putExtra("sms_body", "");
                            startActivity(intent);
                        }
                    }
                });
                //取消操作
                builder.setNegativeButton(getResources().getString(R.string.btnCancel), null);
                builder.show();
            }
        });

    }


    //写入通讯录
    public void writeContacts(String strName, String strPhone) {

        ContentResolver resolver = getContentResolver();
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        Uri dataUri = Uri.parse("content://com.android.contacts/data");


        //查出最后一个ID
        Cursor cursor = resolver.query(uri, new String[]{"_id"}, null, null, null);
        cursor.moveToLast();
        int lastId = cursor.getInt(0);
        int newId = lastId + 1;

        //插入一个联系人id
        ContentValues values = new ContentValues();
        values.put("contact_id", newId);
        resolver.insert(uri, values);

        //插入电话数据
        values.clear();
        values.put("raw_contact_id", newId);
        values.put("mimetype", ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, strPhone);
        resolver.insert(dataUri, values);

        //插入姓名数据
        values.clear();
        values.put("raw_contact_id", newId);
        values.put("mimetype", ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, strName);
        resolver.insert(dataUri, values);
    }

    //获取通讯录
    public List<Map<String, Object>> getContacts() {
        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        Cursor cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()) {
            String phoneName;
            String phoneNumber;
            Map<String, Object> listItem = new HashMap<String, Object>();
            phoneName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
            phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
            listItem.put("phoneName", phoneName);
            listItem.put("phoneNumber", phoneNumber);
            listItems.add(listItem);
        }
        return listItems;
    }

    //获取通话记录
    public List<Map<String, Object>> getCalllog() {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DEFAULT_SORT_ORDER);

        List<Map<String, Object>> listItems = new ArrayList<Map<String, Object>>();
        while(cursor.moveToNext())
        {
            String name = cursor.getString(cursor.getColumnIndex(CallLog.Calls.CACHED_NAME));
            String number = cursor.getString(cursor.getColumnIndex(CallLog.Calls.NUMBER));
            long dateLong = cursor.getLong(cursor.getColumnIndex(CallLog.Calls.DATE));
            String date = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss").format(new Date(dateLong));
            int duration = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.DURATION));
            int type = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE));
            String typeString = "";
            switch (type) {
                case CallLog.Calls.INCOMING_TYPE:
                    typeString = "打入";
                    break;
                case CallLog.Calls.OUTGOING_TYPE:
                    typeString = "打出";
                    break;
                case CallLog.Calls.MISSED_TYPE:
                    typeString = "未接";
                    break;
                default:
                    break;
            }
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("name", (name == null) ? "未备注联系人" : name);
            map.put("number", number);
            map.put("date", date);
            map.put("duration", (duration / 60) + "分钟");
            map.put("type", typeString);
            listItems.add(map);

        }
        return listItems;


    }

    //初始化ListView数据
    public void InitData() {
        try {


            List<Map<String, Object>> contacts = getContacts();  //获取通讯录
            ContactsList = contacts;
            SimpleAdapter adapterPhones = new SimpleAdapter(this, contacts,
                    R.layout.simple_item,
                    new String[]{"phoneName", "phoneNumber"},
                    new int[]{R.id.tvPhoneName, R.id.tvPhoneNumber});

            ListView lvPhones = (ListView) findViewById(R.id.lvPhones);
            lvPhones.setAdapter(adapterPhones);
        }
        catch (ArrayIndexOutOfBoundsException ex)
        {
            Log.d(TAG, "InitData: error",ex);
        }
    }
//    public boolean getPermission(){
//        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_CONTACTS}, 1001);
//            return false;
//        }
//        return true;
//    }
    public boolean checkReadPermission(String string_permission,int request_code) {
        boolean flag = false;
        if (ContextCompat.checkSelfPermission(MainActivity.this, string_permission) == PackageManager.PERMISSION_GRANTED) {//已有权限
            flag = true;
        } else {//申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{string_permission}, request_code);
        }
        return flag;
    }


}
