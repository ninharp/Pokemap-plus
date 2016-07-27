package com.sauernetworks.pokemap.views;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.model.LatLng;
import com.sauernetworks.pokemap.R;
import com.sauernetworks.pokemap.models.events.LoginEventResult;
import com.sauernetworks.pokemap.models.events.SearchInPosition;
import com.sauernetworks.pokemap.models.events.ServerUnreachableEvent;
import com.sauernetworks.pokemap.models.events.TokenExpiredEvent;
import com.sauernetworks.pokemap.views.dialog.AboutDialog;
import com.sauernetworks.pokemap.views.login.RequestCredentialsDialogFragment;
import com.sauernetworks.pokemap.controllers.map.LocationManager;
import com.sauernetworks.pokemap.views.map.MapWrapperFragment;
import com.sauernetworks.pokemap.views.settings.SettingsActivity;
import com.sauernetworks.pokemap.controllers.app_preferences.PokemapAppPreferences;
import com.sauernetworks.pokemap.controllers.app_preferences.PokemapSharedPreferences;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class MainActivity extends BaseActivity {
    private static final String TAG = "Pokemap";
    private static final String MAP_FRAGMENT_TAG = "MapFragment";

    private FragmentManager fragmentManager = getSupportFragmentManager();

    private PokemapAppPreferences pref;



    public static Toast toast;

    //region Lifecycle Methods
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = new PokemapSharedPreferences(this);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        MapWrapperFragment mapWrapperFragment = (MapWrapperFragment) fragmentManager.findFragmentByTag(MAP_FRAGMENT_TAG);
        if(mapWrapperFragment == null) {
            mapWrapperFragment = MapWrapperFragment.newInstance();
        }
        fragmentManager.beginTransaction().replace(R.id.main_container,mapWrapperFragment, MAP_FRAGMENT_TAG)
                .commit();



    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume(){
        super.onResume();
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    //region Menu Methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_relogin) {
            login();
        } else if (id == R.id.action_search) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            final EditText edittext = new EditText(this);
            alert.setMessage(R.string.message_search);
            alert.setTitle(R.string.title_search);

            alert.setView(edittext);

            alert.setPositiveButton(R.string.button_search, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String searchString = edittext.getText().toString();
                    MapWrapperFragment fragment = (MapWrapperFragment)fragmentManager.findFragmentById(R.id.main_container);
                    fragment.searchMap(searchString);
                }
            });

            alert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // what ever you want to do with No option.
                }
            });
            alert.show();
        } else if (id == R.id.action_about) {
            AboutDialog about = new AboutDialog(this);
            about.setTitle(R.string.dialog_title_about);
            about.show();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // TODO: test all this shit on a 6.0+ phone lmfao
        switch (requestCode) {
            case 703:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "permission granted");
                }
                break;
        }
    }

    private void login() {
        if (!pref.isUsernameSet() || !pref.isPasswordSet()) {
            requestLoginCredentials();
        } else {
            nianticManager.login(pref.getUsername(), pref.getPassword());
        }
    }

    private void requestLoginCredentials() {
        getSupportFragmentManager().beginTransaction().add(RequestCredentialsDialogFragment.newInstance(
                new RequestCredentialsDialogFragment.Listener() {
                    @Override
                    public void credentialsIntroduced(String username, String password) {
                        pref.setUsername(username);
                        pref.setPassword(password);
                        login();
                    }
                }), "request_credentials").commit();
    }

    /**
     * Called whenever a LoginEventResult is posted to the bus. Originates from LoginTask.java
     *
     * @param result Results of a log in attempt
     */
    @Subscribe
    public void onEvent(LoginEventResult result) {
        if (result.isLoggedIn()) {
            toast.setText(R.string.login_successfull);
            toast.show();
            LatLng latLng = LocationManager.getInstance(MainActivity.this).getLocation();
            nianticManager.getCatchablePokemon(latLng.latitude, latLng.longitude, 0D);
        } else {
            toast.cancel();
            toast.setText(R.string.login_failed);
            toast.show();
        }
    }

    /**
     * Called whenever a use whats to search pokemons on a different position
     *
     * @param event PoJo with LatLng obj
     */
    @Subscribe
    public void onEvent(SearchInPosition event) {
        toast.setText(R.string.searching_pokemon);
        toast.show();
        nianticManager.getCatchablePokemon(event.getPosition().latitude, event.getPosition().longitude, 0D);
    }

    /**
     * Called whenever a ServerUnreachableEvent is posted to the bus. Posted when the server cannot be reached
     *
     * @param event The event information
     */
    @Subscribe
    public void onEvent(ServerUnreachableEvent event) {
        toast.setText(R.string.unable_to_connect_server);
        toast.show();
    }

    /**
     * Called whenever a TokenExpiredEvent is posted to the bus. Posted when the token from the login expired.
     *
     * @param event The event information
     */
    @Subscribe
    public void onEvent(TokenExpiredEvent event) {
        toast.setText(R.string.login_expired);
        toast.show();
        login();
    }

}
