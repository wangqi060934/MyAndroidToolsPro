package cn.wq.myandroidtoolspro.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cn.wq.myandroidtoolspro.model.ComponentEntry;

public class DBHelper extends SQLiteOpenHelper {
	private static final String DB_NAME = "actions.db";
	private static final int DB_VERSION = 10;
	public static final String ACTION_TABLE_NAME = "action";
	public static final String RECEIVER_TABLE_NAME = "receiver";
	public static final String A_R_TABLE_NAME = "a_r";
	public static final String APPS_TABLE_NAME = "apps";

//	public final static String[] ACTION_TABLE_COLUMNS = { "_id", "action_name"};
	public final static String[] RECEIVER_TABLE_COLUMNS = { "_id", "app_name",
			"package_name", "class_name", "all_actions", "enabled" };
	public final static String[] A_R_TABLE_COLUMNS = { "_id", "action_id",
			"receiver_id" };
	public final static String[] APPS_TABLE_COLUMNS = { "_id", "app_name",
			"package_name", "is_system" };

	private DBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}
	
	private static DBHelper instance;
	public synchronized static DBHelper getInstance(Context context){
		if(instance==null){
			instance=new DBHelper(context);
		}
		return instance;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE IF NOT EXISTS apps (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "app_name TEXT,package_name TEXT,is_system INTEGER)");
		db.execSQL("CREATE TABLE IF NOT EXISTS receiver (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "app_name TEXT,package_name TEXT,class_name TEXT,all_actions TEXT,enabled INTEGER,UNIQUE(package_name,class_name) ON CONFLICT IGNORE)");
		db.execSQL("CREATE TABLE IF NOT EXISTS action (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "action_name TEXT,UNIQUE(action_name) ON CONFLICT IGNORE)");
		db.execSQL("CREATE TABLE IF NOT EXISTS a_r (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
				+ "action_id INTEGER,receiver_id INTEGER,FOREIGN KEY(action_id) REFERENCES action(_id),FOREIGN KEY(receiver_id) REFERENCES receiver(_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS uninstalled ( packageName TEXT PRIMARY KEY,"+
                "appName TEXT,icon BLOB,sourcePath TEXT,backupPath TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_manage (packageName TEXT PRIMARY KEY,appName TEXT,sourcePath TEXT,time LONG,enabled INTEGER)");
        db.execSQL("CREATE TABLE IF NOT EXISTS app_history (packageName TEXT PRIMARY KEY,d_time LONG,e_time LONG)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//        if (oldVersion < 9) {
//            db.execSQL("CREATE TABLE IF NOT EXISTS app_manage (packageName TEXT PRIMARY KEY,appName TEXT,sourcePath TEXT,time LONG,enabled INTEGER)");
//            db.execSQL("CREATE TABLE IF NOT EXISTS app_history (packageName TEXT PRIMARY KEY,d_time LONG,e_time LONG)");
//        }else if(oldVersion == 9){
//			db.execSQL("CREATE TABLE IF NOT EXISTS uninstalled ( packageName TEXT PRIMARY KEY,"+
//					"appName TEXT,icon BLOB,sourcePath TEXT,backupPath TEXT)");
//		}
		onCreate(db);

//        if(oldVersion==6){
//            db.execSQL("CREATE TABLE IF NOT EXISTS app_manage (packageName TEXT PRIMARY KEY,appName TEXT,sourcePath TEXT,time LONG,enabled INTEGER)");
//        }else if(oldVersion==7){
//            db.execSQL("CREATE TABLE IF NOT EXISTS app_history (packageName TEXT PRIMARY KEY,d_time LONG,e_time LONG)");
//        }else{
//            db.execSQL("DROP TABLE IF EXISTS " + ACTION_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + RECEIVER_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + A_R_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS " + APPS_TABLE_NAME);
//            db.execSQL("DROP TABLE IF EXISTS app_manage");
//            db.execSQL("DROP TABLE IF EXISTS app_history");
//            onCreate(db);
//        }
    }

	public static void updateReceiver(ComponentEntry entry, SQLiteDatabase db) {
		ContentValues values = new ContentValues();
		values.put(RECEIVER_TABLE_COLUMNS[5], entry.enabled ? 1 : 0);
		db.update(DBHelper.RECEIVER_TABLE_NAME, values,
				"class_name=? and package_name=?", new String[] {
						entry.className, entry.packageName });
	}

}
