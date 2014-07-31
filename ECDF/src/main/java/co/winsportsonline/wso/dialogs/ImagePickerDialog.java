package co.winsportsonline.wso.dialogs;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.androidquery.AQuery;

import co.winsportsonline.wso.R;
import co.winsportsonline.wso.delegates.ImageChooserDelegate;

/**
 * Created by Franklin Cruz on 21-03-14.
 */
public class ImagePickerDialog extends DialogFragment {

    private String url1;
    private String url2;

    private ImageChooserDelegate delegate;

    public void setDelegate(ImageChooserDelegate delegate) {
        this.delegate = delegate;
    }

    public ImagePickerDialog(String url1, String url2) {

        this.url1 = url1;
        this.url2 = url2;

        setStyle(DialogFragment.STYLE_NO_TITLE, getTheme());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_imagechooser, container, false);

        AQuery aq = new AQuery(rootView);

        aq.id(R.id.main_image_left).image(url1).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(delegate != null) {
                    delegate.onImageSelected(url1);
                }

                dismiss();
            }
        });
        aq.id(R.id.main_image_right).image(url2).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(delegate != null) {
                    delegate.onImageSelected(url1);
                }

                dismiss();
            }
        });



        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(0));

        return rootView;
    }

}
