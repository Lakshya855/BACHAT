package com.example.bachat;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Login extends AppCompatActivity {

    EditText email,password;
    Button login;
    Database_users db;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.editText_Email);
        password = findViewById(R.id.editText_Password);
        login = findViewById(R.id.button_Login);
        db = new Database_users(getApplicationContext());

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {checkusers_data();}
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void checkusers_data(){
        String Email = email.getText().toString();
        String Password = password.getText().toString();

        boolean result = db.checkData(Email, Password);

        if(result){
            Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(Login.this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
        }
    }
}