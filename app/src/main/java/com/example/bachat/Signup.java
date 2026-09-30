package com.example.bachat;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Signup extends AppCompatActivity {

    private EditText id,name,email,password;
    private RadioGroup radioGroupgender;
    private CheckBox policy;
    private Button go;
    Database_users db;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        name = findViewById(R.id.editTextName);
        email = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextPassword);
        radioGroupgender = findViewById(R.id.Radiogroupgender);
        policy = findViewById(R.id.checkBoxNewsletter);
        go = findViewById(R.id.buttonSave);

        //initialise database helper
        db = new Database_users(getApplicationContext());

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void saveUserData(){
        String Username = name.getText().toString();
        String Email = email.getText().toString();
        String Password = password.getText().toString();
        String Gender = ((RadioButton) findViewById(radioGroupgender.getCheckedRadioButtonId())).getText().toString();
        String Policy = policy.isChecked() ? "Yes":"No";

        boolean isInserted = db.insertData(Username,Email,Password,Gender,Policy);

        if(isInserted){
            Toast.makeText(getApplicationContext(),"Data entered successfully",Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
        }
    }
}