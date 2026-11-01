package com.downhill.mobile.emu;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import com.downhill.mobile.rom.AppFilesProvider;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

/** Finds an installed PS2 core and builds the intent that opens the imported disc. */
public final class EmulatorLauncher {

    private EmulatorLauncher() {
    }

    /** Package names of every supported emulator that is actually installed. */
    public static Set<String> installedPackages(Context context) {
        Set<String> out = new LinkedHashSet<>();
        PackageManager pm = context.getPackageManager();
        for (String pkg : EmulatorTarget.allPackages()) {
            try {
                pm.getPackageInfo(pkg, 0);
                out.add(pkg);
            } catch (PackageManager.NameNotFoundException ignored) {
                // not installed
            }
        }
        return out;
    }

    /** First installed target in preference order, or null. */
    public static EmulatorTarget pick(Context context) {
        return EmulatorTarget.firstInstalled(installedPackages(context));
    }

    /** The installed package name for a target, or null. */
    public static String packageFor(EmulatorTarget target, Set<String> installed) {
        for (String p : target.packages) {
            if (installed.contains(p)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Intent that opens {@code iso} in {@code target}.
     *
     * <p>The image is shared through {@link AppFilesProvider}: emulators can not
     * read the private folder directly, and copying 2.7 GB into Downloads would
     * double the storage cost.</p>
     */
    public static Intent build(Context context, EmulatorTarget target, File iso) {
        if (target == null || iso == null || !iso.isFile()) {
            return null;
        }
        Set<String> installed = installedPackages(context);
        String pkg = packageFor(target, installed);
        if (pkg == null) {
            return null;
        }
        Uri uri = AppFilesProvider.uriFor(context, iso);
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri, "application/octet-stream");
        i.setPackage(pkg);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (target.kind == EmulatorTarget.Kind.RETROARCH) {
            i.putExtra("ROM", iso.getAbsolutePath());
            i.putExtra("CONTENT_URI", uri.toString());
        }
        return i;
    }

    /** Play Store page (with a search fallback) for the "install" button. */
    public static Intent storeIntent(Context context, EmulatorTarget target) {
        String pkg = target.packages.isEmpty() ? "" : target.packages.iterator().next();
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg));
        if (context.getPackageManager().resolveActivity(market, 0) != null) {
            return market;
        }
        return new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/search?q=" + target.label + " PS2 emulator apk"));
    }
}
