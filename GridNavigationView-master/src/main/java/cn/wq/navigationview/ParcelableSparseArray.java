package cn.wq.navigationview;


import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.os.ParcelableCompat;
import android.support.v4.os.ParcelableCompatCreatorCallbacks;
import android.util.SparseArray;

public class ParcelableSparseArray extends SparseArray<Parcelable> implements Parcelable {

    public ParcelableSparseArray() {
        super();
    }

    public ParcelableSparseArray(Parcel source, ClassLoader loader) {
        super();
        int size = source.readInt();
        int[] keys = new int[size];
        source.readIntArray(keys);
        Parcelable[] values = source.readParcelableArray(loader);
        for (int i = 0; i < size; ++i) {
            put(keys[i], values[i]);
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        int size = size();
        int[] keys = new int[size];
        Parcelable[] values = new Parcelable[size];
        for (int i = 0; i < size; ++i) {
            keys[i] = keyAt(i);
            values[i] = valueAt(i);
        }
        parcel.writeInt(size);
        parcel.writeIntArray(keys);
        parcel.writeParcelableArray(values, flags);
    }

    public static final Parcelable.Creator<ParcelableSparseArray> CREATOR =
            ParcelableCompat
                    .newCreator(new ParcelableCompatCreatorCallbacks<ParcelableSparseArray>() {
                        @Override
                        public ParcelableSparseArray createFromParcel(Parcel source,
                                                                      ClassLoader loader) {
                            return new ParcelableSparseArray(source, loader);
                        }

                        @Override
                        public ParcelableSparseArray[] newArray(int size) {
                            return new ParcelableSparseArray[size];
                        }
                    });
}
