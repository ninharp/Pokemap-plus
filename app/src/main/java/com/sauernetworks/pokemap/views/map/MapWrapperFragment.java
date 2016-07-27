package com.sauernetworks.pokemap.views.map;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Location;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.sauernetworks.pokemap.R;
import com.sauernetworks.pokemap.controllers.map.LocationManager;
import com.sauernetworks.pokemap.models.map.PokemonMarkerExtended;
import com.sauernetworks.pokemap.models.events.CatchablePokemonEvent;
import com.sauernetworks.pokemap.models.events.SearchInPosition;
import com.sauernetworks.pokemap.views.MainActivity;
import com.pokegoapi.api.map.pokemon.CatchablePokemon;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A simple {@link Fragment} subclass.
 *
 * Use the {@link MapWrapperFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapWrapperFragment extends Fragment implements OnMapReadyCallback,
                                                            GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener,
                                                            ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 19;

    private LocationManager locationManager;

    public static Toast toast;

    FloatingActionMenu materialDesignFAM;
    com.github.clans.fab.FloatingActionButton floatingActionButtonSearch, floatingActionButtonMyLocation, floatingActionButtonAddFavorite, floatingActionButtonFavorites;

    private View mView;
    private SupportMapFragment mSupportMapFragment;
    private GoogleMap mGoogleMap;
    private Location mLocation = null;
    private Marker userSelectedPositionMarker = null;
    private Circle userSelectedPositionCircle = null;
    private MarkerOptions markerOptions;
    private LatLng latLng;
    private HashMap<String, PokemonMarkerExtended> markerList = new HashMap<>();
    private Map<String, Marker> favoritesList = new HashMap<>();
    private Map<String, String> pokemonNames = new HashMap<>();
    private boolean addFavorite = false;
    public MapWrapperFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment MapWrapperFragment.
     */
    public static MapWrapperFragment newInstance() {
        MapWrapperFragment fragment = new MapWrapperFragment();
        Bundle args = new Bundle();

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        populatePokemonNames(pokemonNames);
        //TODO: Load Favorites
        setRetainInstance(true);
    }

    @Override
    public void onStart(){
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePokemonMarkers();
    }

    public void gotoLocation() {
        if (mLocation != null && mGoogleMap != null) {
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));

            MainActivity.toast.setText(R.string.location_found);
            MainActivity.toast.show();
        }
        else{

            MainActivity.toast.setText(R.string.waiting_location);
            MainActivity.toast.show();
        }
    }

    public void searchMap(String search) {
        new GeocoderTask().execute(search);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        locationManager = LocationManager.getInstance(getContext());
        locationManager.register(new LocationManager.Listener() {
            @Override
            public void onLocationChanged(Location location) {
                if (mLocation == null) {
                    mLocation = location;
                    initMap();
                }
                else{
                    mLocation = location;
                }
            }
        });
        // Inflate the layout for this fragment if the view is not null
        if (mView == null) mView = inflater.inflate(R.layout.fragment_map_wrapper, container, false);
        else {

        }

        // build the map
        if (mSupportMapFragment == null) {
            mSupportMapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mSupportMapFragment).commit();
            mSupportMapFragment.setRetainInstance(true);
        }

        if (mGoogleMap == null) {
            mSupportMapFragment.getMapAsync(this);
        }

        mView.findViewById(R.id.closeSuggestions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mView.findViewById(R.id.layoutSuggestions).setVisibility(View.GONE);
            }
        });

        materialDesignFAM = (FloatingActionMenu) mView.findViewById(R.id.material_design_android_floating_action_menu);
        floatingActionButtonSearch = (com.github.clans.fab.FloatingActionButton) mView.findViewById(R.id.floating_action_menu_search_location);
        floatingActionButtonMyLocation = (com.github.clans.fab.FloatingActionButton) mView.findViewById(R.id.floating_action_menu_mylocation);
        floatingActionButtonAddFavorite = (com.github.clans.fab.FloatingActionButton) mView.findViewById(R.id.floating_action_menu_addfavorite);
        floatingActionButtonFavorites = (com.github.clans.fab.FloatingActionButton) mView.findViewById(R.id.floating_action_menu_favorites);

        floatingActionButtonSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                final EditText edittext = new EditText(getActivity());
                alert.setMessage(R.string.message_search);
                alert.setTitle(R.string.title_search);

                alert.setView(edittext);

                alert.setPositiveButton(R.string.button_search, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String searchString = edittext.getText().toString();
                        searchMap(searchString);
                    }
                });

                alert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // what ever you want to do with No option.
                    }
                });
                alert.show();
                materialDesignFAM.close(true);
            }
        });
        floatingActionButtonMyLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gotoLocation();
                materialDesignFAM.close(true);
            }
        });
        floatingActionButtonAddFavorite.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO
                addFavorite = true;
                materialDesignFAM.close(true);
            }
        });
        floatingActionButtonFavorites.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //TODO
                materialDesignFAM.close(true);
            }
        });

        return mView;
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("Pokemap", "onMapClick entered!");
        if (addFavorite) {
            final String[] favoriteName = {"Favorite"};
            markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getContext().getResources(),
                    R.drawable.ic_favorite_white_24dp)));


            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
            final EditText edittext = new EditText(getActivity());
            alert.setMessage(R.string.alert_text_favorit_add);
            alert.setTitle(R.string.alert_title_favorite_add);

            alert.setView(edittext);

            alert.setPositiveButton(R.string.button_search, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    favoriteName[0] = edittext.getText().toString();
                    markerOptions.title("Favorit");
                    Marker markerFavorite = mGoogleMap.addMarker(markerOptions);
                    favoritesList.put(favoriteName[0], markerFavorite);
                }
            });

            alert.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // what ever you want to do with No option.
                }
            });
            alert.show();
            addFavorite = false;
        }
    }

    // An AsyncTask class for accessing the GeoCoding Web Service
    private class GeocoderTask extends AsyncTask<String, Void, List<Address>> {

        @Override
        protected List<Address> doInBackground(String... locationName) {
            // Creating an instance of Geocoder class
            Geocoder geocoder = new Geocoder(getContext());
            List<Address> addresses = null;

            try {
                // Getting a maximum of 3 Address that matches the input text
                addresses = geocoder.getFromLocationName(locationName[0], 3);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return addresses;
        }

        @Override
        protected void onPostExecute(List<Address> addresses) {

            if(addresses==null || addresses.size()==0){
                Toast.makeText(getActivity(), R.string.no_location_found, Toast.LENGTH_SHORT).show();
            }

            // Clears all the existing markers on the map
            mGoogleMap.clear();

            // Adding Markers on Google Map for each matching address
            for(int i=0;i<addresses.size();i++){

                Address address = (Address) addresses.get(i);

                // Creating an instance of GeoPoint, to display in Google Map
                latLng = new LatLng(address.getLatitude(), address.getLongitude());

                String addressText = String.format("%s, %s",
                        address.getMaxAddressLineIndex() > 0 ? address.getAddressLine(0) : "",
                        address.getCountryName());

                /*
                markerOptions = new MarkerOptions();
                markerOptions.position(latLng);
                markerOptions.title(addressText);

                mGoogleMap.addMarker(markerOptions);
                */

                // Locate the first location
                if(i==0)
                    mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
            }
        }
    }

    private void initMap(){

        if (mLocation != null && mGoogleMap != null){
            if (ContextCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(mView.getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                MainActivity.toast.setText(R.string.location_permission_denied);
                MainActivity.toast.show();
                return;
            }
            mGoogleMap.setMyLocationEnabled(true);
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLocation.getLatitude(), mLocation.getLongitude()), 15));
            MainActivity.toast.setText(R.string.location_found_init);
            MainActivity.toast.show();
        }
    }

    private void updatePokemonMarkers() {
        if (mGoogleMap != null && markerList != null && !markerList.isEmpty()){
            for(Iterator<Map.Entry<String, PokemonMarkerExtended>> it = markerList.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, PokemonMarkerExtended> entry = it.next();
                CatchablePokemon catchablePokemon = entry.getValue().getCatchablePokemon();
                Marker marker = entry.getValue().getMarker();
                long millisLeft = catchablePokemon.getExpirationTimestampMs() - System.currentTimeMillis();
                if(millisLeft < 0) {
                    marker.remove();
                    it.remove();
                } else {
                    marker.setSnippet(getExpirationBreakdown(this.getContext(), millisLeft));
                    if(marker.isInfoWindowShown()) {
                        marker.showInfoWindow();
                    }
                }
            }

        }
    }

    private void populatePokemonNames(Map<String, String> pokeNames)
    {
        pokeNames.clear();
        try {
            Resources res = getResources();
            InputStream inputStream = res.openRawResource(R.raw.pokenames_de);
            InputStreamReader inputreader = new InputStreamReader(inputStream);
            BufferedReader buffreader = new BufferedReader(inputreader);
            String line;
            StringBuilder text = new StringBuilder();
            try {
                while (( line = buffreader.readLine()) != null) {
                    String[] names = line.split(";;");
                    //Log.d("Pokemon", names[0]+"-"+names[1]);
                    pokeNames.put(names[0], names[1]);
                }
            } catch (IOException e) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getPokemonName(Map<String, String> pokeNames, String original_name) {
            populatePokemonNames(pokemonNames);

            String name = pokeNames.get(original_name);
            if (name == null) {
                return original_name;
            } else {
                //Log.d("Pokemon Name:", name);
                return name;
            }
    }

    private void setPokemonMarkers(final List<CatchablePokemon> pokeList){
        if (mGoogleMap != null) {

            Set<String> markerKeys = markerList.keySet();
            int pokemonFound = 0;
            for (CatchablePokemon poke : pokeList) {

                if(!markerKeys.contains(poke.getSpawnPointId())) {
                    int resourceID = getResources().getIdentifier("p" + poke.getPokemonId().getNumber(), "drawable", getActivity().getPackageName());
                    Marker marker = mGoogleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(poke.getLatitude(), poke.getLongitude()))
                            .title(getPokemonName(pokemonNames, poke.getPokemonId().name()))
                            .icon(BitmapDescriptorFactory.fromResource(resourceID))
                            .anchor(0.5f, 0.5f));

                    //adding pokemons to list to be removed on next search
                    markerList.put(poke.getSpawnPointId(), new PokemonMarkerExtended(poke, marker));
                    pokemonFound++;
                }
            }

            MainActivity.toast.setText(pokemonFound > 0 ? pokemonFound + " " + this.getString(R.string.new_pokemon_found) : this.getString(R.string.no_pokemon_found));
            MainActivity.toast.show();
            updatePokemonMarkers();
        } else {
            MainActivity.toast.setText(R.string.map_not_initialized);
            MainActivity.toast.show();
        }
    }

    public static String getExpirationBreakdown(Context ctx, long millis) {
        if(millis < 0) {
            return ctx.getString(R.string.pokemon_expired);
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        String expires = ctx.getString(R.string.pokemon_expires_in);
        return(String.format(expires, minutes, seconds));
    }

    /**
     * Called whenever a CatchablePokemonEvent is posted to the bus. Posted when new catchable pokemon are found.
     *
     * @param event The event information
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(CatchablePokemonEvent event) {

        setPokemonMarkers(event.getCatchablePokemon());
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;

        UiSettings settings = mGoogleMap.getUiSettings();
        settings.setCompassEnabled(true);
        settings.setTiltGesturesEnabled(true);
        settings.setMyLocationButtonEnabled(false);
        //Handle long click
        mGoogleMap.setOnMapLongClickListener(this);
        mGoogleMap.setOnMapClickListener(this);
        //Disable for now coz is under FAB
        settings.setMapToolbarEnabled(false);
        initMap();
    }

    @Override
    public void onMapLongClick(LatLng position) {
        //Draw user position marker with circle
        drawMarkerWithCircle (position);

        //Sending event to MainActivity
        SearchInPosition sip = new SearchInPosition();
        sip.setPosition(position);
        EventBus.getDefault().post(sip);
    }

    public Bitmap bitmapSizeByScall( Bitmap bitmapIn, float scall_zero_to_one_f) {

        Bitmap bitmapOut = Bitmap.createScaledBitmap(bitmapIn,
                Math.round(bitmapIn.getWidth() * scall_zero_to_one_f),
                Math.round(bitmapIn.getHeight() * scall_zero_to_one_f), false);

        return bitmapOut;
    }

    private Bitmap scaleImage(Resources res, int id, int lessSideSize) {
        Bitmap b = null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;

        BitmapFactory.decodeResource(res, id, o);

        float sc = 0.0f;
        int scale = 1;
        // if image height is greater than width
        if (o.outHeight > o.outWidth) {
            sc = o.outHeight / lessSideSize;
            scale = Math.round(sc);
        }
        // if image width is greater than height
        else {
            sc = o.outWidth / lessSideSize;
            scale = Math.round(sc);
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        b = BitmapFactory.decodeResource(res, id, o2);
        return b;
    }

    private void drawMarkerWithCircle(LatLng position){
        //Check and eventually remove old marker
        if(userSelectedPositionMarker != null && userSelectedPositionCircle != null){
            userSelectedPositionMarker.remove();
            userSelectedPositionCircle.remove();
        }

        double radiusInMeters = 100.0;
        int strokeColor = 0xff3399FF; // outline
        int shadeColor = 0x4400CCFF; // fill

        CircleOptions circleOptions = new CircleOptions().center(position).radius(radiusInMeters).fillColor(shadeColor).strokeColor(strokeColor).strokeWidth(8);
        userSelectedPositionCircle = mGoogleMap.addCircle(circleOptions);

        userSelectedPositionMarker = mGoogleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(getContext().getString(R.string.text_position_picked))
                .icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory.decodeResource(getContext().getResources(),
                        R.drawable.ic_my_location_white_24dp)))
                .anchor(0.5f, 0.5f));
    }

}

