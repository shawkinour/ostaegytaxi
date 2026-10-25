package com.example.cooldowncompanion;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        int service = intent.getIntExtra("service",-1);
        if(service<0||service>=MainActivity.SERVICES.length) return;
        SharedPreferences prefs=context.getSharedPreferences("cooldown",Context.MODE_PRIVATE);
        long deadline=prefs.getLong("deadline_"+service,0L);
        if(deadline<=0||System.currentTimeMillis()+5000<deadline)return;
        prefs.edit().remove("deadline_"+service).apply();
        if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        MainActivity.createChannel(context);
        Intent launch=new Intent(context,MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent tap=PendingIntent.getActivity(context,1000+service,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT>=26
            ? new Notification.Builder(context,MainActivity.CHANNEL)
            : new Notification.Builder(context);
        Notification notification=builder
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Cooldown reminder")
            .setContentText(MainActivity.SERVICES[service]+": check Zefoy manually")
            .setAutoCancel(true).setContentIntent(tap).build();
        ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(200+service,notification);
    }
}
