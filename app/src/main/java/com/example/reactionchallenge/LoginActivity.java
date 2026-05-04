package com.example.reactionchallenge;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.reactionchallenge.util.Prefs;

public class LoginActivity extends AppCompatActivity {

    private EditText etName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!TextUtils.isEmpty(Prefs.getPlayerName(this))) {
            startActivity(new Intent(this, MainMenuActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_login);
        etName = findViewById(R.id.etName);
        Button btnContinue = findViewById(R.id.btnContinue);

        btnContinue.setOnClickListener(v -> {
            String name = etName.getText() == null ? "" : etName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Ingresá un nombre para continuar", Toast.LENGTH_SHORT).show();
                return;
            }
            Prefs.setPlayerName(this, name);
            startActivity(new Intent(this, MainMenuActivity.class));
            finish();
        });
    }
}
