package co.winsportsonline.wso.datamodel;

/**
 * Created by Boris on 07-05-14.
 */
public class Social implements DataModel {
    Facebook facebook;

    @DataMember(member = "facebook")
    public Facebook getFacebook() {
        return facebook;
    }

    @DataMember(member = "facebook")
    public void setFacebook(Facebook facebook) {
        this.facebook = facebook;
    }
}
