package co.winsportsonline.wso.delegates;

import android.app.ProgressDialog;

import java.util.List;

import co.winsportsonline.wso.datamodel.Filter;
import co.winsportsonline.wso.datamodel.Media;
import co.winsportsonline.wso.fragments.SlideMenu;

/**
 * Created by Franklin Cruz on 28-02-14.
 */
public abstract class SlideMenuDelegate {

    public void onFilterSelected(SlideMenu slidemenu, Filter filter) {

    }

    public void onSearchSelected(SlideMenu slidemenu, List<Media> media, ProgressDialog progress) {

    }

    public void onVodSelected(SlideMenu slidemenu) {

    }

    public void onLiveSelected(SlideMenu slidemenu) {

    }

}
