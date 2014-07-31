package co.winsportsonline.wso.datamodel;

import java.util.Date;

/**
 * Created by Franklin Cruz on 06-03-14.
 */
public class ViewingTimeLimit implements DataModel {

    private Date userTime;
    private int userUnits;
    private int seconds;
    private boolean enabled;


    @DataMember(member = "user_time")
    public Date getUserTime() {
        return userTime;
    }

    @DataMember(member = "user_time")
    public void setUserTime(Date userTime) {
        this.userTime = userTime;
    }

    @DataMember(member = "user_unit")
    public int getUserUnits() {
        return userUnits;
    }

    @DataMember(member = "user_unit")
    public void setUserUnits(int userUnits) {
        this.userUnits = userUnits;
    }

    @DataMember(member = "seconds")
    public int getSeconds() {
        return seconds;
    }

    @DataMember(member = "seconds")
    public void setSeconds(int seconds) {
        this.seconds = seconds;
    }

    @DataMember(member = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @DataMember(member = "enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
