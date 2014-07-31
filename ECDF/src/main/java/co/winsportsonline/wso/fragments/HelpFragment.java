package co.winsportsonline.wso.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import co.winsportsonline.wso.R;

/**
 * Created by Franklin Cruz on 27-02-14.
 */
public class HelpFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_help, container, false);

        WebView webView = (WebView)rootView.findViewById(R.id.help_webview);
        webView.loadUrl("https://estadiocdf.cl/support");

        return rootView;
    }
}
