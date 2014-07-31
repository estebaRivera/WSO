package co.winsportsonline.wso.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.json.JSONObject;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.activities.MainActivity;
import co.winsportsonline.wso.datamodel.User;
import co.winsportsonline.wso.services.ServiceManager;


/**
 * Created by Franklin Cruz on 17-02-14.
 */
public class LoginFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

        SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
        if((sp.getString("userdata", null) != null || (sp.getString("name", null) != null && sp.getString("id", null) != null)) && sp.getString("country_code", null) != null) {
            ServiceManager serviceManager = new ServiceManager(getActivity().getApplicationContext());
            serviceManager.geoData();
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }

        final ProgressDialog progress = new ProgressDialog(getActivity());

        View rootView = inflater.inflate(R.layout.fragment_login, container, false);

        Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Bold.otf");
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        TextView subtitulo = (TextView) rootView.findViewById(R.id.login_subtitulo);
        TextView title = (TextView) rootView.findViewById(R.id.login_referencia);

        final EditText user = (EditText) rootView.findViewById(R.id.login_user);
        final EditText pass = (EditText) rootView.findViewById(R.id.login_pass);

        user.setTypeface(light);
        pass.setTypeface(light);
        subtitulo.setTypeface(bold);
        title.setTypeface(light);

        InputFilter filter = new InputFilter(){
            @Override
            public CharSequence filter(CharSequence charSequence, int start, int end, Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (Character.isSpaceChar(charSequence.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }
        };

        user.setFilters(new InputFilter[]{filter});
        pass.setFilters(new InputFilter[]{filter});

        final ServiceManager serviceManager = new ServiceManager(getActivity());

        Button loginButtonStandard = (Button)rootView.findViewById(R.id.loginButton_2);
        loginButtonStandard.setTypeface(light);
        loginButtonStandard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.show();
                progress.setContentView(R.layout.progress_dialog);
                progress.setCancelable(false);
                progress.setCanceledOnTouchOutside(false);

               if(user.getText().toString().trim().length() > 0 && pass.getText().toString().length() > 0) {
                   try{
                        serviceManager.loginStandard(user.getText().toString(), pass.getText().toString(), new ServiceManager.DataLoadedHandler<User>() {
                            @Override
                            public void loaded(User data) {
                                serviceManager.geoData();
                                progress.dismiss();
                                Intent intent = new Intent(getActivity(), MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                            }

                            @Override
                            public void error(String error) {
                                Toast.makeText(getActivity().getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                                progress.dismiss();
                            }
                        });
                   }catch(Exception e){
                       e.printStackTrace();
                       Log.e("ServiceManager","Connection error: "+e.getMessage());
                       Toast.makeText(getActivity().getApplicationContext(),"Ups! Intenta de nuevo...",Toast.LENGTH_SHORT).show();
                       progress.dismiss();
                   }
               }else{
                   Toast.makeText(getActivity().getApplicationContext(),"Usuario y/o contraseña invalido.",Toast.LENGTH_SHORT).show();
                   progress.dismiss();
               }
            }
        });

        Button loginButtonFacebook = (Button)rootView.findViewById(R.id.loginButton);
        loginButtonFacebook.setTypeface(light);
        loginButtonFacebook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                progress.show();
                progress.setContentView(R.layout.progress_dialog);
                progress.setCancelable(false);
                progress.setCanceledOnTouchOutside(false);
                serviceManager.loginFacebook(getActivity(), new AjaxCallback<JSONObject>(){
                    @Override
                    public void callback(String url, JSONObject user, AjaxStatus status) {
                        try{
                            SharedPreferences prefs = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                            final SharedPreferences.Editor editor = prefs.edit();
                            editor.putString("id", user.getString("id"));
                            editor.putString("name", user.getString("name"));
                            serviceManager.geoData();
                            serviceManager.saveFacebookToken(new AjaxCallback<JSONObject>() {
                                @Override
                                public void callback(String url, JSONObject object, AjaxStatus status) {
                                    try{
                                        editor.putString("access_token", object.getJSONObject("data").getString("access_token"));
                                        editor.commit();
                                        progress.dismiss();
                                        Intent intent = new Intent(getActivity(), MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                        startActivity(intent);
                                    }catch(Exception e){
                                        e.getStackTrace();
                                        Log.e("loginFacebook",e.getMessage());
                                        Toast.makeText(getActivity(),"Ocurrió un error al iniciar sesión con facebook.",Toast.LENGTH_LONG).show();
                                        progress.dismiss();
                                    }
                                }
                            });
                        }catch(Exception e){
                            progress.dismiss();
                        }
                    }
                });
            }
        });

        return rootView;
    }
}
