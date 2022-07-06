package com.example.recipeapp.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.example.recipeapp.R;
import com.example.recipeapp.RecipeClient;
import com.example.recipeapp.databinding.FragmentRecipeDetailsBinding;
import com.example.recipeapp.models.Recipe;
import com.example.recipeapp.models.User;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

public class RecipeDetailsFragment extends Fragment {
    private static final String TAG = "RecipeDetailsFragment";
    private FragmentRecipeDetailsBinding binding;
    private Recipe recipe;
    private final boolean recipeInDatabase = false;
    private RecipeClient client;

    public RecipeDetailsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentRecipeDetailsBinding.inflate(getLayoutInflater());
        client = new RecipeClient(getContext());
        final Bundle bundle = this.getArguments();
        if (bundle != null) {
            recipe = bundle.getParcelable("Recipe");
            Log.i(TAG, "Received bundle: " + recipe.getTitle());
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.tvRecipeName.setText(recipe.getTitle());
        binding.tvCookTime.setText("Cooktime: " + recipe.getCooktime() + " mins");
        binding.tvCuisine.setText("Cuisine: " + recipe.getCuisineType());
        if (recipe.getImageUrl() != null) {
            Glide.with(getContext()).load(recipe.getImageUrl()).into(binding.ivImage);
        } else {
            Glide.with(getContext()).load(recipe.getImage().getUrl()).into(binding.ivImage);
        }
        if (recipe.getRecipeId() != 0) {
            try {
                getIngredients();
                Log.i(TAG, "list: " + recipe.getIngredientList().toString());

            } catch (IOException e) {
                Log.e(TAG, "Error with getting ingredients", e);
            }
        } else {
            List<String> ingredients = recipe.getIngredientList();
            Log.i(TAG, "Ingredients: " + ingredients.toString());
            for (int i = 0; i < ingredients.size(); i++) {
                binding.tvIngredientList.append("• " + ingredients.get(i) + "\n");
            }
        }

        List<String> instructions = recipe.getInstructions();
        Log.i(TAG, "instructions: " + instructions.toString());
        for (int i = 0; i < instructions.size(); i++) {
            binding.tvInstructionsList.append((i + 1) + ". " + instructions.get(i) + "\n");
        }

        if (recipe.getRecipeId() != 0) {
            binding.tvUploadedBy.setText("");
        } else {
            User.getUser(recipe.getAuthor());
            binding.tvUploadedBy.setText("Uploaded by: @username");
        }

        binding.ibBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackToSearch(v);
            }
        });

        if (recipe.isLikedbyCurrentUser(ParseUser.getCurrentUser())) {
            binding.ibHeart.setBackgroundResource(R.drawable.heart_filled);
        } else {
            binding.ibHeart.setBackgroundResource(R.drawable.heart);
        }
        binding.ibHeart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findRecipe("like");
            }
        });

        if (recipe.isMadebyCurrentUser(ParseUser.getCurrentUser())) {
            // TODO Change Image button color to gray
            binding.btnMade.setText("I Made it!");
        } else {
            binding.btnMade.setText("Make it!");
        }
        binding.btnMade.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findRecipe("made");
            }
        });
    }

    private void findRecipe(String action) {
        ParseQuery<Recipe> query = ParseQuery.getQuery(Recipe.class);
        query.include(Recipe.KEY_RECIPE_ID);
        query.include(Recipe.KEY_AUTHOR);
        query.include(Recipe.KEY_LIKED_BY);
        query.include(Recipe.KEY_MADE_BY);
        query.whereEqualTo(Recipe.KEY_RECIPE_ID, recipe.getRecipeId());
        query.findInBackground(new FindCallback<Recipe>() {
            @Override
            public void done(List<Recipe> objects, ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in finding recipe!");
                    return;
                }
                if (objects.size() == 0) {
                    addRecipeToDatabase(action);
                } else if (action == "like") {
                    likeRecipe();
                } else {
                    madeRecipe();
                }
            }
        });
    }

    private void madeRecipe() {
        if (recipe.isMadebyCurrentUser(ParseUser.getCurrentUser())) {
            // TODO Change Image button color to gray
            binding.btnMade.setText("Make it!");
        } else {
            binding.btnMade.setText("I Made it");
        }

        recipe.madeRecipe(ParseUser.getCurrentUser());

        recipe.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in setting recipe to made" + e);
                    return;
                }
                Log.i(TAG, "Recipe is made by: " + recipe.getMadeBy().toString());
            }
        });
    }


    private void likeRecipe() {
        if (recipe.isLikedbyCurrentUser(ParseUser.getCurrentUser())) {
            binding.ibHeart.setBackgroundResource(R.drawable.heart);
        } else {
            binding.ibHeart.setBackgroundResource(R.drawable.heart_filled);
        }
        recipe.likeRecipe(ParseUser.getCurrentUser());

        recipe.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in liking recipe" + e);
                    return;
                }
                Log.i(TAG, "Recipe is liked by: " + recipe.getLikedBy().toString());
            }
        });
//        binding.tvLikes.setText(post.getLikeCount());
    }

    private void addRecipeToDatabase(String action) {
        recipe.put("uploaded", true);
        recipe.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    Log.e(TAG, "Error in saving current recipe");
                    return;
                }
                Log.i(TAG, "New Recipe saved in database!");
                if (action == "like") {
                    likeRecipe();
                } else {
                    madeRecipe();
                }
            }
        });
    }

    public void getIngredients() throws IOException {
        List<String> ingredients = new ArrayList<>();

        client.getRecipesDetailed(recipe.getRecipeId(), new JsonHttpResponseHandler() {

            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess! " + json.toString());
                JSONArray jsonArray = null;
                try {
                    jsonArray = json.jsonObject.getJSONArray("extendedIngredients");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        ingredients.add(i, jsonArray.getJSONObject(i).getString("original"));
                    }
                    Log.i(TAG, "Saved ingredients " + recipe.getIngredientList().toString());
                    for (int i = 0; i < ingredients.size(); i++) {
                        binding.tvIngredientList.append("• " + ingredients.get(i) + "\n");
                    }
                    recipe.setIngredientList(ingredients);
                } catch (JSONException e) {
                    Log.e(TAG, "Hit JSON exception", e);
                }

            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure" + response, throwable);
            }
        });

        Log.i(TAG, "ingredients: " + ingredients);
    }


    public void goBackToSearch(View view) {
        NavHostFragment.findNavController(this).navigate(R.id.recipeSearchFragment);
    }


}