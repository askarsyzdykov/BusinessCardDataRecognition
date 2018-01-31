package com.example.androidocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.leptonica.android.Box;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.memfis19.annca.Annca;
import io.github.memfis19.annca.internal.configuration.AnncaConfiguration;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";

    private static final int REQUEST_CAMERA_PERMISSIONS = 931;
    private static final int CAPTURE_MEDIA = 368;

    Bitmap businessCard;
    private TessBaseAPI mTess;
    String datapath = "";

    ExtendedImageView imageView;
    TextView runOCR;
    TextView displayText;
    EditText etEmail;
    EditText etPhone;
    EditText etWebsite;
    EditText etName;
    TextView openContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ExtendedImageView) findViewById(R.id.imageView);
        runOCR = (TextView) findViewById(R.id.textView);
        openContacts = (TextView) findViewById(R.id.textView6);

        displayText = (TextView) findViewById(R.id.textView2);

        etName = (EditText) findViewById(R.id.etName);
        etPhone = (EditText) findViewById(R.id.etPhone);
        etWebsite = (EditText) findViewById(R.id.etWebsite);
        etEmail = (EditText) findViewById(R.id.etEmail);

        findViewById(R.id.btnCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AnncaConfiguration.Builder builder = new AnncaConfiguration.Builder(MainActivity.this, CAPTURE_MEDIA);
                if (ActivityCompat.checkSelfPermission(
                        MainActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                new Annca(builder.build()).launchCamera();
            }
        });

        //init businessCard
        businessCard = BitmapFactory.decodeResource(getResources(), R.drawable.test_rus);

        //initialize Tesseract API
        String language = "rus+eng";
        datapath = getFilesDir() + "/tesseract/";
        mTess = new TessBaseAPI();

        checkFile(new File(datapath + "tessdata/"));

        mTess.init(datapath, language);

        //run the OCR on the test_image...
        runOCR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etEmail.setText("");
                etPhone.setText("");
                etName.setText("");
                etWebsite.setText("");
                processImage();
            }
        });

        //Add the extracted info from Business Card to the phone's contacts...
        openContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addToContacts();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAPTURE_MEDIA && resultCode == RESULT_OK) {
            Log.d(TAG, "resultCodeActiRes:  " + requestCode);
            String filePath = data.getStringExtra(AnncaConfiguration.Arguments.FILE_PATH);
            Log.d(TAG, "filePathInMainActivity: " + filePath);
            /*Intent i = new Intent(MainActivity.this, CropImage.class);
            onPhotoTaken();
            MainActivity.this.finish();
            startActivity(i);*/

            businessCard = BitmapFactory.decodeFile(filePath);
            imageView.setImageBitmap(businessCard);

            //processImage();
        } else {
            Log.v(TAG, "User cancelled");
        }
    }

    public void processImage() {

        mTess.setImage(businessCard);

        String ocrResult = mTess.getUTF8Text();
        Log.d(TAG, ocrResult);

        final ResultIterator iterator = mTess.getResultIterator();
        String lastUTF8Text;
        float lastConfidence;
        int count = 0;
        iterator.begin();
        List<RectF> rectangles = new ArrayList<>();
        do {
            lastUTF8Text = iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            lastConfidence = iterator.confidence(TessBaseAPI.PageIteratorLevel.RIL_WORD);
            Log.d("suka", "lastUTF8Text = " + lastUTF8Text);
            Log.d("suka", "lastConfidence = " + lastConfidence);
            if (lastConfidence > 70) {
                Box box = mTess.getWords().getBox(count);
                rectangles.add(new RectF(box.getRect()));
            }

            count++;
        } while (iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD));

        imageView.addRects(rectangles);

        extractName(ocrResult);
        extractEmail(ocrResult);
        extractPhone(ocrResult);
        extractWebsite(ocrResult);
    }

    public void extractName(String str) {
        System.out.println("Getting the Name");
        final String NAME_REGEX = "^([А-Я]([а-я]*|\\.) *){1,2}([А-Я][а-я]+-?)+$";
        Pattern p = Pattern.compile(NAME_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);
        if (m.find()) {
            System.out.println(m.group());
            etName.setText(m.group());
        }
    }

    public void extractEmail(String str) {
        System.out.println("Getting the email");
        final String EMAIL_REGEX = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        Pattern p = Pattern.compile(EMAIL_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            System.out.println(m.group());
            etEmail.setText(m.group());
        }
    }

    public void extractPhone(String str) {
        //str = "+77029488168 77029488168";
        System.out.println("Getting Phone Number");
        //final String PHONE_REGEX="(?:^|\\D)(\\d{3})[)\\-. ]*?(\\d{3})[\\-. ]*?(\\d{4})(?:$|\\D)";
        final String PHONE_REGEX = "(?:\\+\\W?)(\\d)[(\\-. ]*?(\\d{3})[)\\-. ]*?(\\d{3})[\\-. ]*?(\\d{2})[\\-. ]*?(\\d{2})(?:$|\\D)";
        Pattern p = Pattern.compile(PHONE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            System.out.println(m.group());
            etPhone.setText(m.group());
        }
    }

    public void extractWebsite(String str) {
        System.out.println("Getting Website");
        final String WEBSITE_REGEX = "[-a-zA-Z0-9:%_\\+.~#?&//=]{2,256}\\.[a-z]{2,4}\\b(\\/[-a-zA-Z0-9@:%_\\+.~#?&//=]*)?";
        Pattern p = Pattern.compile(WEBSITE_REGEX, Pattern.MULTILINE);
        Matcher m = p.matcher(str);   // get a matcher object
        if (m.find()) {
            System.out.println(m.group());
            etWebsite.setText(m.group());
        }
    }

    private void checkFile(File dir) {
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/rus.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles();
            }
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/rus.traineddata";

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/rus.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addToContacts() {

        // Creates a new Intent to insert a contact
        Intent intent = new Intent(ContactsContract.Intents.Insert.ACTION);
        // Sets the MIME type to match the Contacts Provider
        intent.setType(ContactsContract.RawContacts.CONTENT_TYPE);

        //Checks if we have the name, email and phone number...
        if (etName.getText().length() > 0 && (etPhone.getText().length() > 0 || etEmail.getText().length() > 0)) {
            //Adds the name...
            intent.putExtra(ContactsContract.Intents.Insert.NAME, etName.getText());

            //Adds the email...
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL, etEmail.getText());
            //Adds the email as Work Email
            intent.putExtra(ContactsContract.Intents.Insert.EMAIL_TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);

            //Adds the phone number...
            intent.putExtra(ContactsContract.Intents.Insert.PHONE, etPhone.getText());
            //Adds the phone number as Work Phone
            intent.putExtra(ContactsContract.Intents.Insert.PHONE_TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);

            //starting the activity...
            startActivity(intent);
        } else {
            Toast.makeText(getApplicationContext(), "No information to add to contacts!", Toast.LENGTH_LONG).show();
        }


    }
}
