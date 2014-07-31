package co.winsportsonline.wso.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.Media;
import co.winsportsonline.wso.datamodel.Thumbnail;
import co.winsportsonline.wso.delegates.VideoDelegate;
import co.winsportsonline.wso.dialogs.MessageDialog;
import co.winsportsonline.wso.dialogs.PostDialog;
import co.winsportsonline.wso.services.ServiceManager;

/**
 * Created by Franklin Cruz on 26-02-14.
 */
public class VodFragment extends Fragment {

    public final String VOD_CATEGORY_LAST_PROGRAM   = "Programas";
    public final String VOD_CATEGORY_LAST_MATCHES   = "Fútbol";

    private LinearLayout lastProgramsContainer;
    private LinearLayout lastMatchesContainer;

    private List<Media> lastMatchesList = null;
    private List<Media> lastShowsList = null;

    private View prevShow = null;
    private View prevShare = null;
    private View rootView;

    private VideoDelegate videoDelegate;

    private boolean matchesLoaded = false;
    private boolean showsLoaded = false;

    public void setVideoDelegate(VideoDelegate delegate) {
        this.videoDelegate = delegate;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_vod, container, false);

        lastProgramsContainer = (LinearLayout)rootView.findViewById(R.id.last_programs_container);
        lastMatchesContainer = (LinearLayout)rootView.findViewById(R.id.last_matches_container);

        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        TextView programas = (TextView) rootView.findViewById(R.id.last_programs_title_label);
        TextView partidos = (TextView) rootView.findViewById(R.id.last_matches_title_label);

        programas.setTypeface(light);
        partidos.setTypeface(light);

        matchesLoaded = false;
        showsLoaded = false;

        final ProgressDialog progress = new ProgressDialog(getActivity());
        progress.show();
        progress.setContentView(R.layout.progress_dialog);
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);

        ServiceManager serviceManager = new ServiceManager(getActivity());

        serviceManager.loadVODMedia( new String[] { VOD_CATEGORY_LAST_MATCHES }, new ServiceManager.DataLoadedHandler<Media>() {
            @Override
            public void loaded(List<Media> data) {
                lastMatchesList = data;

                displayLastMatches();

                matchesLoaded = true;
                if(showsLoaded) {
                    progress.dismiss();
                }
            }
        });

        serviceManager.loadVODMedia( new String[] { VOD_CATEGORY_LAST_PROGRAM }, new ServiceManager.DataLoadedHandler<Media>() {
            @Override
            public void loaded(List<Media> data) {
                lastShowsList = data;

                displayLastPrograms();

                showsLoaded = true;
                if( matchesLoaded) {
                    progress.dismiss();
                }
            }
        });
        return rootView;
    }

    private void displayLastPrograms() {

        for (final Media m  : lastShowsList) {

            View v = getActivity().getLayoutInflater().inflate(R.layout.last_programs_cell, null);
            lastProgramsContainer.addView(v);

            Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

            TextView titleLabel = (TextView)v.findViewById(R.id.title_label);
            titleLabel.setText(m.getTitle().replace(" - ", "\n"));
            titleLabel.setTypeface(light);

            TextView timeLabel = (TextView)v.findViewById(R.id.time_label);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            String date = "";
            try{
                date = df.format(m.getDateRecorded()) + " - ";
            }catch(Exception e){
                //NOTHING
            }
            timeLabel.setText(String.format("%s%d Min.", date, m.getDuration() / 60));
            timeLabel.setTypeface(light);

            TextView te = (TextView) v.findViewById(R.id.share_text);
            te.setTypeface(light);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)v.getLayoutParams();
            if(params!=null){
                params.setMargins(5,0,5,5);
                params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                params.width = 400;
            }

            v.setLayoutParams(params);

            ImageButton shareButton = (ImageButton)v.findViewById(R.id.share_button);

            final View show = v.findViewById(R.id.show_container);
            final View share = v.findViewById(R.id.share_container);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(prevShare != null && prevShow != null) {
                        hideShare(prevShare,prevShow);
                    }

                    displayShare(share,show);
                    prevShare = share;
                    prevShow = show;
                }
            });

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideShare(share,show);
                    prevShare = null;
                    prevShow = null;
                }
            });

            show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoDelegate != null) {

                        SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                        boolean permission = false;
                        List<String> countries = null;
                        try{
                            countries = m.getAccessRules().getGeo().getCountries();
                        }catch(Exception e){
                            e.printStackTrace();
                            Log.e("COUNTRIES","Error: "+e.getMessage());
                        }
                        if(countries != null && countries.size() > 0){
                            for(String country : countries){
                                if(country.equalsIgnoreCase(sp.getString("country_code",""))){
                                    permission = true;
                                }
                            }
                        }else{
                            permission = true;
                        }
                        if(permission){
                            videoDelegate.onVideoSelected(m);
                        }else{
                            MessageDialog messageDialog = new MessageDialog("ESTE CONTENIDO NO ESTÁ DISPONIBLE EN TU UBICACIÓN.");
                            messageDialog.show(getActivity().getFragmentManager(),"dialog");
                        }
                    }
                }
            });

            final AQuery aq = new AQuery(v);
            Thumbnail t = m.getDefaultThumbnail();
            final String thumbnailUrl;
            if (t != null) {

                thumbnailUrl = t.getUrl();

                View imageFullShare = v.findViewById(R.id.share_image_full);
                imageFullShare.setVisibility(View.VISIBLE);

                View splitViewShare = v.findViewById(R.id.share_split_image_container);
                splitViewShare.setVisibility(View.GONE);

                View vsLabelShare = v.findViewById(R.id.share_vs_label);
                vsLabelShare.setVisibility(View.GONE);


                aq.id(R.id.preview_image).image(t.getUrl());
                aq.id(R.id.share_image_full).image(t.getUrl());
            }
            else if(m.getThumbnails() != null && m.getThumbnails().size() > 0) {

                thumbnailUrl = m.getThumbnails().get(0).getUrl();

                View imageFullShare = v.findViewById(R.id.share_image_full);
                imageFullShare.setVisibility(View.VISIBLE);

                View splitViewShare = v.findViewById(R.id.share_split_image_container);
                splitViewShare.setVisibility(View.GONE);

                View vsLabelShare = v.findViewById(R.id.share_vs_label);
                vsLabelShare.setVisibility(View.GONE);

                aq.id(R.id.preview_image).image(m.getThumbnails().get(0).getUrl());
                aq.id(R.id.share_image_full).image(m.getThumbnails().get(0).getUrl());
            }
            else {
                thumbnailUrl = "";
            }

            View facebookButton = v.findViewById(R.id.facebook_button);

            facebookButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), thumbnailUrl, PostDialog.FACEBOOK_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), thumbnailUrl, PostDialog.FACEBOOK_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                }
            });

            View twitterButton = v.findViewById(R.id.twitter_button);

            twitterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), "", PostDialog.TWITTER_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), "", PostDialog.TWITTER_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }

                }
            });


            View emailButton = v.findViewById(R.id.mail_button);

            emailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");

                        i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                        i.putExtra(Intent.EXTRA_TEXT   , text);

                        Bitmap image = aq.getCachedImage(thumbnailUrl);

                        File cacheImage = new File(getActivity().getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                        try {

                            image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));

                            if(image != null) {
                                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                            }


                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {

                        }
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");

                        i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                        i.putExtra(Intent.EXTRA_TEXT   , text);

                        Bitmap image = aq.getCachedImage(thumbnailUrl);

                        File cacheImage = new File(getActivity().getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                        try {

                            image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));

                            if(image != null) {
                                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                            }


                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {

                        }
                    }

                }
            });

            View clipboardButton = v.findViewById(R.id.clipboard_button);
            clipboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Win Sports", "http://www.winsportsonline.com/index-3"));

                    Toast.makeText(getActivity(), "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show();
                }
            });
        }

    }

    private void displayLastMatches() {
        for (final Media m : lastMatchesList) {

            View v = getActivity().getLayoutInflater().inflate(R.layout.highlight_cell, null);
            if(v!=null)
                lastMatchesContainer.addView(v);

            Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");
//            Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Bold.otf");

            TextView titleLabel = (TextView)v.findViewById(R.id.title_label);
            titleLabel.setText(m.getTitle().replace(" - ", "\n"));
            titleLabel.setTypeface(light);
//
            TextView te = (TextView) v.findViewById(R.id.share_text);
            te.setTypeface(light);

            TextView timeLabel = (TextView)v.findViewById(R.id.time_label);
            DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
            String date = "";
            try{
                date = df.format(m.getDateRecorded()) + " - ";
            }catch(Exception e){
                //NOTHING
            }
            timeLabel.setText(String.format("%s%d Min.", date, m.getDuration() / 60));
            timeLabel.setTypeface(light);

            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)v.getLayoutParams();
            if(params!=null){
                params.setMargins(5,0,5,5);
                params.height = LinearLayout.LayoutParams.MATCH_PARENT;
                params.width = 295;
            }

            v.setLayoutParams(params);

            ImageButton shareButton = (ImageButton)v.findViewById(R.id.share_button);

            final View show = v.findViewById(R.id.show_container);
            final View share = v.findViewById(R.id.share_container);

            shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(prevShare != null && prevShow != null) {
                        hideShare(prevShare,prevShow);
                    }

                    displayShare(share,show);
                    prevShare = share;
                    prevShow = show;
                }
            });

            share.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    hideShare(share,show);
                    prevShare = null;
                    prevShow = null;
                }
            });

            show.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (videoDelegate != null) {
                        SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                        boolean permission = false;
                        List<String> countries = null;
                        try{
                            countries = m.getAccessRules().getGeo().getCountries();
                        }catch(Exception e){
                            e.printStackTrace();
                            Log.e("COUNTRIES","Error: "+e.getMessage());
                        }
                        if(countries != null && countries.size() > 0){
                            for(String country : countries){
                                if(country.equalsIgnoreCase(sp.getString("country_code",""))){
                                    permission = true;
                                }
                            }
                        }else{
                            permission = true;
                        }
                        if(permission){
                            videoDelegate.onVideoSelected(m);
                        }else{
                            MessageDialog messageDialog = new MessageDialog("ESTE CONTENIDO NO ESTÁ DISPONIBLE EN TU UBICACIÓN.");
                            messageDialog.show(getActivity().getFragmentManager(),"dialog");
                        }
                    }
                }
            });


            final AQuery aq = new AQuery(v);
            final String thumbnailUrl;
            Thumbnail t = m.getDefaultThumbnail();
            if (t != null) {
                thumbnailUrl = t.getUrl();
                View imageFullShare = v.findViewById(R.id.share_image_full);
                imageFullShare.setVisibility(View.VISIBLE);

                View splitViewShare = v.findViewById(R.id.share_split_image_container);
                splitViewShare.setVisibility(View.GONE);

                View vsLabelShare = v.findViewById(R.id.share_vs_label);
                vsLabelShare.setVisibility(View.GONE);

                aq.id(R.id.preview_image).image(t.getUrl());
                aq.id(R.id.share_image_full).image(t.getUrl());
            }
            else if(m.getThumbnails() != null && m.getThumbnails().size() > 0) {
                thumbnailUrl = m.getThumbnails().get(0).getUrl();
                View imageFullShare = v.findViewById(R.id.share_image_full);
                imageFullShare.setVisibility(View.VISIBLE);

                View splitViewShare = v.findViewById(R.id.share_split_image_container);
                splitViewShare.setVisibility(View.GONE);

                View vsLabelShare = v.findViewById(R.id.share_vs_label);
                vsLabelShare.setVisibility(View.GONE);

                aq.id(R.id.preview_image).image(m.getThumbnails().get(0).getUrl());
                aq.id(R.id.share_image_full).image(m.getThumbnails().get(0).getUrl());
            }
            else {
                thumbnailUrl = "";
            }

            View facebookButton = v.findViewById(R.id.facebook_button);

            facebookButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), thumbnailUrl, PostDialog.FACEBOOK_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), thumbnailUrl, PostDialog.FACEBOOK_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                }
            });

            View twitterButton = v.findViewById(R.id.twitter_button);

            twitterButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), "", PostDialog.TWITTER_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        PostDialog postDialog = new PostDialog(text, m.getTitle(), "", PostDialog.TWITTER_SHARE);
                        postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                    }

                }
            });


            View emailButton = v.findViewById(R.id.mail_button);

            emailButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if(m.belongsToCategoryByName("Partido")) {
                        String text = String.format("Me repito el plato: Estoy viendo en VOD %s por Win Sports Online", m.getTitle());

                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");

                        i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                        i.putExtra(Intent.EXTRA_TEXT   , text);

                        Bitmap image = aq.getCachedImage(thumbnailUrl);

                        File cacheImage = new File(getActivity().getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                        try {

                            image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));

                            if(image != null) {
                                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                            }


                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {

                        }
                    }
                    else {

                        String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", m.getTitle());

                        Intent i = new Intent(Intent.ACTION_SEND);
                        i.setType("message/rfc822");

                        i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                        i.putExtra(Intent.EXTRA_TEXT   , text);

                        Bitmap image = aq.getCachedImage(thumbnailUrl);

                        File cacheImage = new File(getActivity().getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                        try {

                            image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));

                            if(image != null) {
                                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                            }


                            startActivity(Intent.createChooser(i, "Send mail..."));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {

                        }
                    }

                }
            });

            View clipboardButton = v.findViewById(R.id.clipboard_button);
            clipboardButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText("Win Sports Online", "http://www.winsportsonline.com/index-3"));

                    Toast.makeText(getActivity(), "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    private void displayShare(View share, View show) {
        ObjectAnimator rotationShow = ObjectAnimator.ofFloat(share, "y",share.getMeasuredHeight(), 0.0f);
        rotationShow.setDuration(500);

        show.setPivotY(0);
        show.setPivotX(show.getMeasuredWidth() / 2.0f);
        ObjectAnimator rotationShare = ObjectAnimator.ofFloat(show, "y", 0.0f, -show.getMeasuredHeight());
        rotationShare.setDuration(500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationShow, rotationShare);
        animatorSet.start();
    }

    private void hideShare(View share, View show) {
        ObjectAnimator rotationShow = ObjectAnimator.ofFloat(show, "y",-show.getMeasuredHeight(), 0.0f);
        rotationShow.setDuration(500);

        ObjectAnimator rotationShare = ObjectAnimator.ofFloat(share, "y", 0.0f, share.getMeasuredHeight());
        rotationShare.setDuration(500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationShow, rotationShare);
        animatorSet.start();
    }
}
