package name.boyle.chris.notificationsoncanvas;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.service.notification.StatusBarNotification;
import android.text.TextPaint;
import android.util.Log;

import com.pennas.pebblecanvas.plugin.PebbleCanvasPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by cmb on 12/09/13.
 * //startActivity();
 */
public class NotificationsOnCanvasPlugin extends PebbleCanvasPlugin
{
    protected static final int ID_NOTIFICATIONS = 1;
    public static final int WIDTH = 144;
    public static final int HEIGHT = 32;
    public static final String INTENT_NOTIFICATION_ACCESS =
            "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS";
    public static final int NOTIFICATION_ASK_PERMISSION = 1;
    public static final String LOG_TAG = "NotificationsOnCanvas";

    @Override
    protected ArrayList<PluginDefinition> get_plugin_definitions(Context context) {
        ArrayList<PluginDefinition> plugins = new ArrayList<PluginDefinition>();

        ImagePluginDefinition iplug = new ImagePluginDefinition();
        iplug.id = ID_NOTIFICATIONS;
        iplug.name = context.getString(R.string.plugin_name);
        plugins.add(iplug);

        return plugins;
    }

    @Override
    protected Bitmap get_bitmap_value(int def_id, Context context, String param)
    {
        if (def_id != ID_NOTIFICATIONS) return null;
        Log.d(LOG_TAG, "get_bitmap_value");

        Bitmap b = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);

        if (! NotificationListener.isEnabled) {
            Log.d(LOG_TAG, "get_bitmap_value: not enabled");
            askPermission(context);
            Paint paintSmall = new TextPaint();
            paintSmall.setColor(Color.WHITE);
            paintSmall.setTextSize(11);
            paintSmall.setTextAlign(Paint.Align.LEFT);
            c.drawText("need permission", 0, 11, paintSmall);
            Log.d(LOG_TAG, "get_bitmap_value: asked");
            return b;
        } else if (NotificationListener.needRefresh) {
            context.sendBroadcast(new Intent(NotificationListener.ACTION_REFRESH));
            return null;  // refresh will re-notify Canvas when done
        }

        int maxSize = HEIGHT;
        int x = 0;
        int y = 0;

        LinkedList<StatusBarNotification> notifications =
                new LinkedList<StatusBarNotification>(NotificationListener.notifications.values());
        Collections.reverse(notifications);  // newest first

        for (StatusBarNotification sbn : notifications) {
            if (sbn.getNotification().priority < Notification.PRIORITY_LOW) continue;

            Bitmap icon = getScaledIcon(context, sbn, maxSize);
            if (icon == null) continue;

            int w = icon.getWidth();
            int h = icon.getHeight();
            int iy = y + maxSize/2 - h/2;
            //Log.d(LOG_TAG, "got icon "+w+"x"+h+", drawing at "+x+","+iy);
            c.drawBitmap(icon, null, new Rect(x, iy, x + w, iy + h), null);
            x += icon.getWidth() + 1;
            if (x > WIDTH) break;
        }
        return b;
    }

    protected Bitmap getScaledIcon(Context context, StatusBarNotification sbn, int maxSize) {
        Drawable d;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(sbn.getPackageName().toString(), 0);
            d = pm.getResourcesForApplication(packageInfo.applicationInfo)
                    .getDrawable(sbn.getNotification().icon);
        } catch (Exception e) { return null; }
        if (d == null) return null;

        // Coerce to Bitmap - might already be a BitmapDrawable, but make
        // sure it's mutable and doesn't have unhelpful density info attached
        int iw = d.getIntrinsicWidth();
        int ih = d.getIntrinsicHeight();
        if (iw <= 0) { iw = maxSize; ih = maxSize; }
        Bitmap icon = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
        d.setBounds(0,0,iw,ih);
        d.draw(new Canvas(icon));

        // Scale it to maxSize pixels high
        int w, h;
        if (iw > ih) {
            w = maxSize;
            h = (int)Math.round((((double)ih)/iw)*w);
        } else {
            h = maxSize;
            w = (int)Math.round((((double)iw)/ih)*h);
        }
        return Bitmap.createScaledBitmap(icon, w, h, true);
    }

    protected static void askPermission(Context context) {
        Log.d(LOG_TAG, "askPermission");
        Intent i = new Intent(INTENT_NOTIFICATION_ACCESS);
        Notification n = new Notification.Builder(context)
                .setContentTitle(context.getString(R.string.notification_title))
                .setContentText(context.getString(R.string.notification_detail))
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setContentIntent(PendingIntent.getActivity(
                        context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT))
                .build();
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ASK_PERMISSION, n);
    }

    protected static void gotPermission(Context context) {
        Log.d(LOG_TAG, "gotPermission");
        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancel(NOTIFICATION_ASK_PERMISSION);
    }

    protected static void notifyChange(Context context) {
        Log.d(LOG_TAG, "notifyChange");
        notify_canvas_updates_available(ID_NOTIFICATIONS, context);
        Log.d(LOG_TAG, "notifyChange: done");
    }

    @Override
    protected String get_format_mask_value(int def_id, String format_mask, Context context, String param)
    {
        return null;
    }
}
