package co.winsportsonline.wso.datamodel;

/**
 * Created by Boris on 07-05-14.
 */
public class Facebook implements DataModel {
    String facebookId;
    String username;

    @DataMember(member = "id")
    public String getId() {
        return facebookId;
    }

    @DataMember(member = "id")
    public void setId(String facebookId) {
        this.facebookId = facebookId;
    }

    @DataMember(member = "username")
    public String getUsername() {
        return username;
    }

    @DataMember(member = "username")
    public void setUsername(String username) {
        this.username = username;
    }
}
