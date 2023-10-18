package cn.wq.myandroidtoolspro.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ReceiverWithActionEntry extends ComponentEntry implements Parcelable{
//	public Drawable icon;
	public String actions;
	public String appName;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(packageName);
		out.writeString(className);
		out.writeString(appName);
		out.writeInt(enabled?1:0);
		out.writeString(actions);
	}
	
	public static final Creator<ReceiverWithActionEntry> CREATOR=new Creator<ReceiverWithActionEntry>() {
		
		@Override
		public ReceiverWithActionEntry[] newArray(int size) {
			return new ReceiverWithActionEntry[size];
		}
		
		@Override
		public ReceiverWithActionEntry createFromParcel(Parcel in) {
			ReceiverWithActionEntry entry=new ReceiverWithActionEntry();
			entry.packageName=in.readString();
			entry.className=in.readString();
			entry.appName=in.readString();
			entry.enabled=in.readInt()==1;
			entry.actions=in.readString();
			return entry;
		}
	};
}
