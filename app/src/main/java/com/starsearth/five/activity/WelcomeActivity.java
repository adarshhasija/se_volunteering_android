package com.starsearth.five.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.starsearth.five.BuildConfig;
import com.starsearth.five.R;
import com.starsearth.five.activity.auth.AddEditPhoneNumberActivity;
import com.starsearth.five.activity.auth.LoginActivity;
import com.starsearth.five.application.StarsEarthApplication;
import com.starsearth.five.domain.SEOneListItem;

import java.util.HashMap;
import java.util.Map;

public class WelcomeActivity extends AppCompatActivity {

    public static int LOGIN_WITH_PHONE_NUMBER_REQUEST = 0;
    public static int LOGIN_WITH_EMAIL_ADDRESS_REQUEST = 1;
    public static int PUBLIC_SEARCH = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_one);
        getSupportActionBar().hide();

        ImageView btnSearch = (ImageView) findViewById(R.id.ivSearch);
        Button btnLoginPhone = (Button) findViewById(R.id.btn_login_phone);
        Button btnLoginEmail = (Button) findViewById(R.id.btn_login_email);
        Button btnKeyboard = (Button) findViewById(R.id.btn_keyboard);
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("action", SEOneListItem.Type.EDUCATOR_SEARCH.toString());
                intent.putExtras(bundle);
                startActivityForResult(intent, PUBLIC_SEARCH);
            }
        });
        btnLoginPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              Intent intent = new Intent(WelcomeActivity.this, AddEditPhoneNumberActivity.class);
              startActivityForResult(intent, LOGIN_WITH_PHONE_NUMBER_REQUEST);
            }
        });
        btnLoginEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivityForResult(intent, LOGIN_WITH_EMAIL_ADDRESS_REQUEST);
            }
        });
        if (BuildConfig.DEBUG) {
            btnLoginEmail.setVisibility(View.VISIBLE);
        }
        else {
            btnLoginEmail.setVisibility(View.GONE);
        }
        if (btnKeyboard != null) {
            btnKeyboard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(WelcomeActivity.this, KeyboardActivity.class);
                    startActivity(intent);
                }
            });
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            redirectToMainMenu();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == LOGIN_WITH_PHONE_NUMBER_REQUEST) {
                Toast.makeText(getApplicationContext(), R.string.phone_number_verified, Toast.LENGTH_LONG).show();
                redirectToMainMenu();
            }
            else if (requestCode == LOGIN_WITH_EMAIL_ADDRESS_REQUEST) {
                Toast.makeText(getApplicationContext(), R.string.email_address_verified, Toast.LENGTH_LONG).show();
                redirectToMainMenu();
            }
            else if (requestCode == PUBLIC_SEARCH) {
                //User logged in during the flow of the app. This screen is not needed anymore
                Bundle extras = data.getExtras();
                boolean containsKey = extras.containsKey("isLoggedIn");
                if (containsKey && extras.getBoolean("isLoggedIn") == true) {
                    finish();
                }
            }
        }

    }

    private void updateUserProperties() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            ((StarsEarthApplication) getApplication()).getAnalyticsManager().updateUserAnalyticsInfo(currentUser.getUid());
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child("users").child(currentUser.getUid()).child("se_five").setValue(true);
            mDatabase.child("organizations").child("authorized_people").child(currentUser.getPhoneNumber()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    HashMap<String, Object> map = (HashMap<String, Object>) dataSnapshot.getValue();
                    if (map != null) {
                        for (Map.Entry<String,Object> entry : map.entrySet()) {
                            mDatabase.child("users").child(currentUser.getUid()).child("volunteer_organization").setValue(entry.getKey().toUpperCase());
                        }
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d("WELCOME_ACTIV", "**********UPDATE USER VOLUNTEER ORG FAILED************");
                }
            });
        }

    }

    private void redirectToMainMenu() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                updateUserProperties();
            }
        }).start();
        Intent intent = new Intent(WelcomeActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
