package co.winsportsonline.wso.datamodel;

import java.util.Date;

/**
 * Created by Franklin Cruz on 07-03-14.
 */
public class LiveStreamSchedule implements DataModel {

    private String eventId;
    private Date endDate;
    private Date startDate;
    private String name;
    private String code;
    private AccessRules accessRules;
    private boolean isAutoPublished;
    private boolean isFeatured;
    private boolean isForRecording;
    private boolean isCurrent;
    private boolean isPast;
    private boolean isFuture;

    private LiveStream stream;

    @DataMember(member = "_id")
    public String getEventId() {
        return eventId;
    }

    @DataMember(member = "_id")
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @DataMember(member = "date_end")
    public Date getEndDate() {
        return endDate;
    }

    @DataMember(member = "date_end")
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @DataMember(member = "date_start")
    public Date getStartDate() {
        return startDate;
    }

    @DataMember(member = "date_start")
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @DataMember(member = "name")
    public String getName() {
        return name;
    }

    @DataMember(member = "name")
    public void setName(String name) {
        this.name = name;
    }

    @DataMember(member = "code")
    public String getCode() {
        return code;
    }

    @DataMember(member = "code")
    public void setCode(String code) {
        this.code = code;
    }

    @DataMember(member = "is_auto_published")
    public boolean isAutoPublished() {
        return isAutoPublished;
    }

    @DataMember(member = "is_auto_published")
    public void setAutoPublished(boolean isAutoPublished) {
        this.isAutoPublished = isAutoPublished;
    }

    @DataMember(member = "is_featured")
    public boolean isFeatured() {
        return isFeatured;
    }

    @DataMember(member = "is_featured")
    public void setFeatured(boolean isFeatured) {
        this.isFeatured = isFeatured;
    }

    @DataMember(member = "for_recording")
    public boolean isForRecording() {
        return isForRecording;
    }

    @DataMember(member = "for_recording")
    public void setForRecording(boolean isForRecording) {
        this.isForRecording = isForRecording;
    }

    @DataMember(member = "is_current")
    public boolean isCurrent() {
        return isCurrent;
    }

    @DataMember(member = "is_current")
    public void setCurrent(boolean isCurrent) {
        this.isCurrent = isCurrent;
    }

    @DataMember(member = "is_past")
    public boolean isPast() {
        return isPast;
    }

    @DataMember(member = "is_past")
    public void setPast(boolean isPast) {
        this.isPast = isPast;
    }

    @DataMember(member = "is_future")
    public boolean isFuture() {
        return isFuture;
    }

    @DataMember(member = "is_future")
    public void setFuture(boolean isFuture) {
        this.isFuture = isFuture;
    }

    @DataMember(member = "access_rules")
    public AccessRules getAccessRules() {
        return accessRules;
    }

    @DataMember(member = "access_rules")
    public void setAccessRules(AccessRules accessRules) {
        this.accessRules = accessRules;
    }

    public LiveStream getStream() {
        return stream;
    }

    public void setStream(LiveStream stream) {
        this.stream = stream;
    }
}
