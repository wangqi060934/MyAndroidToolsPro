package cn.wq.myandroidtoolspro.model;

public class BackupEntry extends ComponentEntry {
    public String appName;
    /**
     * @see cn.wq.myandroidtoolspro.helper.IfwUtil#COMPONENT_FLAG_ACTIVITY
     */
    public int cType;

    public boolean isSystem;
}