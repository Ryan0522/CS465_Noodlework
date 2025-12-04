package com.example.roomies;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private List<OnboardingPage> pages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean firstRun = prefs.getBoolean("first_run", true);

        if (!firstRun) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        pages = getOnboardingPages();
        viewPager.setAdapter(new OnboardingAdapter(pages));

        btnNext.setOnClickListener(v -> {
            int pos = viewPager.getCurrentItem();
            if (pos < pages.size() - 1) {
                viewPager.setCurrentItem(pos + 1);
            } else {
                finishOnboarding();
            }
        });

        btnSkip.setOnClickListener(v -> finishOnboarding());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == pages.size() - 1) {
                    btnNext.setText("Finish");
                    btnSkip.setVisibility(View.INVISIBLE);
                } else {
                    btnNext.setText("Next");
                    btnSkip.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void finishOnboarding() {
        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("first_run", false).apply();
        goToMain();
    }

    private void goToMain() {
        finish();
    }

    private List<OnboardingPage> getOnboardingPages() {
        List<OnboardingPage> list = new ArrayList<>();

        list.add(new OnboardingPage(
                "Welcome to Roomies!",
                "View chores easily with a clean, simple interface.",
                R.drawable.onboard1
        ));

        list.add(new OnboardingPage(
                "Share Tasks",
                "Add roommates and assign chores with just a tap.",
                R.drawable.onboard2
        ));

        list.add(new OnboardingPage(
                "Stay Organized",
                "Set personalized reminders and never miss a task!",
                R.drawable.onboard3
        ));

        return list;
    }
}

class OnboardingPage {
    String title, description;
    int imageRes;

    OnboardingPage(String t, String d, int img) {
        this.title = t;
        this.description = d;
        this.imageRes = img;
    }
}

class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.PageViewHolder> {

    private List<OnboardingPage> pages;

    OnboardingAdapter(List<OnboardingPage> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_onboarding, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        OnboardingPage page = pages.get(position);
        holder.title.setText(page.title);
        holder.desc.setText(page.description);
        holder.image.setImageResource(page.imageRes);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, desc;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.img);
            title = itemView.findViewById(R.id.title);
            desc = itemView.findViewById(R.id.desc);
        }
    }
}
