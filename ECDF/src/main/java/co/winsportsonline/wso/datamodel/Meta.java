package co.winsportsonline.wso.datamodel;

/**
 * Created by Esteban- on 24-04-14.
 */
public class Meta implements DataModel  {
    private String id;
    private String label;
    private String aspect;
    private String status;
    private String url;
    private boolean is_original;

    public Meta() {

    }

    /*    public Resolution getResolution() {
            return resolution;
        }

        public void setResolution(Resolution resolution) {
            this.resolution = resolution;
        }
    */
    @DataMember(member = "_id")
    public String getId() {
        return id;
    }
    @DataMember(member = "_id")
    public void setId(String id) {
        this.id = id;
    }
    @DataMember(member = "label")
    public String getLabel() {
        return label;
    }
    @DataMember(member = "label")
    public void setLabel(String label) {
        this.label = label;
    }
    @DataMember(member = "aspect")
    public String getAspect() {
        return aspect;
    }
    @DataMember(member = "aspect")
    public void setAspect(String aspect) {
        this.aspect = aspect;
    }
    @DataMember(member = "status")
    public String getStatus() {
        return status;
    }
    @DataMember(member = "status")
    public void setStatus(String status) {
        this.status = status;
    }
    @DataMember(member = "url")
    public String getUrl() {
        return url;
    }
    @DataMember(member = "url")
    public void setUrl(String url) {
        this.url = url;
    }
    @DataMember(member = "is_original")
    public boolean isIs_original() {
        return is_original;
    }
    @DataMember(member = "is_original")
    public void setIs_original(boolean is_original) {
        this.is_original = is_original;
    }
}
