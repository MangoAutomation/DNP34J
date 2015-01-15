package br.org.scadabr.dnp34j.master.session.database;

public class DataElement {
    private int index;
    private int group;
    private long timestamp;
    private int quality;
    private String value;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "DataElement [index=" + index + ", group=" + group + ", timestamp=" + timestamp + ", quality=" + quality
                + ", value=" + value + "]";
    }
}
