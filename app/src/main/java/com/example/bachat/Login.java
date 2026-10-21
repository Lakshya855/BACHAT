package com.example.bachat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
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

public class Login extends AppCompatActivity {

    EditText email,password;
    Button login;
    LinearLayout googleSignIn;
    Database_users db;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private int RC_SIGN_IN = 100;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.editText_Email);
        password = findViewById(R.id.editText_Password);
        login = findViewById(R.id.button_Login);
        googleSignIn = findViewById(R.id.googleSignInLogin);
        db = new Database_users(getApplicationContext());

        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {checkusers_data();}
        });

        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN);
        builder.requestIdToken(getString(R.string.default_web_client_id));
        builder.requestEmail();
        GoogleSignInOptions options = builder.build();
        googleSignInClient = GoogleSignIn.getClient(this,options);

        googleSignIn.setOnClickListener(v -> {
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
                                    String googleEmail = user.getEmail();
                                    // Log them in directly
                                    Toast.makeText(this, "Google sign-in successful", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(Login.this, type_of_places.class);
                                    intent.putExtra("GOOGLE_EMAIL", googleEmail);
                                    startActivity(intent);
                                    finish();
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

    public void checkusers_data(){
        String Email = email.getText().toString();
        String Password = password.getText().toString();

        boolean result = db.checkData(Email, Password);

        if(result){
            Toast.makeText(Login.this, "Login Successful", Toast.LENGTH_LONG).show();
            int userId = db.getUserId(Email, Password);
            Intent intent = new Intent(Login.this, type_of_places.class);
            intent.putExtra("USER_ID", userId);
            startActivity(intent);
            finish();
        }
        else{
            Toast.makeText(Login.this, "Invalid Email or Password", Toast.LENGTH_LONG).show();
        }
    }
}