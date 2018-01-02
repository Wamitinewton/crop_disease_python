package com.neoneye.android;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import static com.neoneye.android.Constants.serverloc;

public class MainActivity extends AppCompatActivity {

    private ImageButton imgbutton;
    private File plantimg;
    private EditText cropname;
    private Button submit;
    private ImageView cropimg;
    Activity currActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        currActivity = this;
        currActivity.setTitle("PDDA");


//        View v = inflater.inflate(R.layout.activity_main, container, false);
        Context context = this.getBaseContext();

        submit = (Button) findViewById(R.id.button);
        cropname = (EditText) findViewById(R.id.editText);
        imgbutton = (ImageButton) findViewById(R.id.imageButton);
        cropimg = (ImageView) findViewById(R.id.imageView);

        // On clicking submit
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String crop_name_send = cropname.getText().toString();

                if(plantimg == null){
                    Toast.makeText(getApplicationContext(), "Image daalo bhai. Please try again!", Toast.LENGTH_SHORT).show();
                }
                else{
                    CropSend input = new CropSend(crop_name_send, plantimg);
                    new Submit().execute(input);
                }
            }
        });


        // On clicking image select button
        imgbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("Msg","reached here");
                int result = ContextCompat.checkSelfPermission(currActivity, Manifest.permission.READ_EXTERNAL_STORAGE);
                if(result == PackageManager.PERMISSION_GRANTED){
                    Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//                    // Start the Intent
                    startActivityForResult(galleryIntent, 0);
                }
                else{
                    ActivityCompat.requestPermissions(currActivity,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
                }

            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            //pick image from gallery
            Bitmap bitmap = null;
            Uri selectedImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            // Get the cursor
            Cursor cursor = currActivity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            // Move to first row
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String imgDecodableString = cursor.getString(columnIndex);
            cursor.close();
            plantimg = new File(imgDecodableString);
            bitmap = BitmapFactory.decodeFile(plantimg.getPath());
            if(bitmap != null){
                System.out.println(bitmap.toString());
                System.out.println("done");
                cropimg.setImageBitmap(bitmap);
            }
            else{
                System.out.println("error");
            }
        }
    }

    private class Submit extends AsyncTask<CropSend, String , JSONObject>{

        protected JSONObject doInBackground(CropSend... args) {
            CropSend inp;

            inp = args[0];

            try {
                String charset = "UTF-8";
                String requestURL = serverloc;

                MultipartUtility multipart = new MultipartUtility(requestURL, charset);
                if(!inp.cropname.isEmpty()){
//                    multipart.addFormField("text_present", "true");
                    multipart.addFormField("crop_name", inp.cropname);
                }
                else{
                    multipart.addFormField("text_present", "false");
                }
                if(inp.plantimg !=null){
//                    multipart.addFormField("image_present", "true");
                    multipart.addFilePart("crop_image", inp.plantimg);
                }
                else {
                    multipart.addFormField("image_present", "false");
                }
                return new JSONObject(multipart.finish()); // response from server.

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject response){
            super.onPostExecute(response);
            try {
                Log.d("resp",response.toString());
                if(response.getBoolean("status")){
                    Intent intent = new Intent(currActivity, DisplayResults.class);
                    String message = response.getJSONArray("response").toString();
                    intent.putExtra("Json",message);
                    startActivity(intent);
                }
                else{
                    displayError();
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        void displayError(){
            Toast.makeText(currActivity.getApplicationContext(), "Error submitting crop, Please try again", Toast.LENGTH_SHORT).show();
        }
    }
}
