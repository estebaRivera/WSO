package co.winsportsonline.wso.datamodel;

import java.util.Date;
import java.util.List;

/**
 * Created by Franklin Cruz on 06-03-14.
 */
public class LiveStream implements DataModel {



    private String liveStreamId;
    private String account;

    private String name;
    private String playerSkin;
    private String preferedProtocol;
    private Date recordingStartDate;
    private String streamId;
    private AccessRules accessRules;
    private ViewingTimeLimit viewingTimeLimit;
    //private EncondingProfiles encondingProfiles;
    private Date dateCreated;
    private List<String> cdnZones;
    private int views;
    private boolean recording;
    private boolean closedAccess;
    private boolean online;
    private ExternalCDN externalCDN;
    //private HashMap<NPI,NPI> entryPoints;
    private int recordingDateDiff;

    private TokenIssue tokenIssued;

    private List<LiveStreamSchedule> scheduledEvents;

    @DataMember(member = "_id")
    public String getLiveStreamId() {
        return liveStreamId;
    }

    @DataMember(member = "_id")
    public void setLiveStreamId(String liveStreamId) {
        this.liveStreamId = liveStreamId;
    }

    @DataMember(member = "account")
    public String getAccount() {
        return account;
    }

    @DataMember(member = "account")
    public void setAccount(String account) {
        this.account = account;
    }

    @DataMember(member = "name")
    public String getName() {
        return name;
    }

    @DataMember(member = "name")
    public void setName(String name) {
        this.name = name;
    }

    @DataMember(member = "player_skin")
    public String getPlayerSkin() {
        return playerSkin;
    }

    @DataMember(member = "player_skin")
    public void setPlayerSkin(String playerSkin) {
        this.playerSkin = playerSkin;
    }

    @DataMember(member = "preferred_protocol")
    public String getPreferedProtocol() {
        return preferedProtocol;
    }

    @DataMember(member = "preferred_protocol")
    public void setPreferedProtocol(String preferedProtocol) {
        this.preferedProtocol = preferedProtocol;
    }

    @DataMember(member = "recording_start_date")
    public Date getRecordingStartDate() {
        return recordingStartDate;
    }

    @DataMember(member = "recording_start_date")
    public void setRecordingStartDate(Date recordingStartDate) {
        this.recordingStartDate = recordingStartDate;
    }

    @DataMember(member = "stream_id")
    public String getStreamId() {
        return streamId;
    }

    @DataMember(member = "stream_id")
    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    @DataMember(member = "access_rules")
    public AccessRules getAccessRules() {
        return accessRules;
    }

    @DataMember(member = "access_rules")
    public void setAccessRules(AccessRules accessRules) {
        this.accessRules = accessRules;
    }

    @DataMember(member = "viewing_time_limit")
    public ViewingTimeLimit getViewingTimeLimit() {
        return viewingTimeLimit;
    }

    @DataMember(member = "viewing_time_limit")
    public void setViewingTimeLimit(ViewingTimeLimit viewingTimeLimit) {
        this.viewingTimeLimit = viewingTimeLimit;
    }

    @DataMember(member = "date_created")
    public Date getDateCreated() {
        return dateCreated;
    }

    @DataMember(member = "date_created")
    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    @DataMember(member = "cdn_zones")
    public List<String> getCdnZones() {
        return cdnZones;
    }

    @DataMember(member = "cdn_zones")
    public void setCdnZones(List<String> cdnZones) {
        this.cdnZones = cdnZones;
    }

    @DataMember(member = "views")
    public int getViews() {
        return views;
    }

    @DataMember(member = "views")
    public void setViews(int views) {
        this.views = views;
    }

    @DataMember(member = "recording")
    public boolean isRecording() {
        return recording;
    }

    @DataMember(member = "recording")
    public void setRecording(boolean recording) {
        this.recording = recording;
    }

    @DataMember(member = "closed_access")
    public boolean isClosedAccess() {
        return closedAccess;
    }

    @DataMember(member = "closed_access")
    public void setClosedAccess(boolean closedAccess) {
        this.closedAccess = closedAccess;
    }

    @DataMember(member = "online")
    public boolean isOnline() {
        return online;
    }

    @DataMember(member = "online")
    public void setOnline(boolean online) {
        this.online = online;
    }

    @DataMember(member = "external_cdn")
    public ExternalCDN getExternalCDN() {
        return externalCDN;
    }

    @DataMember(member = "external_cdn")
    public void setExternalCDN(ExternalCDN externalCDN) {
        this.externalCDN = externalCDN;
    }

    @DataMember(member = "recording_date_diff")
    public int getRecordingDateDiff() {
        return recordingDateDiff;
    }

    @DataMember(member = "recording_date_diff")
    public void setRecordingDateDiff(int recordingDateDiff) {
        this.recordingDateDiff = recordingDateDiff;
    }

    public TokenIssue getTokenIssued() {
        return tokenIssued;
    }

    public void setTokenIssued(TokenIssue tokenIssued) {
        this.tokenIssued = tokenIssued;
    }

    public List<LiveStreamSchedule> getScheduledEvents() {
        return scheduledEvents;
    }

    public void setScheduledEvents(List<LiveStreamSchedule> scheduledEvents) {
        this.scheduledEvents = scheduledEvents;
    }
}
