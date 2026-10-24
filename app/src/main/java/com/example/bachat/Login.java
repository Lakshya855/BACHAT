package com.example.bachat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

public class Login extends AppCompatActivity {

    private EditText email, password;
    private Button login;
    private View googleSignIn;
    private Database_users db;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

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

        login.setOnClickListener(v -> checkusers_data());

        View registerBtn = findViewById(R.id.textViewRegister);
        if (registerBtn != null) {
            registerBtn.setOnClickListener(v -> {
                startActivity(new Intent(Login.this, Signup.class));
                finish();
            });
        }

        View forgotBtn = findViewById(R.id.textView3_forget);
        if (forgotBtn != null) {
            forgotBtn.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(Login.this, R.style.DarkDialogTheme);
                builder.setTitle("Password Recovery");
                builder.setMessage("Please contact Bachat support or sign in using your linked Google account to restore access.");
                builder.setPositiveButton("OK", null);
                builder.show();
            });
        }

        firebaseAuth = FirebaseAuth.getInstance();
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, options);

        googleSignInLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData()).getResult(ApiException.class);
                    String idToken = account != null ? account.getIdToken() : null;

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
                                        String googleName = user.getDisplayName();
                                        if (googleName == null || googleName.trim().isEmpty()) {
                                            googleName = "BACHAT User";
                                        }
                                        db.insertGoogleUser(googleName, googleEmail, "Yes", "Delhi");
                                        int uId = db.getUserId(googleEmail, "");

                                        Toast.makeText(this, "Welcome, " + googleName + "!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(Login.this, type_of_places.class);
                                        intent.putExtra("USER_ID", uId);
                                        intent.putExtra("GOOGLE_EMAIL", googleEmail);
                                        startActivity(intent);
                                        finish();
                                    }
                                } else {
                                    String msg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                                    Toast.makeText(this, "Sign-in failed: " + msg, Toast.LENGTH_LONG).show();
                                }
                            });
                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in error: " + e.getStatusCode(), Toast.LENGTH_LONG).show();
                }
            }
        });

        googleSignIn.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void checkusers_data() {
        String Email = email.getText().toString().trim();
        String Password = password.getText().toString().trim();

        if (Email.isEmpty() || Password.isEmpty()) {
            Toast.makeText(this, "Please enter both Email and Password", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean result = db.checkData(Email, Password);

        if (result) {
            int userId = db.getUserId(Email, Password);

            AlertDialog.Builder builder = new AlertDialog.Builder(Login.this, R.style.DarkDialogTheme);
            builder.setTitle("Welcome Back!");
            builder.setMessage("Authentication successful.");
            builder.setPositiveButton("Continue", (dialog, which) -> {
                Intent intent = new Intent(Login.this, type_of_places.class);
                intent.putExtra("USER_ID", userId);
                startActivity(intent);
                finish();
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Login.this, R.style.DarkDialogTheme);
            builder.setTitle("Access Denied");
            builder.setMessage("Invalid Email or Password. Please try again.");
            builder.setPositiveButton("Try Again", null);
            builder.show();
        }
    }
}
