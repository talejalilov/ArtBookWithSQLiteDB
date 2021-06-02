package com.example.artbookwithsqlitedbsample;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PatternMatcher;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.View;
import android.view.contentcapture.DataShareWriteAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class SecondActivity extends AppCompatActivity {

    ImageView imageView;
    EditText artName, artistName;
    Button button;

    Bitmap selectedImage;

    SQLiteDatabase database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);

        imageView = findViewById(R.id.imageView);
        artName = findViewById(R.id.artName);
        artistName = findViewById(R.id.artistName);
        button = findViewById(R.id.save);

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(SecondActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(SecondActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);

                } else {
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(intent, 2);
                }
            }
        });

        Intent intent = getIntent();

        String info = intent.getStringExtra("info");
        if (info.matches("new")) {

            artName.setText("");
            artistName.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap selectedImage = BitmapFactory.decodeResource(getApplication().getResources(), R.drawable.gg);
            imageView.setImageBitmap(selectedImage);


        } else {

            int artId = intent.getIntExtra("artId",1);
            button.setVisibility(View.INVISIBLE);

            try{

                Cursor cursor = database.rawQuery("SELECT * FROM art WHERE id = ? ",new String[] {String.valueOf(artId)});

                int artIx = cursor.getColumnIndex("artname");
                int artistNameIx = cursor.getColumnIndex("artistname");
                int imageIx= cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    artName.setText(cursor.getString(artIx));
                    artistName.setText(cursor.getString(artistNameIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }
                cursor.close();

            }catch (Exception e) {
                e.printStackTrace();
            }


        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, 2);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            Uri getImage = data.getData();
            try {
                if (Build.VERSION.SDK_INT >= 28) {

                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), getImage);
                    selectedImage = ImageDecoder.decodeBitmap(source);

                } else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), getImage);
                }
                imageView.setImageBitmap(selectedImage);

            } catch (IOException e) {
                e.printStackTrace();

            }

        }
    }

    public Bitmap makeSmallerImage(Bitmap image, int maxSize) {

        int width = image.getWidth();
        int height = image.getHeight();

        float BitmapRatio = (float) width / (float) height;

        if (BitmapRatio > 1) {
            width = maxSize;
            height = (int) (width / BitmapRatio);

        } else {
            height = maxSize;
            width = (int) (BitmapRatio * height);

        }

        return Bitmap.createScaledBitmap(image, width, height, true);
    }

    public void save(View v) {

        String artName1 = artName.getText().toString();
        String artistName1 = artistName.getText().toString();


        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Bitmap smallImage = makeSmallerImage(selectedImage, 300);
        smallImage.compress(Bitmap.CompressFormat.PNG, 50, outputStream);
        byte[] bytes = outputStream.toByteArray();

        try {

             database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);
            database.execSQL("CREATE TABLE IF NOT EXISTS art (id INTEGER PRIMARY KEY, artname VARCHAR, artistname VARCHAR, image BLOB)");

            String sql = "INSERT INTO art (artname,artistname,image) VALUES (?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sql);
            sqLiteStatement.bindString(1, artName1);
            sqLiteStatement.bindString(2, artistName1);
            sqLiteStatement.bindBlob(3, bytes);
            sqLiteStatement.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(SecondActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

     //  finish();

    }
}
