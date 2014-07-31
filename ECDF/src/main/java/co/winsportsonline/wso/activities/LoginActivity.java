package co.winsportsonline.wso.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.fragments.LoginFragment;

public class LoginActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new LoginFragment())
                    .commit();
        }
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }
}
