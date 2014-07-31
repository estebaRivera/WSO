package co.winsportsonline.wso.datamodel;

/**
 * Created by Boris Muñoz on 07-05-14.
 */
public class User implements DataModel {

    private String userId;
    private String account;
    private String email;
    private Social social;
    private String first_name;
    private String last_name;
    private String username;
    private String password;
//    private String metadata;

    @DataMember(member = "_id")
    public String getUserId() {
        return userId;
    }

    @DataMember(member = "_id")
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @DataMember(member = "account")
    public String getAccount() {
        return account;
    }

    @DataMember(member = "account")
    public void setAccount(String account) {
        this.account = account;
    }

    @DataMember(member = "email")
    public String getEmail() {
        return email;
    }

    @DataMember(member = "email")
    public void setEmail(String email) {
        this.email = email;
    }

    @DataMember(member = "social")
    public Social getSocial() {
        return social;
    }

    @DataMember(member = "social")
    public void setSocial(Social social) {
        this.social = social;
    }

    @DataMember(member = "first_name")
    public String getFirstName() {
        return first_name;
    }

    @DataMember(member = "first_name")
    public void setFirstName(String first_name) {
        this.first_name = first_name;
    }

    @DataMember(member = "last_name")
    public String getLastName() {
        return last_name;
    }

    @DataMember(member = "last_name")
    public void setLastName(String last_name) {
        this.last_name = last_name;
    }

    @DataMember(member = "username")
    public String getUsername() {
        return username;
    }

    @DataMember(member = "username")
    public void setUsername(String username) {
        this.username = username;
    }

    @DataMember(member = "password")
    public String getPassword() {
        return password;
    }

    @DataMember(member = "password")
    public void setPassword(String password) {
        this.password = password;
    }
}
