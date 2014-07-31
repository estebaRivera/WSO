package co.winsportsonline.wso.dialogs;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.androidquery.AQuery;

import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.datamodel.LiveStreamSchedule;
import co.winsportsonline.wso.delegates.VideoDelegate;


/**
 * Created by Boris on 14-04-14.
 */
public class ShareDialog extends DialogFragment{

    private AQuery aq;
    private VideoDelegate videoDelegate;
    private FragmentActivity activity;
    private LiveStreamSchedule media;
    public static final String LIVE_LEFT_HEADER_URL_FORMATSTR = "http://winsportsonline.com/assets/img/event/large/large-";

    public ShareDialog(AQuery aq, VideoDelegate videoDelegate, FragmentActivity activity, LiveStreamSchedule media) {
        setStyle(android.support.v4.app.DialogFragment.STYLE_NO_TITLE, getTheme());
        this.aq = aq;
        this.videoDelegate = videoDelegate;
        this.activity = activity;
        this.media = media;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Typeface light = Typeface.createFromAsset(getActivity().getAssets(), "fonts/Oswald-Light.otf");

        View rootView = inflater.
                inflate(R.layout.share_dialog, container, false);

        TextView shareLabel = (TextView)rootView.findViewById(R.id.share_label);
        shareLabel.setTypeface(light);

        String[] splited;
        if (media.getCode() != null) {
            splited = media.getCode().split("_vs_");
        }else{
            splited = media.getName().split(" vs ");
        }
        String textSocial;
        if(splited.length == 2){
            textSocial = String.format("Estoy viendo %s por http://goo.gl/IpRqp3 #fpcxwin", media.getName());
        }else{
            textSocial = String.format("Estoy viendo %s por http://goo.gl/IpRqp3", media.getName());
        }
        final String text = textSocial;

        View facebookButton = rootView.findViewById(R.id.facebook_button_live);
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
                if (videoDelegate != null) {
                    PostDialog postDialog = new PostDialog(text, media.getName(), LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode()+".jpg", PostDialog.FACEBOOK_SHARE);
                    postDialog.show(activity.getSupportFragmentManager(), "dialog");
                }
            }
        });

        View twitterButton = rootView.findViewById(R.id.twitter_button_live);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getDialog().cancel();
                PostDialog postDialog = new PostDialog(text, media.getName(), LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode()+".jpg", PostDialog.TWITTER_SHARE);
                postDialog.show(getFragmentManager(), "dialog");
            }
        });

        View emailButton = rootView.findViewById(R.id.mail_button_live);

        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDialog().cancel();
                if (videoDelegate != null) {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("message/rfc822");

                    i.putExtra(Intent.EXTRA_SUBJECT, "Win Sports Online");
                    i.putExtra(Intent.EXTRA_TEXT, text);

                    Bitmap image = aq.getCachedImage(LIVE_LEFT_HEADER_URL_FORMATSTR + media.getCode()+".jpg");

                    File cacheImage = new File(activity.getExternalCacheDir() + File.pathSeparator + UUID.randomUUID().toString() + ".png");

                    try {
                        image.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(cacheImage));
                        if (image != null) {
                            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(cacheImage));
                        }
                        activity.startActivity(Intent.createChooser(i, "Send mail..."));
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(activity, "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(activity, "No existen clientes de correo instalados.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        View clipboardButton = rootView.findViewById(R.id.clipboard_button_live);
        clipboardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("Win Sports Online", "https://www.winsportsonline.com/index-1"));
                Toast.makeText(activity, "Enlace copiado al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        return rootView;
    }
}
