package co.winsportsonline.wso.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.activities.LoginActivity;
import co.winsportsonline.wso.datamodel.Filter;
import co.winsportsonline.wso.datamodel.Media;
import co.winsportsonline.wso.datamodel.User;
import co.winsportsonline.wso.delegates.SlideMenuDelegate;
import co.winsportsonline.wso.services.ServiceManager;

/**
 * Created by Franklin Cruz on 18-02-14.
 */
public class SlideMenu extends Fragment {

    private View rootView;
    private SlideMenuDelegate delegate;
    private Button enVivo,ligaPostobon, copaPostobon, torneoPostobon, finales, conexionDeportes, acceso;
    private TextView futbolVOD,programasVOD, cerrar;
    private EditText searchText;
    private ImageButton search;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.slide_menu, container, false);

        futbolVOD = (TextView) rootView.findViewById(R.id.menu_futbol_vod);
        programasVOD = (TextView) rootView.findViewById(R.id.menu_programas);
        cerrar = (TextView) rootView.findViewById(R.id.menu_session);

        enVivo = (Button) rootView.findViewById(R.id.menu_en_vivo);

        ligaPostobon = (Button) rootView.findViewById(R.id.menu_liga);
        copaPostobon = (Button) rootView.findViewById(R.id.menu_copa);
        torneoPostobon = (Button) rootView.findViewById(R.id.menu_torneo);
        finales = (Button) rootView.findViewById(R.id.menu_final);

        conexionDeportes = (Button) rootView.findViewById(R.id.menu_conexion);
        acceso = (Button) rootView.findViewById(R.id.menu_acceso);

        searchText = (EditText) rootView.findViewById(R.id.menu_search);

        search = (ImageButton) rootView.findViewById(R.id.menu_search_button);

        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        futbolVOD.setTypeface(light);
        programasVOD.setTypeface(light);
        enVivo.setTypeface(light);
        ligaPostobon.setTypeface(light);
        copaPostobon.setTypeface(light);
        torneoPostobon.setTypeface(light);
        finales.setTypeface(light);
        conexionDeportes.setTypeface(light);
        acceso.setTypeface(light);
        cerrar.setTypeface(light);
        searchText.setTypeface(light);

        assert rootView != null;

        enVivo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (delegate != null) delegate.onLiveSelected(SlideMenu.this);
            }
        });

        search.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if( searchText.getText().length()==0){
                   Toast.makeText(getActivity(), "Por favor ingrese una palabra", Toast.LENGTH_LONG).show();
                }
                else{
                    final ProgressDialog progress = new ProgressDialog(getActivity());
                    progress.show();
                    progress.setContentView(R.layout.progress_dialog);
                    progress.setCancelable(false);
                    progress.setCanceledOnTouchOutside(false);
                    ServiceManager serviceManager = new ServiceManager(getActivity());
                    serviceManager.search(searchText.getText().toString(), new ServiceManager.DataLoadedHandler<Media>() {
                        @Override
                        public void loaded(List<Media> media) {
                            if (delegate != null)
                                delegate.onSearchSelected(SlideMenu.this, media, progress);
                        }

                        @Override
                        public void error(String error) {
                            super.error(error);
                        }
                    });
                }
            }
        });

        ligaPostobon.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                Filter filter = new Filter();
                filter.setName("Liga Postobón");
                ArrayList<String> l = new ArrayList<String>();
                l.add("52659318a5f7789432000009");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);

            }
        });

        torneoPostobon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Filter filter = new Filter();
                filter.setName("Torneo Postobón");
                ArrayList<String> l = new ArrayList<String>();
                l.add("52659337a5f778943200000b");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);
            }
        });

        copaPostobon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Filter filter = new Filter();
                filter.setName("Copa Postobón");
                ArrayList<String> l = new ArrayList<String>();
                l.add("52659326a5f778943200000a");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);
            }
        });

        finales.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Filter filter = new Filter();
                filter.setName("Finales");
                ArrayList<String> l = new ArrayList<String>();
                l.add("52f522c37239b47b2d000012");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);
            }
        });

        conexionDeportes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Filter filter = new Filter();
                filter.setName("Conexión Deportes");
                ArrayList<String> l = new ArrayList<String>();
                l.add("52659372a5f778943200000f");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);
            }
        });

        acceso.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Filter filter = new Filter();
                filter.setName("Acceso");
                ArrayList<String> l = new ArrayList<String>();
                l.add("5265935ba5f778943200000d");
                filter.setCategories(l);

                if (delegate != null) delegate.onFilterSelected(SlideMenu.this, filter);
            }
        });

        final AQuery aq = new AQuery(getActivity().getApplicationContext());

        final SharedPreferences prefs = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
        ServiceManager sm = new ServiceManager(getActivity());
        User u = sm.loadUserData();

        String id;
        String name;
        if(u != null){
            try{
                id = u.getSocial().getFacebook().getId();
            }catch(Exception e){
                id = null;
            }
            name = u.getFirstName()+" "+u.getLastName();
        }else{
            id = prefs.getString("id", "");
            name = prefs.getString("name", "");

            sm.loginFacebook(getActivity(), new AjaxCallback<JSONObject>() {
                @Override
                public void callback(String url, JSONObject user, AjaxStatus status) {
                    try {
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("id", user.getString("id"));
                        editor.putString("name", user.getString("name"));
                        editor.commit();
                    } catch (Exception e) {
                        prefs.edit().clear().commit();
                        Toast.makeText(getActivity(), "Se cerro la sesión de facebook.", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
        try{
            TextView userLabel = (TextView)rootView.findViewById(R.id.username_label);
            userLabel.setTypeface(light);
            userLabel.setText(name);

            TextView cerrar = (TextView)rootView.findViewById(R.id.cerrar_session);
            cerrar.setTypeface(light);
            if(id != null && id != ""){
                ImageView img = (ImageView)rootView.findViewById(R.id.profile_picture);
                (new ProfileImageTask(aq,img)).execute("https://graph.facebook.com/"+id+"/picture");
            }

            cerrar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    prefs.edit().clear().commit();
                    ServiceManager sm = new ServiceManager(getActivity());
                    sm.logoutFacebook(getActivity());
                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            });

        }catch(Exception e){
            e.printStackTrace();
            Log.e("exception",e.getMessage());
        }

        this.rootView = rootView;

        return rootView;
    }

    @Override
    public View getView() {
        return rootView;
    }

    public SlideMenuDelegate getDelegate() {
        return delegate;
    }

    public void setDelegate(SlideMenuDelegate delegate) {
        this.delegate = delegate;
    }

    class ProfileImageTask extends AsyncTask<String, Void, Bitmap>{

        AQuery aq = null;
        ImageView img = null;

        public ProfileImageTask(AQuery aq, ImageView img){
            this.aq = aq;
            this.img = img;
        }

        @Override
        protected Bitmap doInBackground(String... urls) {
            String url = urls[0];
            try{
                URL img_value = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) img_value.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();

                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                // this is storage overwritten on each iteration with bytes
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                // we need to know how may bytes were read to write them to the byteBuffer
                int len = 0;
                while ((len = input.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                // and then we can return your byte array.
                byte[] x = byteBuffer.toByteArray();

                Bitmap bitmap = BitmapFactory.decodeByteArray(x,0,x.length);
                return bitmap;
            }catch (Exception e){
                e.printStackTrace();
                Log.e("ProfileImageTask","Error: "+e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap){
            img.setImageBitmap(bitmap);
        }
    }
}
