package com.vippygames.mapnector.LoginSystem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.vippygames.mapnector.MapsActivity;
import com.vippygames.mapnector.R;
import com.vippygames.mapnector.DBStorage.User;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

/**
 * This class job is to manage the login, signup stuff of the user. It allows user to login, signup, checks for email
 * verification and provides explanations for errors that pop up while trying to get into the main application.
 * If user passed current "security" step successfully, this activity is destroyed and we get to the main one, MapsActivity.
 */

public class LoginActivity extends AppCompatActivity implements View.OnClickListener {

    //login views stuff
    private EditText edMail, edPass;
    private Button btnLogin, btnCreateAccount;

    private FirebaseAuth mAuth;

    /**intent to move to mapsActivity if authentication succeed */
    private Intent intent;

    /** signup dialog */
    private Dialog signupDialog;

    /*those views are of the dialog(signup screen)*/
    private EditText edMailSignup, edPassSignup, edRepeatPassSignup, edUsernameSignup;
    private Button btnSignup;

    /**to wait for response from firebase*/
    private ProgressDialog pd;

    private Snackbar snackbar;

    private SharedPreferences sp;

    /** we check if we can autologin, if yes do so, otherwise user has to login  */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        intent = new Intent(this, MapsActivity.class);

        //auto login
        if(mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
            startActivity(intent);
            finish();
        }

        ConstraintLayout loginLayout = findViewById(R.id.loginLayout);

        snackbar = Snackbar.make(loginLayout,"Activate your account by confirming the email we sent to you",Snackbar.LENGTH_LONG);
        snackbar.setDuration(BaseTransientBottomBar.LENGTH_INDEFINITE);
        snackbar.setAction("UNDERSTOOD", view -> { });

        edMail = findViewById(R.id.edMail);
        edPass = findViewById(R.id.edPass);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);

        btnLogin.setOnClickListener(this);
        btnCreateAccount.setOnClickListener(this);

        signupDialog = new Dialog(this);
        signupDialog.setContentView(R.layout.activity_signup);
        signupDialog.setTitle("my title");
        signupDialog.setCancelable(true);

        //get screen width, height
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        //set width and height of dialog relative to screen width and height
        signupDialog.getWindow().setLayout((int)(width / 1.3), (int)(height / 1.3));

        //get signup views
        edMailSignup = signupDialog.findViewById(R.id.edMailSignup);
        edPassSignup = signupDialog.findViewById(R.id.edPassSignup);
        edRepeatPassSignup = signupDialog.findViewById(R.id.edRepeatPassSignup);
        edUsernameSignup = signupDialog.findViewById(R.id.edUsername);
        btnSignup = signupDialog.findViewById(R.id.btnSignup);
        btnSignup.setOnClickListener(this);

        pd = new ProgressDialog(this);
        pd.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.parseColor("#04f1e9")));

        sp = getSharedPreferences("data", MODE_PRIVATE); //to save email
        String posMail = sp.getString("mail", "-");

        //if found saved mail load it
        if(!posMail.equals("-")) {
            edMail.setText(posMail);
        }
    }

    /** handles click on: btnLogin, btnCreateAccount, btnSignup */
    @Override
    public void onClick(View v) {
        if(v == btnLogin) {
            //check for empty fields
            boolean isOneEmpty = false;
            if(edMail.getText().toString().equals("")) {
                edMail.setError("Required field");
                isOneEmpty = true;
            }
            if(edPass.getText().toString().equals("")) {
                edPass.setError("Required field");
                isOneEmpty = true;
            }
            if(isOneEmpty) {
                return;
            }

            mAuth.signInWithEmailAndPassword(edMail.getText().toString(), edPass.getText().toString())
                    .addOnCompleteListener(this, task -> {

                        if(task.isSuccessful())
                        {
                            pd.dismiss();

                            //if this account exists check if he verified email
                            if(!FirebaseAuth.getInstance().getCurrentUser().isEmailVerified()) {
                                snackbar.show(); //if not remind him to verify
                            }
                            else {
                                //if email verified let him in
                                SharedPreferences.Editor editor=sp.edit();

                                //save current email login value for next login
                                editor.putString("mail", edMail.getText().toString());
                                editor.apply();

                                //move to main app
                                startActivity(intent);
                                finish();
                            }
                        }
                        else
                        {
                            //if login failed hide progDialog and check for errors
                            pd.dismiss();
                            try {
                                throw task.getException();
                            }
                            catch(FirebaseAuthInvalidUserException e) {
                                edMail.setError("account does not exists");
                            }
                            catch(FirebaseAuthInvalidCredentialsException e) {
                                edPass.setError("Wrong Password");
                            }
                            catch(Exception ignored) {

                            }
                            finally {
                                Toast.makeText(LoginActivity.this, "login failed",Toast.LENGTH_LONG).show();
                            }
                        }

                    });
            //here we told firebase to do some stuff but waiting for listener's response, show pd
            pd.setMessage("Logging in, Please Wait...");
            pd.show();
        }
        else if(v == btnCreateAccount) {
            signupDialog.show(); //show dialog to create new account
        }
        else if(v == btnSignup) { //user ready to create new account

            //check for empty fields
            boolean isOneEmpty = false;
            if(edMailSignup.getText().toString().equals("")) {
                edMailSignup.setError("Required field");
                isOneEmpty = true;
            }
            if(edPassSignup.getText().toString().equals("")) {
                edPassSignup.setError("Required field");
                isOneEmpty = true;
            }
            if(edRepeatPassSignup.getText().toString().equals("")) {
                edRepeatPassSignup.setError("Required field");
                isOneEmpty = true;
            }
            if(edUsernameSignup.getText().toString().equals("")) {
                edUsernameSignup.setError("Required field");
                isOneEmpty = true;
            }
            if(isOneEmpty) {
                return;
            }

            //check if password and repeated password match
            if(edPassSignup.getText().toString().equals(edRepeatPassSignup.getText().toString())) {
                signupDialog.dismiss(); //if yes hide dialog

                //and try create new user
                mAuth.createUserWithEmailAndPassword(edMailSignup.getText().toString(), edPassSignup.getText().toString())
                        .addOnCompleteListener(this, task -> {
                            if (task.isSuccessful()) {
                                // Sign in success, now send user email verification and ask him to verify his email
                                pd.dismiss();

                                //show message about email verification
                                snackbar.show();

                                //send verification
                                FirebaseAuth.getInstance().getCurrentUser().sendEmailVerification();

                                //set created user in firebase db
                                FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                                DatabaseReference userRef = firebaseDatabase.getReference().child("Users").push();
                                String uid = mAuth.getCurrentUser().getUid();
                                User user = new User(edUsernameSignup.getText().toString(), uid);
                                user.key = userRef.getKey();
                                userRef.setValue(user);
                            } else {
                                // If sign in fails, check and display why
                                pd.dismiss();

                                try{
                                    throw task.getException();
                                }
                                catch(FirebaseAuthWeakPasswordException e) {
                                    edPassSignup.setError("Weak Password");
                                    edRepeatPassSignup.setText("");
                                }
                                catch(FirebaseAuthInvalidCredentialsException e) {
                                    edMailSignup.setError("Not Existing Email");
                                }
                                catch(FirebaseAuthUserCollisionException e) {
                                    edMailSignup.setError("There is already an account with the given email address");
                                }
                                catch(Exception ignored) {

                                }
                                finally {
                                    signupDialog.show(); //if error show dialog again with error description
                                    Toast.makeText(LoginActivity.this, "Authentication failed",
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                            // ...
                        });
                pd.setMessage("Signing in, Please Wait..."); //wait for firebase to trigger the a=listener above
                pd.show();
            }
            else { //we get here if password doesn't match repeated password
                edPassSignup.setText("");
                edRepeatPassSignup.setText("");
                edRepeatPassSignup.setError("Passwords did not match");
            }
        }
    }
}