package co.winsportsonline.wso.datamodel;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Franklin Cruz on 11-03-14.
 */
public class Media implements DataModel {

    private String mediaId;
    private Date dateRecorded;
    private String mediaDescription;
    private String slugTitle;
    private String title;
    private AccessRules accessRules;
    private Date dateCreated;
    private Date availableUntil;
    private Date availableFrom;
    private List<Thumbnail> thumbnails;
    //private NSArray *meta;
    private List<MediaCategory> categories;
    private List<String> tags;
    private int views;
    private int duration;
    private String status;
    private boolean isInitialized;
    private boolean isPublished;
    private List<Meta> meta = new ArrayList<Meta>();


    public boolean belongsToCategoryByName(String category) {
        try {
            for(int i = 0; i < categories.size(); ++i) {
                if(categories.get(i).getName().equals(category)) {
                    return true;
                }
            }
        }
        catch (Exception e) {

        }

        return false;
    }

    public boolean belongsToCategoryById(String id) {
        try {
            for(int i = 0; i < categories.size(); ++i) {
                if(categories.get(i).getCategoryId().equals(id)) {
                    return true;
                }
            }
        }
        catch (Exception e) {

        }

        return false;
    }

    public boolean belongsToCategoryByTag(String id) {
        try {
            for(int i = 0; i < tags.size(); ++i) {
                if(tags.get(i).equals(id)) {
                    return true;
                }
            }
        }
        catch (Exception e) {

        }

        return false;
    }
    public Thumbnail getDefaultThumbnail() {
        if (getThumbnails() != null) {
            for (Thumbnail t : getThumbnails()) {
                if (t.isDefault()) {
                    return t;
                }
            }
        }

        return null;
    }

    public Meta getMetaLabel(String label){
        for(int i = 0;i < meta.size();i++){
            if(meta.get(i).getLabel().equals(label)){
                return meta.get(i);
            }
        }
        return null;
    }

    @DataMember(member = "meta")
    public List<Meta> getMeta() {
        return meta;
    }

    @DataMember(member = "meta")
    public void setMeta(List<Meta> meta) {
        this.meta = meta;
    }

    @DataMember(member = "_id")
    public String getMediaId() {
        return mediaId;
    }

    @DataMember(member = "_id")
    public void setMediaId(String mediaId) {
        this.mediaId = mediaId;
    }

    @DataMember(member = "date_recorded")
    public Date getDateRecorded() {
        return dateRecorded;
    }

    @DataMember(member = "date_recorded")
    public void setDateRecorded(Date dateRecorded) {
        this.dateRecorded = dateRecorded;
    }

    @DataMember(member = "description")
    public String getMediaDescription() {
        return mediaDescription;
    }

    @DataMember(member = "description")
    public void setMediaDescription(String mediaDescription) {
        this.mediaDescription = mediaDescription;
    }

    @DataMember(member = "slug")
    public String getSlugTitle() {
        return slugTitle;
    }

    @DataMember(member = "slug")
    public void setSlugTitle(String slugTitle) {
        this.slugTitle = slugTitle;
    }

    @DataMember(member = "title")
    public String getTitle() {
        return title;
    }

    @DataMember(member = "title")
    public void setTitle(String title) {
        this.title = title;
    }

    //@DataMember(member = "access_rules")
    public AccessRules getAccessRules() {
        return accessRules;
    }

    //@DataMember(member = "access_rules")
    public void setAccessRules(AccessRules accessRules) {
        this.accessRules = accessRules;
    }

    @DataMember(member = "date_created")
    public Date getDateCreated() {
        return dateCreated;
    }

    @DataMember(member = "date_created")
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @DataMember(member = "available_until")
    public Date getAvailableUntil() {
        return availableUntil;
    }

    @DataMember(member = "available_until")
    public void setAvailableUntil(Date availableUntil) {
        this.availableUntil = availableUntil;
    }

    @DataMember(member = "availabel_from")
    public Date getAvailableFrom() {
        return availableFrom;
    }

    @DataMember(member = "availabel_from")
    public void setAvailableFrom(Date availableFrom) {
        this.availableFrom = availableFrom;
    }

    @DataMember(member = "thumbnails")
    public List<Thumbnail> getThumbnails() {
        return thumbnails;
    }

    @DataMember(member = "thumbnails")
    public void setThumbnails(List<Thumbnail> thumbnails) {
        this.thumbnails = thumbnails;
    }

    @DataMember(member = "categories")
    public List<MediaCategory> getCategories() {
        return categories;
    }

    @DataMember(member = "categories")
    public void setCategories(List<MediaCategory> categories) {
        this.categories = categories;
    }

    @DataMember(member = "tags")
    public List<String> getTags() {
        return tags;
    }

    @DataMember(member = "tags")
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @DataMember(member = "views")
    public int getViews() {
        return views;
    }

    @DataMember(member = "views")
    public void setViews(int views) {
        this.views = views;
    }

    @DataMember(member = "duration")
    public int getDuration() {
        return duration;
    }

    @DataMember(member = "duration")
    public void setDuration(int duration) {
        this.duration = duration;
    }

    @DataMember(member = "status")
    public String getStatus() {
        return status;
    }

    @DataMember(member = "status")
    public void setStatus(String status) {
        this.status = status;
    }

    @DataMember(member = "is_initialized")
    public boolean isInitialized() {
        return isInitialized;
    }

    @DataMember(member = "is_initialized")
    public void setInitialized(boolean isInitialized) {
        this.isInitialized = isInitialized;
    }

    @DataMember(member = "is_published")
    public boolean isPublished() {
        return isPublished;
    }

    @DataMember(member = "is_published")
    public void setPublished(boolean isPublished) {
        this.isPublished = isPublished;
    }
}
