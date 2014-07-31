package co.winsportsonline.wso.datamodel;

import java.util.List;

/**
 * Created by Franklin Cruz on 06-03-14.
 */
public class AccessRules implements DataModel {

    private Devices device;
    private Cellular cellular;
    private Geo geo;
    private ClosedAccess closedAccess;

    @DataMember(member = "devices")
    public Devices getDevice() {
        return device;
    }

    @DataMember(member = "devices")
    public void setDevice(Devices device) {
        this.device = device;
    }

    @DataMember(member = "cellular")
    public Cellular getCellular() {
        return cellular;
    }

    @DataMember(member = "cellular")
    public void setCellular(Cellular cellular) {
        this.cellular = cellular;
    }

    @DataMember(member = "geo")
    public Geo getGeo() {
        return geo;
    }

    @DataMember(member = "geo")
    public void setGeo(Geo geo) {
        this.geo = geo;
    }

    @DataMember(member = "closed_access")
    public ClosedAccess getClosedAccess() {
        return closedAccess;
    }

    @DataMember(member = "closed_access")
    public void setClosedAccess(ClosedAccess closedAccess) {
        this.closedAccess = closedAccess;
    }


    public static class Devices implements DataModel {

        private boolean denyMobile;
        private boolean denyDesktop;
        private boolean denyTV;

        @DataMember(member = "deny_mobile")
        public boolean isDenyMobile() {
            return denyMobile;
        }

        @DataMember(member = "deny_mobile")
        public void setDenyMobile(boolean denyMobile) {
            this.denyMobile = denyMobile;
        }

        @DataMember(member = "deny_desktop")
        public boolean isDenyDesktop() {
            return denyDesktop;
        }

        @DataMember(member = "deny_desktop")
        public void setDenyDesktop(boolean denyDesktop) {
            this.denyDesktop = denyDesktop;
        }

        @DataMember(member = "deny_tv")
        public boolean isDenyTV() {
            return denyTV;
        }

        @DataMember(member = "deny_tv")
        public void setDenyTV(boolean denyTV) {
            this.denyTV = denyTV;
        }
    }

    public static class Cellular implements DataModel {

        private boolean allow;
        private boolean enabled;

        @DataMember(member = "allow")
        public boolean isAllow() {
            return allow;
        }

        @DataMember(member = "allow")
        public void setAllow(boolean allow) {
            this.allow = allow;
        }

        @DataMember(member = "enabled")
        public boolean isEnabled() {
            return enabled;
        }

        @DataMember(member = "enabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Geo implements DataModel {

        private List<String> countries;
        private boolean allow;
        private boolean enabled;


        @DataMember(member = "countries")
        public List<String> getCountries() {
            return countries;
        }

        @DataMember(member = "countries")
        public void setCountries(List<String> countries) {
            this.countries = countries;
        }

        @DataMember(member = "allow")
        public boolean isAllow() {
            return allow;
        }

        @DataMember(member = "allow")
        public void setAllow(boolean allow) {
            this.allow = allow;
        }

        @DataMember(member = "enabled")
        public boolean isEnabled() {
            return enabled;
        }

        @DataMember(member = "enabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class ClosedAccess implements DataModel {

        private boolean enabled;

        @DataMember(member = "enabled")
        public boolean isEnabled() {
            return enabled;
        }

        @DataMember(member = "enabled")
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

}