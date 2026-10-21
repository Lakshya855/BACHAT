package com.example.bachat;

import android.annotation.SuppressLint;
import android.app.Activity;
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

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class Signup extends AppCompatActivity {

    private EditText id,name,email,password;
    private RadioGroup radioGroupgender;
    private CheckBox policy;
    private Button go,contactSupport;
    Database_users db;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    View google;
    private int RC_SIGN_IN = 100;
    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
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
        google = findViewById(R.id.googleSignIn);

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

        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
        builder.requestIdToken(getString(R.string.default_web_client_id));
        builder.requestEmail();
        GoogleSignInOptions options = builder.build();
        googleSignInClient = GoogleSignIn.getClient(this,options);

        google.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {

            if (resultCode != Activity.RESULT_OK || data == null) {
                Toast.makeText(this, "Google sign-in cancelled or failed", Toast.LENGTH_LONG).show();
                return;
            }

            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);

                String idToken = account.getIdToken();

                if (idToken == null) {
                    Toast.makeText(this, "Google ID token is missing", Toast.LENGTH_LONG).show();
                    return;
                }

                AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);


                firebaseAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task -> {

                            if (task.isSuccessful()) {
                                FirebaseUser user = firebaseAuth.getCurrentUser();

                                if (user != null) {
                                    String googleName = user.getDisplayName();
                                    String googleEmail = user.getEmail();

                                    if (googleEmail == null || googleEmail.trim().isEmpty()) {
                                        Toast.makeText(this, "Could not retrieve Google email", Toast.LENGTH_LONG).show();
                                        return;
                                    }

                                    if (googleName == null || googleName.trim().isEmpty()) {
                                        googleName = "BACHAT User";
                                    }

                                    boolean inserted = db.insertGoogleUser(googleName, googleEmail, "Yes");

                                    if (inserted) {
                                        Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Signup.this, type_of_places.class);

                                        intent.putExtra("GOOGLE_EMAIL", googleEmail);
                                        intent.putExtra("PROFILE_INCOMPLETE", true);

                                        startActivity(intent);
                                        finish();

                                    } else {
                                        Toast.makeText(this, "Could not save Google user details", Toast.LENGTH_LONG).show();
                                    }
                                }

                            } else {
                                String message = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                Toast.makeText(this, "Firebase sign-in failed: " + message, Toast.LENGTH_LONG).show();
                            }
                        });


            } catch (ApiException e) {
                Toast.makeText(this, "Google sign-in error: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
            }
        }
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
            Intent intent = new Intent(Signup.this, Login.class);
            startActivity(intent);
            finish();
        }
        else{
            Toast.makeText(getApplicationContext(),"Error",Toast.LENGTH_LONG).show();
        }
    }
}