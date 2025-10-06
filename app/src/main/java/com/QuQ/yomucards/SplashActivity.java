package com.QuQ.yomucards;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String KEY_POLICY_ACCEPTED = "policyAccepted";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        logo.startAnimation(fadeIn);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean policyAccepted = prefs.getBoolean(KEY_POLICY_ACCEPTED, false);

        if (policyAccepted) {
            // Если пользователь уже принял политику — запускаем приложение
            startMainActivityWithDelay();
        } else {
            // Иначе показываем диалог
            showPrivacyPolicyDialog(prefs);
        }
    }

    private void showPrivacyPolicyDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(this)
                .setTitle("Политика конфиденциальности")
                .setMessage("Перед использованием приложения необходимо принять политику конфиденциальности.\n\n"
                        + "Нажмите «Принять», если вы согласны с условиями, или «Выйти», чтобы закрыть приложение.")
                .setCancelable(false)
                .setPositiveButton("Принять", (dialog, which) -> {
                    // Сохраняем согласие
                    prefs.edit().putBoolean(KEY_POLICY_ACCEPTED, true).apply();
                    startMainActivityWithDelay();
                })
                .setNegativeButton("Выйти", (dialog, which) -> {
                    // Закрыть приложение
                    finishAffinity();
                })
                .setNeutralButton("Подробнее", (dialog, which) -> {
                    // Открываем ссылку на политику
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://sites.google.com/view/quqid/yomucards?authuser=0"));
                    startActivity(browserIntent);
                    showPrivacyPolicyDialog(prefs);
                })
                .show();
    }

    private void startMainActivityWithDelay() {
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        }, 1000); // задержка для анимации
    }
}
