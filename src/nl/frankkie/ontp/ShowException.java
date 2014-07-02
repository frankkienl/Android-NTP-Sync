package nl.frankkie.ontp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * Show Exception in Dialog
 *
 * @author FrankkieNL
 */
public class ShowException {

    public static boolean printExceptionsToLogCat = true;

    public static Handler handler = new Handler();

    public static void showExceptionFromNonUIThread(final Exception exception, final Context context) {
        handler.post(new Runnable() {

            public void run() {
                showException(exception, context);
            }
        });
    }

    /**
     * Show dialog with Stacktrace
     *
     * @param exception the Exception
     * @param context Activity-context to show dialog on
     */
    public static void showException(Exception exception, Context context) {
        if (printExceptionsToLogCat) {
            exception.printStackTrace();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(exception.getMessage());
        String stackTrace = getStackTrace(exception);
        builder.setMessage(stackTrace);
        builder.setPositiveButton("ok..", new android.content.DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                //do nothing, just remove dialog
            }
        });
        builder.create().show();
    }

    /**
     * Get Stacktrace as String
     *
     * @param aThrowable exception to stringify
     * @return stacktrace as string
     */
    public static String getStackTrace(Throwable aThrowable) {
        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        aThrowable.printStackTrace(printWriter);
        return result.toString();
    }
}
