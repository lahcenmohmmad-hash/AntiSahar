package com.antisahar.app.ui;

import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.antisahar.app.R;
import com.antisahar.app.data.Prefs;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppsPickerActivity extends AppCompatActivity {

    private RecyclerView rv;
    private TextView tvCount;
    private Adapter adapter;
    private Prefs prefs;
    private Set<String> selected = new HashSet<>();

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_apps_picker);

        prefs = new Prefs(this);
        selected = prefs.getBlockedApps();

        rv = findViewById(R.id.rvApps);
        tvCount = findViewById(R.id.tvSelectedCount);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);

        updateCount();
        new LoadAppsTask().execute();
    }

    @Override
    protected void onPause() {
        super.onPause();
        prefs.setBlockedApps(selected);
    }

    private void updateCount() {
        tvCount.setText(getString(R.string.apps_selected, selected.size()));
    }

    static class AppEntry {
        final String pkg; final String label; final Drawable icon;
        AppEntry(String p, String l, Drawable i) { pkg = p; label = l; icon = i; }
    }

    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppEntry>> {
        @Override
        protected List<AppEntry> doInBackground(Void... voids) {
            PackageManager pm = getPackageManager();
            Intent main = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
            List<AppEntry> out = new ArrayList<>();
            String me = getPackageName();
            Set<String> seen = new HashSet<>();
            for (ResolveInfo ri : list) {
                ApplicationInfo ai = ri.activityInfo.applicationInfo;
                if (ai.packageName.equals(me)) continue;       // never list ourselves
                if (!seen.add(ai.packageName)) continue;
                String label;
                try { label = pm.getApplicationLabel(ai).toString(); }
                catch (Exception e) { label = ai.packageName; }
                Drawable icon = null;
                try { icon = pm.getApplicationIcon(ai); } catch (Exception ignored) {}
                out.add(new AppEntry(ai.packageName, label, icon));
            }
            Collections.sort(out, new Comparator<AppEntry>() {
                @Override public int compare(AppEntry a, AppEntry b) {
                    return a.label.compareToIgnoreCase(b.label);
                }
            });
            return out;
        }

        @Override
        protected void onPostExecute(List<AppEntry> appEntries) {
            adapter.submit(appEntries);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private final List<AppEntry> items = new ArrayList<>();
        void submit(List<AppEntry> data) {
            items.clear(); items.addAll(data); notifyDataSetChanged();
        }
        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_app, parent, false);
            return new VH(v);
        }
        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            final AppEntry e = items.get(position);
            h.tvName.setText(e.label);
            h.tvPkg.setText(e.pkg);
            if (e.icon != null) h.ivIcon.setImageDrawable(e.icon);
            h.sw.setChecked(selected.contains(e.pkg));
            h.itemView.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    boolean nowOn = !selected.contains(e.pkg);
                    if (nowOn) selected.add(e.pkg); else selected.remove(e.pkg);
                    h.sw.setChecked(nowOn);
                    updateCount();
                }
            });
        }
        @Override public int getItemCount() { return items.size(); }

        class VH extends RecyclerView.ViewHolder {
            final TextView tvName, tvPkg;
            final ImageView ivIcon;
            final MaterialSwitch sw;
            VH(View v) {
                super(v);
                tvName = v.findViewById(R.id.tvAppName);
                tvPkg  = v.findViewById(R.id.tvAppPkg);
                ivIcon = v.findViewById(R.id.ivAppIcon);
                sw     = v.findViewById(R.id.swPick);
            }
        }
    }
}
