package com.example.recipeapp.adapters;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.recipeapp.R;
import com.example.recipeapp.databinding.ItemRecipeCardBinding;
import com.example.recipeapp.models.Recipe;

import java.util.List;

public class RecipeSearchAdapter extends RecyclerView.Adapter<RecipeSearchAdapter.ViewHolder> {
    private static final String TAG = "RecipeSearchAdapter";
    public List<Recipe> recipesList;
    private final Context context;
    private ItemRecipeCardBinding item_binding;

    public RecipeSearchAdapter(Context context, List<Recipe> recipes) {
        this.context = context;
        this.recipesList = recipes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        item_binding = ItemRecipeCardBinding.inflate(LayoutInflater.from(context), parent, false);
        return new ViewHolder(item_binding);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Recipe recipe = recipesList.get(position);
        holder.bind(recipe);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(Recipe.class.getSimpleName(), recipe);
        holder.itemView.setOnClickListener(Navigation.createNavigateOnClickListener(R.id.action_recipeSearchFragment_to_recipeDetailsFragment, bundle));
    }

    @Override
    public int getItemCount() {
        return recipesList.size();
    }

    public void clear() {
        recipesList.clear();
        notifyDataSetChanged();
    }

    public void addAll(List<Recipe> list) {
        recipesList.addAll(list);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{
        public ItemRecipeCardBinding binding;
        private Recipe currentRecipe;

        public ViewHolder(@NonNull ItemRecipeCardBinding itemView) {
            super(itemView.getRoot());
            this.binding = itemView;
        }

        public void bind(Recipe recipe) {
            currentRecipe = recipe;
            binding.tvTitle.setText(recipe.getTitle());
            Glide.with(context).load(recipe.getImageUrl()).into(binding.ivImage);
            binding.tvCookTime.setText(recipe.getCooktime() + "m");
            binding.tvCuisine.setText(recipe.getCuisineType());
        }

    }
}
