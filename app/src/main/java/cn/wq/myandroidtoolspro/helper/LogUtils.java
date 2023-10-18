package cn.wq.myandroidtoolspro.helper;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

@Deprecated
public class LogUtils {
    public static void initLogging() {
        LogManager lm = LogManager.getLogManager();
        lm.reset();

        Logger logger = MatLogger.getInstsnce("MatPro");
        logger.addHandler(new MatHandler());
        lm.addLogger(logger);


    }
}

class MatLogger extends Logger{
    private volatile static MatLogger instance;
    private static final Object LOCK = new Object();

    /**
     * Protected method to construct a logger for a named subsystem.
     * <p>
     * The logger will be initially configured with a null Level
     * and with useParentHandlers set to true.
     *
     * @param name               A name for the logger.  This should
     *                           be a dot-separated name and should normally
     *                           be based on the package name or class name
     *                           of the subsystem, such as java.net
     *                           or javax.swing.  It may be null for anonymous Loggers.
     * @param resourceBundleName name of ResourceBundle to be used for localizing
     *                           messages for this logger.  May be null if none
     *                           of the messages require localization.
     * @throws MissingResourceException if the resourceBundleName is non-null and
     *                                  no corresponding resource can be found.
     */
    private MatLogger(String name, String resourceBundleName) {
        super(name, resourceBundleName);
    }

    public static MatLogger getInstsnce(String name) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new MatLogger(name, null);
                }
            }
        }
        return instance;
    }
}

class MatHandler extends Handler{



    public MatHandler() {
        setLevel(MatLevel.DEBUG);
        setFilter(null);
        setFormatter(new MatFormatter());
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) {
            return;
        }

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}

class MatLevel extends Level{
    /**INFO级别.*/
    public static final Level INFO = new MatLevel("INFO", 650);
    /**DEBUG级别.*/
    public static final Level DEBUG = new MatLevel("DEBUG", 620);

    protected MatLevel(String name, int value) {
        super(name, value);
    }

}

class MatFormatter extends Formatter{
    private final String lineSep = System.getProperty("line.separator");
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder(sdf.format(new Date()))
                .append(" ")
                .append(record.getLevel().getLocalizedName())
                .append(" - ")
                .append(formatMessage(record))
                .append(lineSep);
        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}