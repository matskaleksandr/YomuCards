package com.QuQ.yomucards;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Находим ImageView по ID
        ImageView logo = findViewById(R.id.splash_logo);

        // Загружаем анимацию из ресурсов
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);

        // Применяем анимацию к логотипу
        logo.startAnimation(fadeIn);

        // Задержка перед переходом на главную активность
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Переход на главную активность
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
                finish();  // Закрыть SplashActivity, чтобы не вернуться назад
            }
        }, 1000);  // 2 секунды задержки
    }
}
