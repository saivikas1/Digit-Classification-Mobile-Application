package com.example.projcamera;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Activity2 extends AppCompatActivity {

    ArrayList<Bitmap> chunkedImages = new ArrayList<Bitmap>(4);
    String[] result = {"", "", "", ""};
    Bitmap image;
    byte[] imageArray;
    byte[] imageArray1;
    byte[] imageArray2;
    byte[] imageArray3;
    byte[] imageArray4;
    int digit = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();

        ImageView Imagedisplay = (ImageView) findViewById(R.id.dpImage);
//        image = (Bitmap) extras.get("image");
        Bitmap image = (Bitmap) extras.get("image");
        Imagedisplay.setImageBitmap(image);

        Button button2 = findViewById(R.id.btn2);

        ImageView Imagedisplay1 = Imagedisplay;
        button2.setOnClickListener(new  View.OnClickListener(){
            @RequiresApi(api = Build.VERSION_CODES.N)
            public void onClick(View view){

//                splitImage(Imagedisplay1, chunkedImages);
                String fullURL1 = "http://" + "172.20.10.10" + ":" + 5000 + "/";
                String fullURL2 = "http://" + "172.20.10.3" + ":" + 8000 + "/";
                String fullURL3 = "http://" + "172.20.10.8" + ":" + 5000 + "/";
                String fullURL4 = "http://" + "172.20.10.9" + ":" + 5000 + "/";

                splitImage(Imagedisplay1, chunkedImages);

                sendRequest(fullURL1, chunkedImages.get(0), 0);
                sendRequest(fullURL2, chunkedImages.get(1), 1);
                sendRequest(fullURL3, chunkedImages.get(2), 2);
                sendRequest(fullURL4, chunkedImages.get(3), 3);


                try {
                    TimeUnit.SECONDS.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (result[0].length()!=0 || result[1].length()!=0 || result[2].length()!=0 || result[3].length()!=0) {
                    Map<Integer, Double> map = new HashMap<>();
                    for (int i = 0; i < result.length; i++) {
                        if (result[i].length()!=0) {
                            String[] res = result[i].split(" ", 2);
                            int predictedDigit = Integer.parseInt(res[0]);
                            double prob = Double.parseDouble(res[1]);
                            if (!map.containsKey(20)) {
                                map.put(predictedDigit, prob);
                            } else {
                                if (map.get(predictedDigit) < prob) {
                                    map.put(predictedDigit, prob);
                                }
                            }
                        }
                    }

                    digit = Collections.max(map.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey();
                    saveImageToGallery(image);
                }
                finish();
            }
        });
    }

    private void splitImage(ImageView Imagedisplay1, ArrayList<Bitmap> chunkedImages) {
        int rows, cols, blockHeight, blockWidth;
        BitmapDrawable drawable = (BitmapDrawable) Imagedisplay1.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), true);
        rows = cols = 2;
        blockHeight = bitmap.getHeight() / rows;
        blockWidth = bitmap.getWidth() / cols;

        int yCoord = 0;
        for(int x = 0; x < rows; x++) {
            int xCoord = 0;
            for(int y = 0; y < cols; y++) {
                chunkedImages.add(Bitmap.createBitmap(scaledBitmap, xCoord, yCoord, blockWidth, blockHeight));
                xCoord += blockWidth;
            }
            yCoord += blockHeight;
        }
    }


    public void sendRequest(String url, Bitmap bitmap, int i) {
//        Spinner category = findViewById(R.id.category);
//        String cat = category.getSelectedItem().toString();
//        String fullURL = "http://" + "172.20.10.3" + ":" + 8000 + "/";
        OkHttpClient client = new OkHttpClient();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        multipartBodyBuilder.addFormDataPart("image","image"+".jpeg", RequestBody.create(MediaType.parse("image/*jpeg"), byteArray));
        RequestBody postBodyImage = multipartBodyBuilder.build();
        Request request = new Request.Builder()
                .post(postBodyImage)
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {

            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                result[i] = response.body().string();
            }
        });
    }

    private void saveImageToGallery(Bitmap bitmap){

        OutputStream fos;
        try {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues =  new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "Image_" + ".jpg");
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "TestFolder"+File.separator+ digit);
                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                fos = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                Objects.requireNonNull(fos);
                Toast.makeText(this, "Image Saved", Toast.LENGTH_SHORT).show();
            }

        }
        catch(Exception e) {
            Toast.makeText(this, "Image not saved \n" + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
