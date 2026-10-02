package com.example.bachat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Signup extends AppCompatActivity {

    private EditText id,name,email,password;
    private TextView Terms;
    private RadioGroup radioGroupgender;
    private CheckBox policy;
    private Button go,contactSupport;
    Database_users db;
    @SuppressLint("WrongViewCast")
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
        contactSupport = findViewById(R.id.contact_support);
        Terms = findViewById(R.id.terms);

        //initialise database helper
        db = new Database_users(getApplicationContext());

        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserData();
            }
        });

        contactSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:+916378772026"));
                startActivity(intent);
            }
        });

        Terms.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(Signup.this);
                builder.setTitle("Agreement");
                builder.setMessage("By creating an account, you agree to use Bachat for monitoring and optimizing electricity consumption. You understand that the electricity-saving suggestions provided by the app are recommendations and actual savings may vary depending on usage, appliance efficiency, electricity tariffs, and other factors.\n" +
                        "\n" +
                        "You agree to provide accurate information and use the application responsibly. Your account information will be handled according to our privacy policy.");
                builder.setPositiveButton("Ok",null);
                builder.show();
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

        int selectedId = radioGroupgender.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_LONG).show();
            return;
        }

        String Gender = ((RadioButton) findViewById(radioGroupgender.getCheckedRadioButtonId())).getText().toString();
        String Policy = policy.isChecked() ? "Yes":"No";

        if(Policy.equals("No")){
            Toast.makeText(this,"Please agree to our terms and policy",Toast.LENGTH_LONG).show();
            name.setText("");
            email.setText("");
            password.setText("");
            radioGroupgender.clearCheck();
            policy.setChecked(false);
            return;
        }

        boolean isInserted = db.insertData(Username,Email,Password,Gender,Policy);

        if(isInserted){
            Toast.makeText(getApplicationContext(),"Data entered successfully",Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
        }
    }
}