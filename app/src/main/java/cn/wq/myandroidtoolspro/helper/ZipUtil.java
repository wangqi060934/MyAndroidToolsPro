package cn.wq.myandroidtoolspro.helper;

import android.support.annotation.NonNull;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtil {
    public static void zip(@NonNull File[] _files, String zipFileName) throws Exception {
        //throw new ZipException("No entries")
        if (_files.length == 0) {
            File file = new File(zipFileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            return;
        }
        BufferedInputStream origin;
        FileOutputStream dest = new FileOutputStream(zipFileName);
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));
        int BUFFER = 1024;
        byte data[] = new byte[BUFFER];

        for (File _file : _files) {
            FileInputStream fi = new FileInputStream(_file);
            origin = new BufferedInputStream(fi, BUFFER);

            String path = _file.getAbsolutePath();
            ZipEntry entry = new ZipEntry(path.substring(path.lastIndexOf("/") + 1));
            out.putNextEntry(entry);
            int count;
            while ((count = origin.read(data, 0, BUFFER)) != -1) {
                out.write(data, 0, count);
            }
            origin.close();
        }

        out.close();
    }


    public static void unzip(String zipFile, File destDir) throws Exception {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zin = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipEntry ze;
            while ((ze = zin.getNextEntry()) != null) {
                File unzipFile = new File(destDir, ze.getName());
                if (ze.isDirectory()) {
                    if (!unzipFile.isDirectory()) {
                        unzipFile.mkdirs();
                    }
                } else {
                    FileOutputStream fout = new FileOutputStream(unzipFile.getAbsolutePath(), false);
                    try {
                        for (int c = zin.read(); c != -1; c = zin.read()) {
                            fout.write(c);
                        }
                        zin.closeEntry();
                    } finally {
                        fout.close();
                    }
                }
            }
        } finally {
            zin.close();
        }
    }
}
