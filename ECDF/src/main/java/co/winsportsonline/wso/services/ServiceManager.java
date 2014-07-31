package co.winsportsonline.wso.services;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.androidquery.AQuery;
import com.androidquery.auth.FacebookHandle;
import com.androidquery.callback.AjaxCallback;
import com.androidquery.callback.AjaxStatus;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.DataMember;
import co.winsportsonline.wso.datamodel.DataModel;
import co.winsportsonline.wso.datamodel.LiveStream;
import co.winsportsonline.wso.datamodel.LiveStreamSchedule;
import co.winsportsonline.wso.datamodel.Media;
import co.winsportsonline.wso.datamodel.TokenIssue;
import co.winsportsonline.wso.datamodel.User;

/**
 * Created by Franklin Cruz on 06-03-14.
 */
public class ServiceManager {

    public static final String TAG = "ServiceManager";

    public static final String BASE_URL = "https://api.streammanager.co/api/";
    public static final String API_TOKEN = "b5430d55dc64849b1a34e877267e72ba";
    public static final String UPDATE_SERVICE = "http://190.215.44.18/cdf/UpdateService.svc/NeedUpdate/";
    public static final String CURRENT_VERSION = "1";

    public static final String ACCESS_URL_LOGIN = "https://www.winsportsonline.com/api/auth/oauth2/token";
    public static final String URL_LOGIN = "https://winsportsonline.com/api/account";
    public static final String GRANT_TYPE = "password";
    public static final String REFRESH_GRANT_TYPE = "refresh_token";

    public static final String ACCESS_URL_MEDIA = "https://winsportsonline.com/api/media";

    private Context context;
    private AQuery aq;

    private FacebookHandle fHandle = null;

    public ServiceManager(Context context) {
        this.context = context;
        aq = new AQuery(context);
    }

    public void saveUserData(User user) {
        try {
            SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
            prefs.edit().putString("userdata", encodeJsonObject(user).toString()).commit();
        }
        catch (Exception e) {
        }
    }

    public User loadUserData() {
        try {
            SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
            User u = parseJsonObject(new JSONObject(prefs.getString("userdata", null)), User.class);
        return u;
        }
        catch (Exception e) {
            return null;
        }
    }

    public void saveAccessData(String token_type, String access_token, String refresh_token){
        try {
            SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString("token_type", token_type);
            edit.putString("access_token", access_token);
            edit.putString("refresh_token", refresh_token);
            edit.commit();
        }catch (Exception e){

        }
    }

    public void checkForUpdates(final DataLoadedHandler<Boolean> loadedHandler) {
        String url = String.format("%s%s", UPDATE_SERVICE, CURRENT_VERSION);
        aq.ajax(url, String.class, new AjaxCallback<String>() {
            @Override
            public void callback(String url, String object, AjaxStatus status) {
                loadedHandler.loaded(object.equalsIgnoreCase("TRUE"));
            }
        });
    }

    public void loginFacebook(Activity activity, AjaxCallback<JSONObject> ajaxCallback){
        String PERMISSIONS = "";
        String url = "https://graph.facebook.com/me";
        String APP_ID_FACEBOOK = "238809549653178";
        fHandle = new FacebookHandle(activity, APP_ID_FACEBOOK, PERMISSIONS);
        aq.auth(fHandle).progress(R.layout.progress_dialog).ajax(url, JSONObject.class, ajaxCallback);
    }

    public void logoutFacebook(Activity activity){
        String PERMISSIONS = "";
        String APP_ID_FACEBOOK = "238809549653178";
        fHandle = new FacebookHandle(activity, APP_ID_FACEBOOK, PERMISSIONS);
        fHandle.unauth();
    }

    public void saveFacebookToken(AjaxCallback<JSONObject> ajaxCallback){
        String url = "https://winsportsonline.com/signin/auth-fb-api";
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("t", fHandle.getToken(context));
        aq.ajax(url, params, JSONObject.class, ajaxCallback);

        context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE).edit()
                .putString("facebook_token", fHandle.getToken(context))
                .commit();
    }

    public void loginStandard(final String username, final String password, final DataLoadedHandler<User> loadedHandler){

        LoginAccessTask loginAccessTask = new LoginAccessTask(GRANT_TYPE,username,password);
        loginAccessTask.callback(new OnTaskCompleted() {
            @Override
            public void onTaskCompleted(final JSONObject accessObj) {
                if(accessObj != null){
                    String accessToken = "";
                    try{
                        accessToken = accessObj.getString("access_token");
                    }catch(Exception e){
                        e.printStackTrace();
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                    String url = String.format("%s?access_token=%s", URL_LOGIN, accessToken);
                    aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>(){
                        @Override
                        public void callback(String url, JSONObject loginObj, AjaxStatus status) {
                            try{
                                if (!loginObj.isNull("data") && loginObj.getString("status").equals("OK")) {
                                    User u = parseJsonObject(loginObj.getJSONObject("data"),User.class);
                                    saveAccessData(accessObj.getString("token_type"),
                                            accessObj.getString("access_token"),
                                            accessObj.getString("refresh_token"));
                                    u.setUsername(username);
                                    u.setPassword(password);
                                    saveUserData(u);
                                    loadedHandler.loaded(u);
                                }else{
                                    loadedHandler.error("Ups! Contáctese con el administrador...");
                                }
                            }catch(Exception e){
                                e.printStackTrace();
                                Log.e(TAG, "Error: "+e.getMessage());
                                loadedHandler.error("Usuario y/o contraseña invalido.");
                            }
                        }
                    });
                }
            }
        });
        loginAccessTask.execute(ACCESS_URL_LOGIN);
    }

    public void reLogin(final OnTaskCompleted callback){
        SharedPreferences sp = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
        if(sp.getString("userdata", null) != null) {
            User u = loadUserData();
            LoginAccessTask loginAccessTask = new LoginAccessTask(GRANT_TYPE,u.getUsername(),u.getPassword());
            loginAccessTask.callback(new OnTaskCompleted() {
                @Override
                public void onTaskCompleted(final JSONObject accessObj) {
                    try{
                        saveAccessData(accessObj.getString("token_type"),
                                accessObj.getString("access_token"),
                                accessObj.getString("refresh_token"));
                    }catch(Exception e){
                        context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE).edit().clear().commit();
                        callback.onTaskCompleted(null);
                    }
                }
            });
            loginAccessTask.execute(ACCESS_URL_LOGIN);
        }else if(sp.getString("name", null) != null && sp.getString("id", null) != null){
            String url = "https://winsportsonline.com/signin/auth-fb-api";
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("t", sp.getString("facebook_token",""));
            aq.ajax(url, params, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject object, AjaxStatus status) {
                    try{
                        context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE).edit()
                                .putString("access_token", object.getJSONObject("data").getString("access_token"))
                                .commit();
                    }catch(Exception e){
                        context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE).edit().clear().commit();
                        callback.onTaskCompleted(null);
                    }
                }
            });
        }
    }

    public void loadLiveStreamList(final DataLoadedHandler<LiveStream> loadedHandler) {
        String url = String.format("%slive-stream?token=%s", BASE_URL, API_TOKEN);
        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {

            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try{
                    if (!object.isNull("data") && object.getString("status").equals("OK")) {
                        JSONArray list = object.getJSONArray("data");
                        List<LiveStream> streams = new ArrayList<LiveStream>();
                        for (int i = 0; i < list.length(); ++i) {
                            JSONObject raw = list.getJSONObject(i);
                            streams.add(parseJsonObject(raw, LiveStream.class));
                        }

                        loadedHandler.loaded(streams);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    public void loadLiveStreamSchedule(final LiveStream stream, final DataLoadedHandler<LiveStreamSchedule> loadedHandler) {

        String url = String.format("%slive-stream/%s/schedule?token=%s", BASE_URL, stream.getLiveStreamId(), API_TOKEN);
        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {

            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try{
                    if (!object.isNull("data") && object.getString("status").equals("OK")) {
                        JSONArray list = object.getJSONArray("data");
                        List<LiveStreamSchedule> schedule = new ArrayList<LiveStreamSchedule>();
                        for (int i = 0; i < list.length(); ++i) {
                            JSONObject raw = list.getJSONObject(i);
                            LiveStreamSchedule item = parseJsonObject(raw, LiveStreamSchedule.class);
                            item.setStream(stream);
                            schedule.add(item);
                        }

                        loadedHandler.loaded(schedule);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });

    }

    public void loadVODMedia(String[] categories, final DataLoadedHandler<Media> loadedHandler) {
        String url = String.format("%smedia?token=%s&limit=50", BASE_URL, API_TOKEN);

        if(categories.length > 0) {
            url += "&category_name=";
            for(int i = 0; i < categories.length; ++i) {
                url += categories[i];

                if (i < categories.length - 1) {
                    url += ",";
                }
            }
        }

        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try {
                    if (!object.isNull("data") && object.getString("status").equals("OK")) {
                        JSONArray list = object.getJSONArray("data");
                        List<Media> media = new ArrayList<Media>();
                        for (int i = 0; i < list.length(); ++i) {
                            JSONObject raw = list.getJSONObject(i);
                            Media item = parseJsonObject(raw, Media.class);
                            media.add(item);
                        }
                        loadedHandler.loaded(media);
                    }
                }catch (Exception e) {
                    Log.e(TAG, "Error on load");
                }
            }
        });
    }

    public void search(String query, final DataLoadedHandler<Media> loadedHandler){

        String url = String.format("%smedia?token=%s&limit=30&query=%s", BASE_URL, API_TOKEN,query);
        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {

            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try {
                    if (!object.isNull("data") && object.getString("status").equals("OK")) {
                        JSONArray list = object.getJSONArray("data");
                        List<Media> media = new ArrayList<Media>();
                        for (int i = 0; i < list.length(); ++i) {
                            JSONObject raw = list.getJSONObject(i);
                            Media item = parseJsonObject(raw, Media.class);
                            media.add(item);
                        }
                        loadedHandler.loaded(media);
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    public void loadVODMediaByCategoryId(String[] categories, final DataLoadedHandler<Media> loadedHandler) {
        String url = String.format("%smedia?token=%s&limit=100", BASE_URL, API_TOKEN);

        if(categories.length > 0) {
            url += "&category_id=";
            for(int i = 0; i < categories.length; ++i) {
                url += categories[i];

                if (i < categories.length - 1) {
                    url += ",";
                }
            }
        }

        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {

            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                try {
                    if (!object.isNull("data") && object.getString("status").equals("OK")) {
                        JSONArray list = object.getJSONArray("data");
                        List<Media> media = new ArrayList<Media>();
                        for (int i = 0; i < list.length(); ++i) {
                            JSONObject raw = list.getJSONObject(i);
                            Media item = parseJsonObject(raw, Media.class);
                            media.add(item);
                        }

                        loadedHandler.loaded(media);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        });
    }

    public Boolean checkToken(final int cont){
        try{
            SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
            String url = String.format("%s?grant_type=%s&refres_token=%s", URL_LOGIN, REFRESH_GRANT_TYPE, prefs.getString("refresh_token", null));
            final Boolean[] res = {false};
            aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>(){
                @Override
                public void callback(String url, JSONObject accessObj, AjaxStatus status) {
                    try{
                        saveAccessData(accessObj.getString("token_type"),
                                accessObj.getString("access_token"),
                                accessObj.getString("refresh_token"));
                        res[0] = true;
                    }catch(Exception e){
                        if(cont==1){
                            res[0] = false;
                        }else{
                            res[0] = checkToken(1);
                        }
                    }
                }
            }.header("X-Requested-With","XMLHttpRequest"));
            return res[0];
        }catch(Exception e){
            Log.e(TAG,"CheckToken: "+e.getMessage());
        }
        return false;
    }

    public void issueTokenForMedia(final String mediaId, final DataLoadedHandler<TokenIssue> handler){
        SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
        String url = String.format("%s/%s?access_token=%s", ACCESS_URL_MEDIA, mediaId, prefs.getString("access_token", null));
        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject res, AjaxStatus ajaxStatus) {
                if(checkToken(0)){
                    issueTokenForMedia(mediaId, handler);
                    return;
                }
                String status;
                if(ajaxStatus.getCode()==200) {
                    status = "OK";
                }else{
                    Log.e(TAG,"IssueTokenForMedia: code "+ajaxStatus.getCode()+" error "+ajaxStatus.getError());
                    status = "CONTENIDO NO DISPONIBLE EN TU CUENTA.";
                }
                if(status.equalsIgnoreCase("OK")){
                    url = String.format("%saccess/issue?type=media&token=%s&id=%s", BASE_URL, API_TOKEN, mediaId);

                    aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {

                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus ajaxStatus) {
                            try {
                                TokenIssue tokenIssue = parseJsonObject(object, TokenIssue.class);
                                handler.loaded(tokenIssue);
                            } catch (Exception exception) {
                                TokenIssue tokenIssue = new TokenIssue();
                                tokenIssue.setStatus("Ups! Intenta de nuevo.");
                                handler.loaded(tokenIssue);
                            }
                        }
                    });
                }else if(status.equalsIgnoreCase("ERROR")){
                    TokenIssue tokenIssue = new TokenIssue();
                    tokenIssue.setStatus("ERROR");
                    handler.loaded(tokenIssue);
                }else{
                    TokenIssue tokenIssue = new TokenIssue();
                    tokenIssue.setStatus(status);
                    handler.loaded(tokenIssue);
                }
            }
        }.header("X-Requested-With","XMLHttpRequest"));
    }

    public void issueTokenForLive(final String mediaId,final int index, final DataLoadedHandler<TokenIssue> handler) {
        String url;
        SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
        if(index == 1){
            url = String.format("http://winsportsonline.com/api/la?access_token=%s",prefs.getString("access_token", null));
        }else{
            url = String.format("http://winsportsonline.com/api/la/2?access_token=%s",prefs.getString("access_token", null));
        }
        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                if(checkToken(0)){
                    issueTokenForLive(mediaId, index, handler);
                    return;
                }
                String res;
                try{
                    res = object.getString("s");
                }catch(Exception e){
                    res = "e";
                }
                if(status.getCode() == 200 && res.equalsIgnoreCase("o")){
                    url = String.format("%saccess/issue?type=live&token=%s&id=%s", BASE_URL, API_TOKEN, mediaId);
                    aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>() {
                        @Override
                        public void callback(String url, JSONObject object, AjaxStatus status) {
                            try {
                                TokenIssue tokenIssue = parseJsonObject(object, TokenIssue.class);
                                handler.loaded(tokenIssue);
                            }
                            catch (Exception exception) {
                                handler.error("Ups! Intenta de nuevo.");
                            }
                        }
                    });
                }else{
                    Log.e(TAG,"IssueTokenForLive: code "+status.getCode()+" error "+status.getError());
                    handler.error("CONTENIDO NO DISPONIBLE EN TU CUENTA.");
                }
            }
        }.header("X-Requested-With","XMLHttpRequest"));
    }

    public void geoData() {
        String url = "https://freegeoip.net/json/";

        aq.ajax(url, JSONObject.class, new AjaxCallback<JSONObject>(){
            @Override
            public void callback(String url, JSONObject object, AjaxStatus status) {
                SharedPreferences prefs = context.getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                try {
                    prefs.edit().putString("country_code", object.getString("country_code")).commit();
                    Log.e(TAG, object.getString("country_code"));
                }catch (Exception e) {
                    prefs.edit().clear().commit();
                    e.printStackTrace();
                    Log.e(TAG, "Error: "+e.getMessage());
                }
            }
        });
    }

    public static <T> JSONObject encodeJsonObject(T obj)
            throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, JSONException {

        if(obj == null)
            return null;

        JSONObject result = new JSONObject();

        for (Method method : obj.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("get")) {
                String variableName = method.getAnnotation(DataMember.class)
                        .member();

                if(method.getReturnType() == Date.class) {
                    Date value = (Date)method.invoke(obj);

                    if(value != null) {
                        String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
                        DateFormat df = new SimpleDateFormat(format, Locale.ENGLISH);
                        result.put(variableName, df.format(value));
                    }
                    else {
                        result.put(variableName, value);
                    }
                }
                else if(DataModel.class.isAssignableFrom(method.getReturnType())) {
                    Object value = method.invoke(obj);
                    result.put(variableName, encodeJsonObject(value));
                }
                else {
                    Object value = method.invoke(obj);
                    result.put(variableName, value);
                }
            }
        }
        return result;
    }


    public static <T> T parseJsonObject(JSONObject jsonObj, Class<T> type)
            throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, JSONException,
            IOException,ParseException {

        T result = type.newInstance();

        for (Method method : type.getMethods()) {
            if (method.getName().startsWith("set")) {
                DataMember dataMember = method.getAnnotation(DataMember.class);

                if (dataMember == null) {
                    continue;
                }

                String variableName = dataMember.member();
                Object value = jsonObj.isNull(variableName) ? null : jsonObj
                        .get(variableName);

                if (value != null) {
                    @SuppressWarnings("rawtypes")
                    Class[] params = method.getParameterTypes();
                    if (params[0] == String.class) {
                        method.invoke(result, value.toString());
                    } else if(params[0] == Date.class) {
                        String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
                        DateFormat df = new SimpleDateFormat(format);
                        df.setTimeZone(TimeZone.getTimeZone("GMT"));
                        method.invoke(result,df.parse(value.toString()));
                    } else if(java.util.List.class.isAssignableFrom(params[0])){

                        Class<?> genericType = (Class<?>)((ParameterizedType)method.getGenericParameterTypes()[0]).getActualTypeArguments()[0];

                        if (DataModel.class.isAssignableFrom(genericType)) {
                            List untypedList = new ArrayList();
                            JSONArray array = (JSONArray)value;
                            for (int i = 0; i < array.length(); ++i) {
                                untypedList.add(parseJsonObject(array.getJSONObject(i), genericType));
                            }
                            method.invoke(result, untypedList);
                        }
                        else {
                            try {
                                List untypedList = new ArrayList();
                                JSONArray array = (JSONArray)value;
                                for (int i = 0; i < array.length(); ++i) {
                                    untypedList.add(array.get(i));
                                }
                                method.invoke(result, untypedList);
                            }
                            catch (Exception e) {
                                Log.d(TAG, "Attempt to parse a weird type from JSON: " + e.getMessage());
                            }
                        }

                    }else if(DataModel.class.isAssignableFrom(params[0])) {
                        method.invoke(result, parseJsonObject((JSONObject)value, params[0]));
                    }else {
                        method.invoke(result, value);
                    }
                }
            }
        }

        return result;
    }

    public static abstract class DataLoadedHandler<T> {

        public void loaded(T data) {

        }

        public void loaded(List<T> data) {

        }

        public void loaded(HashMap<String, List<T>> data) {

        }

        public void error(String error) {

        }
    }

    class LoginAccessTask extends AsyncTask<String, Void, Void> {

        private String grandType;
        private String username;
        private String password;
        private OnTaskCompleted callback = null;

        public LoginAccessTask(String grandType, String username, String password){
            this.grandType = grandType;
            this.username = username;
            this.password = password;
        }

        public void callback(OnTaskCompleted callback){
            this.callback = callback;
        }

        @Override
        protected Void doInBackground(String... urls)  {
            JSONObject jsonObject = null;
            String url = urls[0];
            HttpClient cliente = new DefaultHttpClient();
            HttpPost postHttp = new HttpPost(url);

            ArrayList<NameValuePair> datos = new ArrayList<NameValuePair>();
            datos.add(new BasicNameValuePair("grant_type", grandType));
            datos.add(new BasicNameValuePair("username", username));
            datos.add(new BasicNameValuePair("password", password));
            UrlEncodedFormEntity parametrosEncriptados = null;
            try {
                parametrosEncriptados = new UrlEncodedFormEntity(datos);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("LoginAccessTask", ""+e.getMessage());
            }
            postHttp.setEntity(parametrosEncriptados);
            postHttp.setHeader("Authorization", "Basic VEVTVDo=");
            postHttp.setHeader("X-Requested-With","XMLHttpRequest");

            try{
                HttpResponse resHttp = cliente.execute(postHttp);

                BufferedReader lector = new BufferedReader(new InputStreamReader(resHttp.getEntity().getContent()));
                StringBuilder respuesta = new StringBuilder();
                String separador = System.getProperty("line.separator");
                String fila;

                while ((fila = lector.readLine()) != null) {
                    respuesta.append(fila + separador);
                }
                lector.close();
                jsonObject = new JSONObject(respuesta.toString());
            }catch (Exception e){
                e.printStackTrace();
                Log.e("LoginAccessTask", ""+e.getMessage());
            }
            if(callback != null)
                callback.onTaskCompleted(jsonObject);
            return null;
        }
    }

    public interface OnTaskCompleted{
        void onTaskCompleted(JSONObject jsonObject);
    }

}
