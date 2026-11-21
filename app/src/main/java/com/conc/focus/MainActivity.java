package com.conc.focus;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 140;
    private final int purple = Color.rgb(111, 70, 177);
    private final int dark = Color.rgb(35, 28, 49);
    private LinearLayout content;
    private TextView clockLabel;
    private final Handler handler = new Handler();
    private final Runnable updater = new Runnable() {
        @Override public void run() {
            if (clockLabel != null) {
                if (FocusState.isActive(MainActivity.this)) {
                    long seconds = (FocusState.remainingMillis(MainActivity.this) + 999L) / 1000L;
                    clockLabel.setText(String.format(java.util.Locale.KOREA,
                            "🔒 집중 중  %02d:%02d", seconds / 60, seconds % 60));
                } else if (wasRunning) {
                    wasRunning = false;
                    render();
                }
            }
            handler.postDelayed(this, 1000L);
        }
    };
    private boolean wasRunning = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(247, 242, 255));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        render();
    }
    @Override public void onResume() {
        super.onResume();
        render();
        handler.removeCallbacks(updater);
        handler.post(updater);
    }
    @Override public void onPause() {
        handler.removeCallbacks(updater);
        super.onPause();
    }

    private int dp(int x) {
        return Math.round(x * getResources().getDisplayMetrics().density);
    }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }
    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(null, android.graphics.Typeface.BOLD);
        return t;
    }
    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }
    private LinearLayout row() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }
    private void addGap(LinearLayout l, int height) {
        View spacer = new View(this);
        l.addView(spacer, new LinearLayout.LayoutParams(1, dp(height)));
    }
    private Button button(String s, boolean primary, Runnable onClick) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(14);
        b.setTextColor(primary ? Color.WHITE : dark);
        b.setBackground(background(primary ? purple : Color.rgb(234, 223, 247), 15));
        b.setPadding(dp(14), dp(12), dp(14), dp(12));
        b.setOnClickListener(v -> onClick.run());
        return b;
    }
    private void sectionTitle(LinearLayout parent, String title, String detail) {
        parent.addView(text(title, 19, dark, true));
        addGap(parent, 5);
        parent.addView(text(detail, 13, Color.DKGRAY, false));
        addGap(parent, 12);
    }

    private void render() {
        boolean active = FocusState.isActive(this);
        wasRunning = active;
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(249, 247, 252));
        content = column();
        content.setPadding(dp(20), dp(24), dp(20), dp(30));
        scroll.addView(content);
        setContentView(scroll);

        TextView brand = text("CONC  ✦", 29, purple, true);
        content.addView(brand);
        addGap(content, 4);
        content.addView(text("30분을 온전히 내 시간으로", 14, Color.DKGRAY, false));
        addGap(content, 20);

        LinearLayout statusBox = column();
        statusBox.setPadding(dp(18), dp(19), dp(18), dp(18));
        statusBox.setBackground(background(Color.rgb(236, 229, 250), 20));
        clockLabel = text("", 23, dark, true);
        statusBox.addView(clockLabel);
        addGap(statusBox, 5);
        statusBox.addView(text(active ? "30분 후 허용하지 않은 앱의 차단이 자동으로 풀려요." :
                "위젯을 한 번 누르면 바로 30분 시작!", 13, Color.DKGRAY, false));
        addGap(statusBox, 12);
        Button begin = button(active ? "집중 중 · 중단 버튼 없음" : "여기서도 30분 집중 시작",
                !active, () -> {
                    if (!FocusState.isServiceEnabled(this)) {
                        Toast.makeText(this, "접근성 권한부터 켜줘!", Toast.LENGTH_LONG).show();
                        return;
                    }
                    FocusState.begin(this);
                    Toast.makeText(this, "집중 모드 시작! 30분 동안 화이팅 💜", Toast.LENGTH_SHORT).show();
                    render();
                });
        begin.setEnabled(!active);
        statusBox.addView(begin);
        content.addView(statusBox);
        addGap(content, 24);

        sectionTitle(content, "① 사용 권한", "앱 사용 감지에 접근성 서비스가 필요해요. 화면 내용은 읽지 않아요.");
        boolean enabled = FocusState.isServiceEnabled(this);
        TextView permission = text(enabled ? "✓ 접근성 권한 연결됨" : "● 접근성 권한 설정 필요", 14,
                enabled ? Color.rgb(37, 116, 85) : Color.rgb(167, 71, 39), true);
        content.addView(permission);
        addGap(content, 9);
        content.addView(button("접근성 설정 열기", false, () -> {
            Intent settings = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(settings);
        }));
        addGap(content, 25);

        sectionTitle(content, "② 집중 중 허용할 앱 선택", active ?
                "집중 중에는 허용 목록을 변경할 수 없어요." :
                "체크한 앱만 사용 가능해요. 체크하지 않은 앱은 모두 차단돼요. 전화·설정 등 필수 기능은 예외예요.");

        Set<String> selected = FocusState.allowedApps(this);
        content.addView(text("집중 중 사용 허용: " + selected.size() + "개 · 체크하지 않은 앱은 차단", 13, purple, true));
        addGap(content, 12);
        List<ResolveInfo> apps = installedLauncherApps();
        if (apps.isEmpty()) {
            content.addView(text("선택 가능한 앱을 찾지 못했어요.", 13, Color.GRAY, false));
        }
        PackageManager pm = getPackageManager();
        for (ResolveInfo info : apps) {
            String pkg = info.activityInfo.packageName;
            if (FocusState.isProtected(pkg)) continue;
            LinearLayout entry = row();
            entry.setPadding(dp(8), dp(6), dp(8), dp(6));
            ImageView icon = new ImageView(this);
            try { icon.setImageDrawable(info.loadIcon(pm)); }
            catch (RuntimeException e) { icon.setImageResource(R.drawable.ic_conc); }
            LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(dp(34), dp(34));
            ip.rightMargin = dp(12);
            entry.addView(icon, ip);
            String name = String.valueOf(info.loadLabel(pm));
            TextView label = text(name, 14, dark, false);
            label.setSingleLine(true);
            label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            entry.addView(label, new LinearLayout.LayoutParams(0, dp(45), 1f));
            label.setGravity(Gravity.CENTER_VERTICAL);
            CheckBox box = new CheckBox(this);
            box.setButtonTintList(android.content.res.ColorStateList.valueOf(purple));
            box.setChecked(selected.contains(pkg));
            box.setEnabled(!active);
            box.setOnCheckedChangeListener((v, checked) -> {
                Set<String> change = FocusState.allowedApps(this);
                if (checked) change.add(pkg); else change.remove(pkg);
                FocusState.saveAllowedApps(this, change);
            });
            entry.addView(box);
            entry.setOnClickListener(v -> { if (!active) box.setChecked(!box.isChecked()); });
            content.addView(entry);
            View line = new View(this);
            line.setBackgroundColor(Color.rgb(234, 232, 238));
            content.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        }
        addGap(content, 25);

        sectionTitle(content, "③ 위젯 · 앱 사진", "갤러리에서 고른 사진이 1×1 위젯 배경이 돼요.");
        File photo = new File(getFilesDir(), "chosen_widget.png");
        if (photo.exists()) {
            ImageView preview = new ImageView(this);
            preview.setImageBitmap(BitmapFactory.decodeFile(photo.getAbsolutePath()));
            preview.setScaleType(ImageView.ScaleType.CENTER_CROP);
            LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(dp(80), dp(80));
            imageParams.bottomMargin = dp(10);
            content.addView(preview, imageParams);
        }
        content.addView(button("갤러리에서 사진 선택", true, () -> {
            Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            pick.setType("image/*");
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(pick, PICK_IMAGE);
        }));
        addGap(content, 9);
        content.addView(button("선택한 사진으로 홈 화면 앱 바로가기 만들기", false,
                this::pinPhotoShortcut));
        addGap(content, 12);
        content.addView(text("※ 원래 앱 아이콘 자체는 바뀌지 않지만, 사진 아이콘 바로가기를 추가할 수 있어요.",
                12, Color.GRAY, false));
        addGap(content, 26);

        sectionTitle(content, "④ 홈 화면 위젯 설치", "홈 화면 빈 곳을 길게 누른 다음 위젯 → CONC → 1×1을 선택해줘.");
        content.addView(text("설정 완료 후에는 위젯을 한 번 탭할 때 즉시 시작하며, 앱 안에는 중도 취소 기능이 없어요. "
                + "허용 목록에 없는 앱은 집중 중 차단돼요. 단, 안드로이드 설정에서 권한을 끄거나 앱을 삭제하는 것까지 막을 수는 없어요. "
                + "긴급 전화 기능은 계속 사용할 수 있어요.", 13, Color.DKGRAY, false));
        addGap(content, 20);
    }

    private List<ResolveInfo> installedLauncherApps() {
        PackageManager pm = getPackageManager();
        Intent launch = new Intent(Intent.ACTION_MAIN);
        launch.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> all = pm.queryIntentActivities(launch, 0);
        TreeMap<String, ResolveInfo> unique = new TreeMap<>();
        for (ResolveInfo info : all) {
            if (info.activityInfo == null) continue;
            String pkg = info.activityInfo.packageName;
            if (FocusState.isProtected(pkg)) continue;
            unique.putIfAbsent(pkg, info);
        }
        ArrayList<ResolveInfo> result = new ArrayList<>(unique.values());
        result.sort((a, b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(
                String.valueOf(b.loadLabel(pm))));
        return result;
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != PICK_IMAGE || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                BitmapFactory.decodeStream(in, null, bounds);
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            int sample = 1;
            while (bounds.outWidth / sample > 900 || bounds.outHeight / sample > 900) sample *= 2;
            options.inSampleSize = sample;
            Bitmap decoded;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                decoded = BitmapFactory.decodeStream(in, null, options);
            }
            if (decoded == null) throw new IllegalArgumentException("Cannot read selected photo");
            int side = Math.min(decoded.getWidth(), decoded.getHeight());
            Bitmap square = Bitmap.createBitmap(decoded,
                    (decoded.getWidth() - side) / 2, (decoded.getHeight() - side) / 2, side, side);
            Bitmap small = Bitmap.createScaledBitmap(square, 240, 240, true);
            try (FileOutputStream out = new FileOutputStream(
                    new File(getFilesDir(), "chosen_widget.png"))) {
                small.compress(Bitmap.CompressFormat.PNG, 95, out);
            }
            decoded.recycle();
            if (square != decoded) square.recycle();
            FocusWidget.updateAll(this);
            Toast.makeText(this, "위젯 사진을 바꿨어! 💜", Toast.LENGTH_SHORT).show();
            render();
        } catch (Exception e) {
            Toast.makeText(this, "사진을 읽지 못했어. 다른 사진을 골라줘.", Toast.LENGTH_LONG).show();
        }
    }

    private void pinPhotoShortcut() {
        File photo = new File(getFilesDir(), "chosen_widget.png");
        if (!photo.exists()) {
            Toast.makeText(this, "먼저 사진을 골라줘!", Toast.LENGTH_SHORT).show();
            return;
        }
        ShortcutManager manager = getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) {
            Toast.makeText(this, "이 홈 화면은 바로가기 추가를 지원하지 않아.", Toast.LENGTH_LONG).show();
            return;
        }
        Bitmap bitmap = BitmapFactory.decodeFile(photo.getAbsolutePath());
        if (bitmap == null) return;
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setAction(Intent.ACTION_MAIN);
        ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "conc_photo")
                .setShortLabel("CONC")
                .setLongLabel("CONC 집중 설정")
                .setIcon(Icon.createWithBitmap(bitmap))
                .setIntent(openApp)
                .build();
        manager.requestPinShortcut(shortcut, null);
    }
}
