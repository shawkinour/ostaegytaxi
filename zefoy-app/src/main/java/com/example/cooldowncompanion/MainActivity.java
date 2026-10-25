package com.example.cooldowncompanion;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public final class MainActivity extends Activity {
    static final String CHANNEL = "zefoy_cooldown";
    static final String[] SERVICES = {"Views", "Likes", "Comment Hearts", "Shares", "Favorites"};
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int DARK = Color.rgb(20, 26, 43);
    private final int PRIMARY = Color.rgb(69, 78, 218);
    private final int WHITE = Color.WHITE;
    private SharedPreferences prefs;
    private EditText link, seconds;
    private TextView countdown, status, info;
    private Spinner picker;
    private int selected = 0;
    private boolean loading = false;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 1000);
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("cooldown", MODE_PRIVATE);
        selected = prefs.getInt("selected", 0);
        if (selected < 0 || selected >= SERVICES.length) selected = 0;
        createChannel();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 123);
        buildScreen();
    }
    static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL, "Cooldown reminders", NotificationManager.IMPORTANCE_DEFAULT);
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        }
    }
    private void createChannel() { createChannel(this); }
    private int dp(int v) { return Math.round(getResources().getDisplayMetrics().density * v); }
    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }
    private TextView text(String s, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s); v.setTextSize(size); v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }
    private void add(LinearLayout root, View child, int height, int top) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, height < 0 ? height : dp(height));
        params.topMargin=dp(top);
        root.addView(child, params);
    }
    private Button button(String title, int color) {
        Button b = new Button(this);
        b.setText(title);
        b.setAllCaps(false);
        b.setTextColor(WHITE);
        b.setTextSize(15);
        b.setBackground(shape(color, 12));
        return b;
    }
    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(246, 247, 251));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(28),dp(20),dp(30));
        scroll.addView(root);
        setContentView(scroll);
        TextView title = text("Zefoy Companion", 27, DARK, true);
        add(root,title,-2,0);
        TextView subtitle = text("Unofficial mobile cooldown assistant • no automatic engagement",12,Color.DKGRAY,false);
        add(root,subtitle,-2,6);

        add(root,text("Select a service",15,DARK,true),-2,22);
        picker = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,SERVICES);
        picker.setAdapter(adapter);
        picker.setBackground(shape(WHITE,10));
        picker.setPadding(dp(10),0,dp(10),0);
        add(root,picker,52,9);

        add(root,text("TikTok video URL",15,DARK,true),-2,17);
        link = new EditText(this);
        link.setSingleLine(true);
        link.setTextSize(14);
        link.setHint("https://www.tiktok.com/@user/video/...");
        link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        link.setPadding(dp(12),dp(8),dp(12),dp(8));
        link.setBackground(shape(WHITE,10));
        add(root,link,56,9);

        add(root,text("Zefoy wait time (seconds)",15,DARK,true),-2,17);
        seconds = new EditText(this);
        seconds.setSingleLine(true);
        seconds.setTextSize(17);
        seconds.setInputType(InputType.TYPE_CLASS_NUMBER);
        seconds.setPadding(dp(12),dp(8),dp(12),dp(8));
        seconds.setBackground(shape(WHITE,10));
        add(root,seconds,54,9);

        LinearLayout timerBox=new LinearLayout(this);
        timerBox.setOrientation(LinearLayout.VERTICAL);
        timerBox.setGravity(Gravity.CENTER);
        timerBox.setPadding(dp(12),dp(20),dp(12),dp(20));
        timerBox.setBackground(shape(WHITE,16));
        countdown=text("00:00",42,PRIMARY,true);
        countdown.setGravity(Gravity.CENTER);
        timerBox.addView(countdown);
        status=text("No active timer",13,Color.DKGRAY,false);
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams stats = new LinearLayout.LayoutParams(-1,-2);
        stats.topMargin=dp(5);timerBox.addView(status,stats);
        add(root,timerBox,144,20);

        Button save=button("Save link",DARK);
        add(root,save,52,16);
        Button start=button("Start cooldown reminder",PRIMARY);
        add(root,start,54,10);
        Button open=button("Open Zefoy in browser",Color.rgb(25,125,106));
        add(root,open,54,10);
        Button copy=button("Copy TikTok link",Color.rgb(102,111,138));
        add(root,copy,50,10);
        Button cancel=button("Cancel timer",Color.rgb(164,70,75));
        add(root,cancel,48,10);
        info=text("CAPTCHA remains manual. The timer is a reminder only; it cannot shorten Zefoy's server-side cooldown. Android may delay notifications to save battery.",12,Color.GRAY,false);
        add(root,info,-2,18);

        loading=true;
        picker.setSelection(selected);
        loadService();
        loading=false;
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){
                if(loading||pos==selected)return;
                saveDraft();
                selected=pos;
                prefs.edit().putInt("selected",selected).apply();
                loadService();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
        save.setOnClickListener(v -> saveDraftWithToast());
        start.setOnClickListener(v -> startTimer());
        open.setOnClickListener(v -> {
            saveDraft();
            try { startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://zefoy.com/"))); }
            catch(Exception ex){ toast("Install a browser to open Zefoy"); }
        });
        copy.setOnClickListener(v -> {
            String s=link.getText().toString().trim();
            if(!validUrl(s)){link.setError("Paste a valid TikTok https link");return;}
            ((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("TikTok URL",s));
            toast("TikTok link copied");
        });
        cancel.setOnClickListener(v -> cancelTimer());
        handler.post(ticker);
    }
    private boolean validUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme()) && host!=null &&
              (host.equalsIgnoreCase("tiktok.com") || host.toLowerCase(Locale.ROOT).endsWith(".tiktok.com"));
        }catch(Exception e){return false;}
    }
    private void saveDraft(){
        prefs.edit().putString("url_"+selected,link.getText().toString().trim())
          .putString("seconds_"+selected,seconds.getText().toString().trim()).apply();
    }
    private void saveDraftWithToast(){
        String s=link.getText().toString().trim();
        if(!s.isEmpty()&&!validUrl(s)){link.setError("Enter a valid HTTPS TikTok link");return;}
        saveDraft();toast("Saved on your phone");
    }
    private void loadService(){
        link.setText(prefs.getString("url_"+selected,""));
        seconds.setText(prefs.getString("seconds_"+selected,"120"));
        refresh();
    }
    private PendingIntent alarm(int i) {
        Intent intent=new Intent(this,ReminderReceiver.class);
        intent.putExtra("service",i);
        return PendingIntent.getBroadcast(this,700+i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void startTimer(){
        String url=link.getText().toString().trim();
        if(!url.isEmpty()&&!validUrl(url)){link.setError("Enter a valid HTTPS TikTok URL");return;}
        long value;
        try { value=Long.parseLong(seconds.getText().toString().trim()); }
        catch(Exception e){seconds.setError("Enter seconds");return;}
        if(value<1||value>86400){seconds.setError("Enter 1 to 86400 seconds");return;}
        saveDraft();
        long deadline=System.currentTimeMillis()+value*1000L;
        prefs.edit().putLong("deadline_"+selected,deadline).apply();
        AlarmManager mgr=(AlarmManager)getSystemService(ALARM_SERVICE);
        try { mgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,deadline,alarm(selected)); }
        catch(Exception e){toast("Notification scheduling unavailable");}
        refresh();toast("Reminder started for "+SERVICES[selected]);
    }
    private void cancelTimer(){
        prefs.edit().remove("deadline_"+selected).apply();
        ((AlarmManager)getSystemService(ALARM_SERVICE)).cancel(alarm(selected));
        refresh();toast("Timer cancelled");
    }
    private void refresh(){
        if(countdown==null)return;
        long deadline=prefs.getLong("deadline_"+selected,0);
        long remaining=Math.max(0,(deadline-System.currentTimeMillis()+999)/1000);
        countdown.setText(String.format(Locale.ROOT,"%02d:%02d:%02d",remaining/3600,(remaining/60)%60,remaining%60));
        status.setText(deadline==0?"No active cooldown":remaining>0?"Waiting for "+SERVICES[selected]:"Wait finished — check Zefoy manually");
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
    @Override public void onPause(){if(link!=null)saveDraft();super.onPause();}
    @Override public void onDestroy(){handler.removeCallbacks(ticker);super.onDestroy();}
}
