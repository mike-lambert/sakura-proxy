package com.cyfrant.orchidgate.updater;

public class Update {
    private long version;
    private String location;
    private String packageSignature;
    private String updateSignature;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPackageSignature() {
        return packageSignature;
    }

    public void setPackageSignature(String packageSignature) {
        this.packageSignature = packageSignature;
    }

    public String getUpdateSignature() {
        return updateSignature;
    }

    public void setUpdateSignature(String updateSignature) {
        this.updateSignature = updateSignature;
    }
}
