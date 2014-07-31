package co.winsportsonline.wso.datamodel;

/**
 * Created by Franklin Cruz on 07-03-14.
 */
public class TokenIssue implements DataModel {

    private String status;
    private String message;
    private String accessToken;

    @DataMember(member = "status")
    public String getStatus() {
        return status;
    }

    @DataMember(member = "status")
    public void setStatus(String status) {
        this.status = status;
    }

    @DataMember(member = "message")
    public String getMessage() {
        return message;
    }

    @DataMember(member = "message")
    public void setMessage(String message) {
        this.message = message;
    }

    @DataMember(member = "access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @DataMember(member = "access_token")
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
