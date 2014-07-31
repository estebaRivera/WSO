package co.winsportsonline.wso.delegates;

import co.winsportsonline.wso.datamodel.LiveStreamSchedule;
import co.winsportsonline.wso.datamodel.Media;

/**
 * Created by Franklin Cruz on 14-03-14.
 */
public abstract class VideoDelegate {

    public void onVideoSelected(Media media) {

    }

    public void onLiveShowBegin(LiveStreamSchedule media, int index) {

    }

    public void displayImageChooser(String image1, String image2, ImageChooserDelegate delegate) {

    }

    public void onVideoSelected(LiveStreamSchedule media) {

    }
}
