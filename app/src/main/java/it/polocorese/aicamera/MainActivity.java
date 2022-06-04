/*
 * Copyright (c) 2022 Nicolò Cantori
 *
 * This file is part of AI Camera.
 *
 * AI Camera is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * AI Camera is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with AI Camera. If not, see <https://www.gnu.org/licenses/>
 */

package it.polocorese.aicamera;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.label.ImageLabel;
import com.google.mlkit.vision.label.ImageLabeler;
import com.google.mlkit.vision.label.ImageLabeling;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private File photoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File directory = new File(getFilesDir(), "photos");
        if (!directory.exists()) {
            directory.mkdir();
        }

        photoFile = new File(directory, "photo.jpg");
        setContentView(R.layout.activity_main);
        Button takePicture = findViewById(R.id.btn_take_pic);
        takePicture.setOnClickListener(v -> onTakePicture());
    }

    private void onTakePicture() {
        Uri uri = FileProvider.getUriForFile(this, "it.polocorese.aicamera.fileprovider", photoFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, 10);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
            if (bitmap != null) {

                // Rotate the image if necessary:
                
                try {
                    ExifInterface exif = new ExifInterface(photoFile.getAbsolutePath());
                    int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                    int rotation = 0;
                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotation = 90;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotation = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotation = 270;
                            break;
                    }
                    if (rotation > 0) {
                        Matrix matrix = new Matrix();
                        matrix.postRotate(rotation);
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                    }
                } catch (IOException ignored) {}

                Log.i("MYTAG", "IMAGE RESOLUTION: " + bitmap.getWidth() + " x " + bitmap.getHeight());
                ImageView ivPhoto = findViewById(R.id.iv_photo);
                ivPhoto.setImageBitmap(bitmap);

                // Photo recognition:
                
                ImageLabeler labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS);
                InputImage inputImage = InputImage.fromBitmap(bitmap, 0);
                labeler.process(inputImage).addOnSuccessListener(imageLabels -> {
                    String message = "In this photo I seem to recognize:\n";
                    for (ImageLabel label : imageLabels) {
                        message += " - " + label.getText() + " (" + Math.round(label.getConfidence() * 100f) + "%)\n";
                    }
                    AlertDialog.Builder b = new AlertDialog.Builder(this);
                    b.setMessage(message);
                    b.show();
                });

                // Cleanup
                photoFile.delete();
            }
        }
    }
}
