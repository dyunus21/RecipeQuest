package com.example.recipeapp.fragments;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.recipeapp.R;
import com.example.recipeapp.databinding.FragmentUploadRecipeBinding;
import com.example.recipeapp.models.BitmapScaler;
import com.example.recipeapp.models.Recipe;
import com.example.recipeapp.models.User;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class UploadRecipeFragment extends Fragment {
    public final static int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 42;
    public final static int PICK_PHOTO_CODE = 1046;
    private static final String TAG = "FragmentUploadRecipe";
    public String photoFileName = "photo.jpg";
    private File photoFile;
    private FragmentUploadRecipeBinding binding;

    public UploadRecipeFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentUploadRecipeBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToUpload();
            }
        });

        binding.btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchCamera();
            }
        });
        binding.btnGallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPickPhoto(v);
            }
        });
        binding.btnPublish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateRecipe();
            }
        });

    }

    private void validateRecipe() {
        final String title = binding.etRecipeName.getText().toString();
        final String cuisineType = binding.etCuisine.getText().toString();
        final int cooktime = Integer.parseInt(binding.etCooktime.getText().toString());  // TODO: Inform user if inputted string
        final String ingredients = binding.etIngredientList.getText().toString();
        final String instructions = binding.etInstructions.getText().toString();

        //TODO: Later update to TOAST messages regarding specific fields
        if (title.isEmpty() || cuisineType.isEmpty() || cooktime == 0 || ingredients.isEmpty() || instructions.isEmpty()) {
            Toast.makeText(getContext(), "Field cannot be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile == null || binding.ivImage.getDrawable() == null) {
            Toast.makeText(getContext(), "Post does not contain any image!", Toast.LENGTH_SHORT).show();
            return;
        }
        publishRecipe(title, cuisineType, cooktime, ingredients, instructions);
    }

    private void publishRecipe(final String title, final String cuisineType, final int cooktime, final String ingredients, final String instructions) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setCuisineType(cuisineType);
        recipe.setCooktime(cooktime);
        recipe.setAuthor(new User(ParseUser.getCurrentUser()));
        List<String> ingredientList = Arrays.asList(ingredients.split("\n"));
        Log.i(TAG, "Ingredient List Uploaded: " + ingredientList);
        recipe.setIngredientList(ingredientList);
        List<String> instructionList = Arrays.asList(instructions.split("\n"));
        Log.i(TAG, "Instruction List Uploaded: " + instructionList);
        recipe.setInstructions(instructionList);
        recipe.setImage(new ParseFile(photoFile));
        Log.i(TAG, "Finished inputting recipe details");
        recipe.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error with saving recipe", e);
                    Toast.makeText(getContext(), "Unable to save recipe!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.i(TAG, "Successfully saved recipe: " + recipe.getTitle());
                binding.etRecipeName.setText("");
                binding.etCuisine.setText("");
                binding.etCooktime.setText("");
                binding.etIngredientList.setText("");
                binding.etInstructions.setText("");
                binding.ivImage.setImageResource(0);
            }
        });
    }

    private void goBackToUpload() {
        NavHostFragment.findNavController(this).navigate(R.id.uploadFragment);
    }

    // TODO: Move this to ImageClient later
    public void onPickPhoto(View view) {
        Log.i(TAG, "onPickPhoto!");
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        Log.i(TAG, "start intent for gallery!");
        startActivityForResult(intent, PICK_PHOTO_CODE);

    }

    public File resizeFile(final Bitmap image) {
        Bitmap resizedBitmap = BitmapScaler.scaleToFitWidth(image, 800);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes);
        File resizedFile = getPhotoFileUri(photoFileName);
        try {
            resizedFile.createNewFile();
            FileOutputStream fos = null;
            fos = new FileOutputStream(resizedFile);
            fos.write(bytes.toByteArray());
            fos.close();
        } catch (IOException e) {
            Log.e(TAG, "Unable to create new file ", e);
        }
        Log.i(TAG, "File: " + resizedFile);
        binding.ivImage.setImageBitmap(resizedBitmap);
        return resizedFile;
    }

    @SuppressLint("Range")
    public String getFileName(final Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public File getPhotoFileUri(final String fileName) {
        File mediaStorageDir = new File(getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES), TAG);

        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Log.d(TAG, "failed to create directory");
        }

        return new File(mediaStorageDir.getPath() + File.separator + fileName);
    }

    public Bitmap loadFromUri(final Uri photoUri) {
        Bitmap image = null;
        try {
            // check version of Android on device
            if (Build.VERSION.SDK_INT > 27) {
                ImageDecoder.Source source = ImageDecoder.createSource(getContext().getContentResolver(), photoUri);
                image = ImageDecoder.decodeBitmap(source);
            } else {
                image = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), photoUri);
            }
        } catch (IOException e) {
            Log.e(TAG, "Unable to load image from URI", e);
        }
        return image;
    }

    private void launchCamera() {
        final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        photoFile = getPhotoFileUri(photoFileName);

        final Uri fileProvider = FileProvider.getUriForFile(getContext(), "com.example.provider", photoFile);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider);
        if (intent.resolveActivity(getContext().getPackageManager()) != null) {
            this.startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            Log.i(TAG, "onActivity result camera");
            if (resultCode == RESULT_OK) {
                final Bitmap takenImage = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                photoFile = resizeFile(takenImage);
                Log.i(TAG, "File: " + photoFile.toString());
            } else {
                Toast.makeText(getContext(), "Picture wasn't taken!", Toast.LENGTH_SHORT).show();
            }
        } else if ((data != null) && requestCode == PICK_PHOTO_CODE) {
            final Uri photoUri = data.getData();
            Bitmap selectedImage = loadFromUri(photoUri);
            photoFile = getPhotoFileUri(getFileName(photoUri));
            photoFile = resizeFile(selectedImage);
            Log.i(TAG, "File: " + photoFile.toString());
        }
    }
}