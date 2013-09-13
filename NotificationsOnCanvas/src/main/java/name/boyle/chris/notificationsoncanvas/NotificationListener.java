package name.boyle.chris.notificationsoncanvas;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import java.util.LinkedHashMap;
import java.util.TreeMap;

/**
 * Created by cmb on 12/09/13.
 */
public class NotificationListener extends NotificationListenerService
{
    public static final String ACTION_REFRESH = "name.boyle.chris.notificationsoncanvas.REFRESH";
    public static boolean isEnabled = false, needRefresh = true;
    public static LinkedHashMap<String, StatusBarNotification> notifications =
            new LinkedHashMap<String, StatusBarNotification>();

    protected String getKey(StatusBarNotification sbn) {
        String k = sbn.getPackageName()+"/"+sbn.getId();
        String tag = sbn.getTag();
        if (tag != null) k += "/"+tag;
        return k;
    }

    @Override
    public IBinder onBind(Intent mIntent) {
        Log.d(NotificationsOnCanvasPlugin.LOG_TAG, "onBind");
        IBinder mIBinder = super.onBind(mIntent);
        NotificationsOnCanvasPlugin.gotPermission(this);
        notifications.clear();
        // can't call getActiveNotifications yet so do this silly receiver dance
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TreeMap<Long, StatusBarNotification> orderOfPosting =
                        new TreeMap<Long, StatusBarNotification>();
                for (StatusBarNotification sbn : getActiveNotifications()) {
                    orderOfPosting.put(sbn.getPostTime(), sbn);
                }
                for (StatusBarNotification sbn : orderOfPosting.values()) {
                    notifications.put(getKey(sbn), sbn);
                }
                NotificationsOnCanvasPlugin.notifyChange(NotificationListener.this);
                unregisterReceiver(this);
                needRefresh = false;
            }
        }, new IntentFilter(ACTION_REFRESH));
        isEnabled = true;
        needRefresh = true;
        return mIBinder;
    }

    @Override
    public boolean onUnbind(Intent mIntent) {
        Log.d(NotificationsOnCanvasPlugin.LOG_TAG, "onUnbind");
        isEnabled = false;
        notifications.clear();
        return super.onUnbind(mIntent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        Log.d(NotificationsOnCanvasPlugin.LOG_TAG, "onNotificationPosted");
        notifications.remove(getKey(sbn));  // force to front
        notifications.put(getKey(sbn), sbn);
        NotificationsOnCanvasPlugin.notifyChange(this);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(NotificationsOnCanvasPlugin.LOG_TAG, "onNotificationRemoved");
        notifications.remove(getKey(sbn));
        NotificationsOnCanvasPlugin.notifyChange(this);
    }
}
