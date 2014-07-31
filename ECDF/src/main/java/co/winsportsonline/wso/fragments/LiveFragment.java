package co.winsportsonline.wso.fragments;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.LiveStream;
import co.winsportsonline.wso.datamodel.LiveStreamSchedule;
import co.winsportsonline.wso.delegates.VideoDelegate;
import co.winsportsonline.wso.dialogs.MessageDialog;
import co.winsportsonline.wso.dialogs.PostDialog;
import co.winsportsonline.wso.dialogs.ShareDialog;
import co.winsportsonline.wso.services.ServiceManager;

/**
 * Created by Franklin  Cruz on 17-02-14.
 */
public class LiveFragment extends Fragment {

    public static final String LIVE_LEFT_HEADER_URL_FORMATSTR = "http://winsportsonline.com/assets/img/event/large/large-";
    public static final String LIVE_RIGHT_HEADER_URL_FORMATSTR = "http://winsportsonline.com/assets/img/event/large/large-";

    public static final String LIVE_THUMBNAIL_VISIT_IMAGE_URL = "http://winsportsonline.com/assets/img/event/small/small-";
    public static final String LIVE_THUMBNAIL_LOCAL_IMAGE_URL = "http://winsportsonline.com/assets/img/event/small/small-";

    public static final String LIVE_LOCAL_TEAM_URL_FORMATSTR = "http://winsportsonline.com/assets/img/event/large/large-";
    public static final String LIVE_VISIT_TEAM_URL_FORMATSTR = "http://winsportsonline.com/assets/img/event/large/large-";

    private LinearLayout nextShowContainer;

    private List<LiveStreamSchedule> liveStreamSchedules = new ArrayList<LiveStreamSchedule>();
    LiveStreamSchedule nextShow;

    private View rootView;
    private View prevShow = null;
    private View prevShare = null;

    private Resources res;
    private Drawable d;
    private ImageView icono_compartir;
    private int loadedSources = 0;

    private VideoDelegate videoDelegate;

    public void setVideoSelectedDelegate(VideoDelegate delegate) {
        this.videoDelegate = delegate;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        rootView = inflater.inflate(R.layout.fragment_live, container, false);
        res = getResources();
        d = res.getDrawable(R.drawable.share_icon_b);

        final ProgressDialog progress = new ProgressDialog(getActivity());
        progress.show();
        progress.setContentView(R.layout.progress_dialog);
        progress.setCancelable(false);
        progress.setCanceledOnTouchOutside(false);

        nextShowContainer = (LinearLayout) rootView.findViewById(R.id.container_next_shows);

        final ServiceManager serviceManager = new ServiceManager(getActivity());
        serviceManager.loadLiveStreamList(new ServiceManager.DataLoadedHandler<LiveStream>() {
            @Override
            public void loaded(List<LiveStream> data) {
                loadedSources = 0;
                final int totalSources = data.size();

                liveStreamSchedules.clear();
                for (int i = 0; i < data.size(); ++i) {
                    serviceManager.loadLiveStreamSchedule(data.get(i), new ServiceManager.DataLoadedHandler<LiveStreamSchedule>() {
                        @Override
                        public void loaded(List<LiveStreamSchedule> data) {

                            liveStreamSchedules.addAll(data);

                            Collections.sort(liveStreamSchedules, new Comparator<LiveStreamSchedule>() {
                                @Override
                                public int compare(LiveStreamSchedule lhs, LiveStreamSchedule rhs) {
                                    if (lhs.getStartDate().getTime() > rhs.getStartDate().getTime()) {
                                        return 1;
                                    } else if (lhs.getStartDate().getTime() < rhs.getStartDate().getTime()) {
                                        return -1;
                                    } else {
                                        return 0;
                                    }
                                }
                            });
                            TextView equipo1 = (TextView) rootView.findViewById(R.id.left_title_label);
                            TextView equipo2 = (TextView) rootView.findViewById(R.id.right_title_label);
                            equipo1.setText("");
                            equipo2.setText("");

                            ++loadedSources;
                            if (liveStreamSchedules.size() > 0 && loadedSources == totalSources) {
                                nextShow = liveStreamSchedules.get(0);

                                List<LiveStreamSchedule> matchLiveList = countMatchLive();

                                int ini;
                                if (matchLiveList.size() > 1) {
                                    displayNextShow(liveStreamSchedules.subList(0, 2));
                                    ini = 2;
                                } else {
                                    displayNextShow(liveStreamSchedules.get(0));
                                    ini = 1;
                                }

                                nextShowContainer.removeAllViews();
                                for (int i = ini; i < liveStreamSchedules.size(); ++i) {
                                    createLiveMediaCell(liveStreamSchedules.get(i));
                                }

                            }
                        }

                        @Override
                        public void error(String error) {
                            super.error(error);
                            loadedSources++;
                            if (liveStreamSchedules.size() > 0 && loadedSources == totalSources) {
                                nextShow = liveStreamSchedules.get(0);

                                displayNextShow(liveStreamSchedules.get(0));
                                nextShowContainer.removeAllViews();
                                for (int i = 1; i < liveStreamSchedules.size(); ++i) {
                                    createLiveMediaCell(liveStreamSchedules.get(i));
                                }
                            }
                        }
                    });
                }
                progress.dismiss();
            }
        });

        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");
        Button playButton = (Button) rootView.findViewById(R.id.play_button);
        playButton.setTypeface(light);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (nextShow != null) {
                    if (videoDelegate != null) {
                        SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                        boolean permission = false;
                        List<String> countries = null;
                        try {
                            countries = nextShow.getAccessRules().getGeo().getCountries();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("COUNTRIES", "Error: " + e.getMessage());
                        }
                        if (countries != null && countries.size() > 0) {
                            for (String country : countries) {
                                if (country.equalsIgnoreCase(sp.getString("country_code", ""))) {
                                    permission = true;
                                }
                            }
                        } else {
                            permission = true;
                        }
                        if (permission) {
                            videoDelegate.onLiveShowBegin(nextShow,1);
                        } else {
                            MessageDialog messageDialog = new MessageDialog("ESTE CONTENIDO NO ESTÁ DISPONIBLE EN TU UBICACIÓN.");
                            messageDialog.show(getActivity().getFragmentManager(), "dialog");
                        }
                    }
                }
            }
        });

        return rootView;
    }

    private List<LiveStreamSchedule> countMatchLive() {
        List<LiveStreamSchedule> list = new ArrayList<LiveStreamSchedule>();
        Long now = new Date().getTime();
        for (LiveStreamSchedule lss : liveStreamSchedules) {
            if (lss.getStartDate().getTime() < now && lss.getEndDate().getTime() > now) {
                list.add(lss);
            }
        }
        return list;
    }

    private void displayNextShow(final LiveStreamSchedule media) {
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        RelativeLayout r = (RelativeLayout) rootView.findViewById(R.id.one_match);
        r.setVisibility(View.VISIBLE);
        RelativeLayout t = (RelativeLayout) rootView.findViewById(R.id.two_match);
        t.setVisibility(View.GONE);
        TextView shareButton = (TextView) rootView.findViewById(R.id.button_share_live);
        TextView timeTextView = (TextView) rootView.findViewById(R.id.time_label);
        TextView envivo = (TextView) rootView.findViewById(R.id.en_vivo);
        TextView proximo = (TextView) rootView.findViewById(R.id.proximos_encuetros);
        TextView teamLeft = (TextView) rootView.findViewById(R.id.left_title_label);
        TextView teamRight = (TextView) rootView.findViewById(R.id.right_title_label);

        teamLeft.setText("");
        teamRight.setText("");
        teamLeft.setTypeface(light);
        teamRight.setTypeface(light);
        timeTextView.setTypeface(light);
        proximo.setTypeface(light);
        proximo.setText("A CONTINUACIÓN");
        envivo.setTypeface(light);
        shareButton.setTypeface(light);
        shareButton.setText("COMPARTIR PROGRAMA");

        DateFormat df = new SimpleDateFormat("HH:mm' HRS'");
        timeTextView.setText(df.format(media.getStartDate()));
        envivo.setText("EN VIVO");

        icono_compartir = (ImageView) rootView.findViewById(R.id.imagen_compartir);
        icono_compartir.setImageDrawable(d);

        final AQuery aq = new AQuery(rootView.findViewById(R.id.one_match));

        String[] splited = null;

        if (media.getCode() != null) {
            splited = media.getCode().split("_vs_");
            aq.id(R.id.main_image_left).image(LIVE_LOCAL_TEAM_URL_FORMATSTR + "" + media.getCode() + ".jpg");
            aq.id(R.id.main_image_right).image(LIVE_VISIT_TEAM_URL_FORMATSTR + "" + media.getCode() + ".jpg");
        } else {
            if (media.getName() != null)
                splited = media.getName().split(" v/s ");
        }
        if (splited.length == 2) {
            teamLeft.setText(splited[0].replace("-", " ").toUpperCase());
            teamRight.setText(splited[1].replace("-", " ").toUpperCase());
        } else {
            teamLeft.setText(media.getName().replace("-", " ").toUpperCase());
        }
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareDialog shareDialog = new ShareDialog(aq, videoDelegate, getActivity(), media);
                shareDialog.show(getFragmentManager(), "dialog");
            }
        });
    }

    private void displayNextShow(final List<LiveStreamSchedule> listMatch) {

        Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Bold.otf");
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        RelativeLayout r = (RelativeLayout) rootView.findViewById(R.id.one_match);
        r.setVisibility(View.GONE);
        RelativeLayout t = (RelativeLayout) rootView.findViewById(R.id.two_match);
        t.setVisibility(View.VISIBLE);

        Button play1 = (Button) rootView.findViewById(R.id.play_button_1);
        Button play2 = (Button) rootView.findViewById(R.id.play_button_2);

        TextView name1 = (TextView) rootView.findViewById(R.id.en_vivo_1);
        name1.setTypeface(bold);
        TextView name2 = (TextView) rootView.findViewById(R.id.en_vivo_2);
        name2.setTypeface(bold);

        TextView time1 = (TextView) rootView.findViewById(R.id.time_label_1);
        time1.setTypeface(light);
        TextView time2 = (TextView) rootView.findViewById(R.id.time_label_2);
        time2.setTypeface(light);

        TextView proximo = (TextView) rootView.findViewById(R.id.proximos_encuetros);
        proximo.setTypeface(light);
        proximo.setText("A CONTINUACIÓN");

        TextView shareButton1 = (TextView) rootView.findViewById(R.id.button_share_live_1);
        shareButton1.setTypeface(light);
        TextView shareButton2 = (TextView) rootView.findViewById(R.id.button_share_live_2);
        shareButton2.setTypeface(light);

        TextView teamLeft = (TextView) rootView.findViewById(R.id.left_title_label_1);
        teamLeft.setTypeface(light);
        teamLeft.setText(listMatch.get(0).getName().toUpperCase());
        TextView teamRight = (TextView) rootView.findViewById(R.id.right_title_label_2);
        teamRight.setTypeface(light);
        teamRight.setText(listMatch.get(1).getName().toUpperCase());

        DateFormat df = new SimpleDateFormat("HH:mm' HRS'");
        time1.setText(df.format(listMatch.get(0).getStartDate()));
        time2.setText(df.format(listMatch.get(1).getStartDate()));

        final AQuery aq = new AQuery(rootView.findViewById(R.id.two_match));

        if (listMatch.get(0).getCode() != null) {
            aq.id(R.id.main_image_left_1).image(LIVE_LOCAL_TEAM_URL_FORMATSTR + "" + listMatch.get(0).getCode() + ".jpg");
            aq.id(R.id.main_image_left_2).image(LIVE_LOCAL_TEAM_URL_FORMATSTR + "" + listMatch.get(0).getCode() + ".jpg");
            aq.id(R.id.main_image_right_1).image(LIVE_VISIT_TEAM_URL_FORMATSTR + "" + listMatch.get(1).getCode() + ".jpg");
            aq.id(R.id.main_image_right_2).image(LIVE_VISIT_TEAM_URL_FORMATSTR + "" + listMatch.get(1).getCode() + ".jpg");
        }
        play1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                boolean permission = false;
                List<String> countries = null;
                try {
                    countries = listMatch.get(0).getAccessRules().getGeo().getCountries();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.e("COUNTRIES", "Error: " + e.getMessage());
                }
                if (countries != null && countries.size() > 0) {
                    for (String country : countries) {
                        if (country.equalsIgnoreCase(sp.getString("country_code", ""))) {
                            permission = true;
                        }
                    }
                } else {
                    permission = true;
                }
                if (permission) {
                    videoDelegate.onLiveShowBegin(listMatch.get(0),1);
                } else {
                    MessageDialog messageDialog = new MessageDialog("ESTE CONTENIDO NO ESTÁ DISPONIBLE EN TU UBICACIÓN.");
                    messageDialog.show(getActivity().getFragmentManager(), "dialog");
                }
            }
        });
        play2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences sp = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);
                boolean permission = false;
                List<String> countries = null;
                try {
                    countries = listMatch.get(1).getAccessRules().getGeo().getCountries();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (countries != null && countries.size() > 0) {
                    for (String country : countries) {
                        if (country.equalsIgnoreCase(sp.getString("country_code", ""))) {
                            permission = true;
                        }
                    }
                } else {
                    permission = true;
                }
                if (permission) {
                    videoDelegate.onLiveShowBegin(listMatch.get(1),2);
                } else {
                    MessageDialog messageDialog = new MessageDialog("ESTE CONTENIDO NO ESTÁ DISPONIBLE EN TU UBICACIÓN.");
                    messageDialog.show(getActivity().getFragmentManager(), "dialog");
                }
            }
        });

        shareButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareDialog shareDialog = new ShareDialog(aq, videoDelegate, getActivity(), listMatch.get(0));
                shareDialog.show(getFragmentManager(), "dialog");
            }
        });
        shareButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ShareDialog shareDialog = new ShareDialog(aq, videoDelegate, getActivity(), listMatch.get(1));
                shareDialog.show(getFragmentManager(), "dialog");
            }
        });
    }

    private void createLiveMediaCell(final LiveStreamSchedule media) {

//        Typeface bold = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Bold.otf");
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        final RelativeLayout v = (RelativeLayout) getLayoutInflater(null).inflate(R.layout.live_show_cell, null);
        final AQuery aq = new AQuery(v);

        TextView title = (TextView) v.findViewById(R.id.title_label);
        title.setTypeface(light);

        TextView t = (TextView) v.findViewById(R.id.share_text);
        t.setTypeface(light);

        if (media.getName() != null)
            if (media.getName().length() < 30)
                title.setText(media.getName().toUpperCase());
            else
                title.setText(media.getName().toUpperCase().substring(0, 29) + "...");
        else
            title.setText("WIN SPORTS ONLINE");

        TextView time = (TextView) v.findViewById(R.id.time_label);

        DateFormat df = new SimpleDateFormat("dd MMM, HH:mm 'HRS'");

        time.setText(df.format(media.getStartDate()));
        time.setTypeface(light);

        final String[] splited;

        if (media.getCode() != null) {
            splited = media.getCode().split("_vs_");

            View imageFullShare = v.findViewById(R.id.share_image_full);
            imageFullShare.setVisibility(View.VISIBLE);

            View imageFull = v.findViewById(R.id.image_full);
            imageFull.setVisibility(View.VISIBLE);

            aq.id(R.id.image_full).image(LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg");
            aq.id(R.id.share_image_full).image(LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg");

            ImageView image = (ImageView) v.findViewById(R.id.image_full);
            ImageView image2 = (ImageView) v.findViewById(R.id.share_image_full);

            image.setClickable(false);
            image.setFocusable(false);
            image.setEnabled(false);

            image2.setClickable(false);
            image2.setFocusable(false);
            image2.setEnabled(false);

        } else {

            splited = media.getName().split(" v/s ");

        }

        if (splited.length > 1) {

            View vsLabelShare = v.findViewById(R.id.share_vs_label);
            vsLabelShare.setVisibility(View.VISIBLE);

            if(!media.getCode().equalsIgnoreCase("default")){
                View vsLabel = v.findViewById(R.id.vs_label);
                vsLabel.setVisibility(View.VISIBLE);
            }

        }

        View splitView = v.findViewById(R.id.split_image_container);
        splitView.setVisibility(View.GONE);

        View splitViewShare = v.findViewById(R.id.share_split_image_container);
        splitViewShare.setVisibility(View.GONE);


        final View show = v.findViewById(R.id.show_container);
        final View share = v.findViewById(R.id.share_container);

        show.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Do nothing!!!
            }
        });

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                hideShare(share, show);
                prevShare = null;
                prevShow = null;

            }
        });

        ImageButton shareButton = (ImageButton) v.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View sender) {

                if (prevShare != null && prevShow != null) {
                    hideShare(prevShare, prevShow);
                }

                displayShare(share, show);
                prevShare = share;
                prevShow = show;

            }
        });

        final DateFormat dfS = new SimpleDateFormat("HH:mm 'HRS el' dd 'de' MMM");
        View facebookButton = v.findViewById(R.id.facebook_button);

        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String text = String.format("%s a las %s por http://goo.gl/IpRqp3", media.getName(), dfS.format(media.getStartDate()));
                PostDialog postDialog = new PostDialog(text, media.getName(), LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg", PostDialog.FACEBOOK_SHARE);
                postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
            }
        });

        View twitterButton = v.findViewById(R.id.twitter_button);

        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (splited.length > 1) {
                    String text = String.format("%s a las %s por http://goo.gl/IpRqp3", media.getName(), dfS.format(media.getStartDate()));

                    PostDialog postDialog = new PostDialog(text, media.getName(), LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg", PostDialog.TWITTER_SHARE);
                    postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                } else {

                    String text = String.format("%s a las %s por http://goo.gl/IpRqp3H", media.getName(), dfS.format(media.getStartDate()));

                    PostDialog postDialog = new PostDialog(text, media.getName(), LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg", PostDialog.TWITTER_SHARE);
                    postDialog.show(getActivity().getSupportFragmentManager(), "dialog");
                }

            }
        });


        View emailButton = v.findViewById(R.id.mail_button);

        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoDelegate != null) {

                    String text = String.format("Estoy viendo EN VIVO %s por Win Sports Online", media.getName());

                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");

                    i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                    i.putExtra(Intent.EXTRA_TEXT, text);

                    Bitmap image = aq.getCachedImage(LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode() + ".jpg");

                    File cacheImage = new File(getActivity().getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                    try {
                        try{
                            image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));
                        }catch(Exception e){
                            //
                        }

                        if (image != null) {
                            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                        }
                        getActivity().startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
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

        ImageButton reminderButton = (ImageButton) v.findViewById(R.id.remind_button);
        reminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createReminder(media);
            }
        });

        nextShowContainer.addView(v);

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) v.getLayoutParams();
        params.setMargins(5, 0, 5, 5);
        params.height = LinearLayout.LayoutParams.MATCH_PARENT;
        params.width = 367;

        v.setClipChildren(false);
        v.setLayoutParams(params);

        v.setEnabled(false);
        v.setClickable(false);
        v.setFocusable(false);
        v.setSoundEffectsEnabled(false);
    }

    private void displayShare(View share, View show) {
        ObjectAnimator rotationShow = ObjectAnimator.ofFloat(share, "y", share.getMeasuredHeight(), 0.0f);
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
        ObjectAnimator rotationShow = ObjectAnimator.ofFloat(show, "y", -show.getMeasuredHeight(), 0.0f);
        rotationShow.setDuration(500);

        ObjectAnimator rotationShare = ObjectAnimator.ofFloat(share, "y", 0.0f, share.getMeasuredHeight());
        rotationShare.setDuration(500);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotationShow, rotationShare);
        animatorSet.start();
    }

    private void createReminder(LiveStreamSchedule liveStreamSchedule) {

        SharedPreferences prefs = getActivity().getSharedPreferences("co.winsportsonline.wso", Context.MODE_PRIVATE);

        if (prefs.getBoolean("reminder_" + liveStreamSchedule.getEventId(), false)) {
            Toast.makeText(getActivity(), "El recordatorio ya ha sido creado", Toast.LENGTH_SHORT).show();
            return;
        }

        int id_calendars[] = getCalendar(getActivity());

        if (id_calendars.length == 0) {
            Toast.makeText(getActivity(), "Calendario no disponible", Toast.LENGTH_SHORT).show();
            return;
        }

        long calID = id_calendars[0];

        long startMillis = 0;

        long endMillis = 0;

        Calendar beginTime = Calendar.getInstance();
        beginTime.setTime(liveStreamSchedule.getStartDate());
        startMillis = beginTime.getTimeInMillis();
        Calendar endTime = Calendar.getInstance();
        endTime.setTime(liveStreamSchedule.getEndDate());
        endMillis = endTime.getTimeInMillis();

        TimeZone timeZone = TimeZone.getDefault();

        ContentResolver cr = getActivity().getContentResolver();
        ContentValues values = new ContentValues();

        values.put(CalendarContract.Events.DTSTART, startMillis);
        values.put(CalendarContract.Events.DTEND, endMillis);
        values.put(CalendarContract.Events.TITLE, liveStreamSchedule.getName());

        //TODO: Add description?
        //values.put(CalendarContract.Events.DESCRIPTION, "");

        values.put(CalendarContract.Events.CALENDAR_ID, calID);


        values.put(CalendarContract.Events.EVENT_TIMEZONE, timeZone.getID());

        values.put(CalendarContract.Events.ALL_DAY, 0);


        Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);

        long eventID = Long.parseLong(uri.getLastPathSegment());

        ContentValues reminderValues = new ContentValues();

        reminderValues.put(CalendarContract.Reminders.MINUTES, 3);
        reminderValues.put(CalendarContract.Reminders.EVENT_ID, eventID);
        reminderValues.put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT);

        cr.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues);

        prefs.edit().putBoolean("reminder_" + liveStreamSchedule.getEventId(), true).commit();

        Toast.makeText(getActivity(), "Recordatorio creado", Toast.LENGTH_SHORT).show();
    }

    public int[] getCalendar(Context c) {

        String projection[] = {"_id", "calendar_displayName"};

        Uri calendars = Uri.parse("content://com.android.calendar/calendars");

        ContentResolver contentResolver = c.getContentResolver();
        Cursor managedCursor = contentResolver.query(calendars, projection, null, null, null);

        int aux[] = new int[0];

        if (managedCursor.moveToFirst()) {

            aux = new int[managedCursor.getCount()];

            int cont = 0;
            do {
                aux[cont] = managedCursor.getInt(cont);
                cont++;
            } while (managedCursor.moveToNext());

            managedCursor.close();
        }
        return aux;

    }

}