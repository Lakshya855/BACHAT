package com.example.bachat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
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

import java.io.File;
import java.io.FileOutputStream;

public class Signup extends AppCompatActivity {

    private EditText name, email, password;
    private RadioGroup radioGroupgender;
    private CheckBox policy;
    private Spinner spinnerState;
    private Button go, contactSupport;
    private ImageView imageViewProfilePhoto;
    private Database_users db;
    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private View google;

    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickGalleryLauncher;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        name = findViewById(R.id.editTextName);
        email = findViewById(R.id.editTextEmail);
        password = findViewById(R.id.editTextPassword);
        radioGroupgender = findViewById(R.id.Radiogroupgender);
        spinnerState = findViewById(R.id.spinnerState);
        policy = findViewById(R.id.checkBoxNewsletter);
        go = findViewById(R.id.buttonSave);
        contactSupport = findViewById(R.id.contact_support);
        google = findViewById(R.id.googleSignIn);
        imageViewProfilePhoto = findViewById(R.id.imageViewProfilePhoto);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PowerUsageDataset.INDIAN_STATES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerState.setAdapter(adapter);

        takePictureLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Bundle extras = result.getData().getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        imageViewProfilePhoto.setImageBitmap(imageBitmap);
                        saveProfileBitmap(imageBitmap);
                    }
                }
            }
        });

        pickGalleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri imageUri = result.getData().getData();
                if (imageUri != null) {
                    imageViewProfilePhoto.setImageURI(imageUri);
                }
            }
        });

        imageViewProfilePhoto.setOnClickListener(v -> showPhotoOptionsDialog());

        db = new Database_users(getApplicationContext());

        go.setOnClickListener(v -> saveUserData());

        contactSupport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:+916378772026"));
            startActivity(intent);
        });

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
                        Toast.makeText(this, "Google ID token missing", Toast.LENGTH_LONG).show();
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

                                        String state = spinnerState.getSelectedItem() != null ? spinnerState.getSelectedItem().toString() : "Delhi";
                                        boolean inserted = db.insertGoogleUser(googleName, googleEmail, "Yes", state);

                                        if (inserted) {
                                            int uId = db.getUserId(googleEmail, "");
                                            Toast.makeText(this, "Welcome to Bachat, " + googleName + "!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(Signup.this, type_of_places.class);
                                            intent.putExtra("USER_ID", uId);
                                            intent.putExtra("GOOGLE_EMAIL", googleEmail);
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
        });

        google.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void saveProfileBitmap(Bitmap bitmap) {
        try {
            File file = new File(getFilesDir(), "user_profile.png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception ignored) {}
    }

    private void showPhotoOptionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DarkDialogTheme);
        builder.setTitle("Profile Avatar");
        String[] options = {"Take Photo", "Choose from Gallery"};
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    takePictureLauncher.launch(takePictureIntent);
                } else {
                    Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
                }
            } else if (which == 1) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickGalleryLauncher.launch(pickPhoto);
            }
        });
        builder.show();
    }

    private void saveUserData() {
        String Username = name.getText().toString().trim();
        String Email = email.getText().toString().trim();
        String Password = password.getText().toString().trim();

        if (Username.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Email.isEmpty() || !Email.contains("@")) {
            Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Password.length() < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedId = radioGroupgender.getCheckedRadioButtonId();
        if (selectedId == -1) {
            Toast.makeText(this, "Please select your gender", Toast.LENGTH_LONG).show();
            return;
        }

        RadioButton selectedRb = findViewById(selectedId);
        String Gender = selectedRb != null ? selectedRb.getText().toString() : "Other";
        String Policy = policy.isChecked() ? "Yes" : "No";
        String state = spinnerState.getSelectedItem() != null ? spinnerState.getSelectedItem().toString() : "Delhi";

        if (Policy.equals("No")) {
            Toast.makeText(this, "Please agree to our terms and policy to proceed", Toast.LENGTH_LONG).show();
            return;
        }

        boolean isInserted = db.insertData(Username, Email, Password, Gender, Policy, state);

        if (isInserted) {
            AlertDialog.Builder builder = new AlertDialog.Builder(Signup.this, R.style.DarkDialogTheme);
            builder.setTitle("Account Created! 🎉");
            builder.setMessage("Your Bachat account is ready. Log in to start tracking and optimizing your electricity bills.");
            builder.setPositiveButton("Go to Login", (dialog, which) -> {
                Intent intent = new Intent(Signup.this, Login.class);
                startActivity(intent);
                finish();
            });
            builder.setCancelable(false);
            builder.show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(Signup.this, R.style.DarkDialogTheme);
            builder.setTitle("Registration Failed");
            builder.setMessage("An account with this email may already exist. Please try signing in.");
            builder.setPositiveButton("Try Again", null);
            builder.show();
        }
    }
}
