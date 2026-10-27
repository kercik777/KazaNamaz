package tm.kaza.namaz.ag;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import tm.kaza.namaz.ag.data.DataStore;
import tm.kaza.namaz.ag.util.SystemBarsHelper;
import tm.kaza.namaz.ag.util.ThemeUtil;

/**
 * Экран загрузки: применяет тему и плавно открывает главный экран.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtil.apply(this);
        super.onCreate(savedInstanceState);
        SystemBarsHelper.edgeToEdge(this);
        setContentView(R.layout.activity_splash);

        View logo = findViewById(R.id.splashContent);
        if (logo != null) {
            logo.setAlpha(0f);
            logo.animate()
                    .alpha(1f)
                    .setDuration(650)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }, 1000);
    }
}
