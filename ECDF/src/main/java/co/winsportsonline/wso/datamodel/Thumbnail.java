package co.winsportsonline.wso.datamodel;

/**
 * Created by Franklin Cruz on 11-03-14.
 */
public class Thumbnail implements DataModel {

    private String url;
    private String name;
    private String thumbnailId;
    private String cdnZone;
    private boolean isDefault;

    @DataMember(member = "url")
    public String getUrl() {
        return url;
    }

    @DataMember(member = "url")
    public void setUrl(String url) {
        this.url = url;
    }

    @DataMember(member = "name")
    public String getName() {
        return name;
    }

    @DataMember(member = "name")
    public void setName(String name) {
        this.name = name;
    }

    @DataMember(member = "_id")
    public String getThumbnailId() {
        return thumbnailId;
    }

    @DataMember(member = "_id")
    public void setThumbnailId(String thumbnailId) {
        this.thumbnailId = thumbnailId;
    }

    @DataMember(member = "cdn_zone")
    public String getCdnZone() {
        return cdnZone;
    }

    @DataMember(member = "cdn_zone")
    public void setCdnZone(String cdnZone) {
        this.cdnZone = cdnZone;
    }

    @DataMember(member = "is_default")
    public boolean isDefault() {
        return isDefault;
    }

    @DataMember(member = "is_default")
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }
}
