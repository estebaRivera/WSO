package co.winsportsonline.wso.datamodel;

/**
 * Created by Franklin Cruz on 06-03-14.
 */
public class ExternalCDN implements DataModel {

    private boolean enabled;
    private String type;
    private String edgeUrl;

    @DataMember(member = "enabled")
    public boolean isEnabled() {
        return enabled;
    }

    @DataMember(member = "enabled")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @DataMember(member = "type")
    public String getType() {
        return type;
    }

    @DataMember(member = "type")
    public void setType(String type) {
        this.type = type;
    }

    @DataMember(member = "edgeUrl")
    public String getEdgeUrl() {
        return edgeUrl;
    }

    @DataMember(member = "edgeUrl")
    public void setEdgeUrl(String edgeUrl) {
        this.edgeUrl = edgeUrl;
    }
}
