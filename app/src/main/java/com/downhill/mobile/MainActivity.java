package com.downhill.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.downhill.mobile.emu.EmulatorLauncher;
import com.downhill.mobile.emu.EmulatorTarget;
import com.downhill.mobile.emu.TuningProfile;
import com.downhill.mobile.export.OverlayExporter;
import com.downhill.mobile.legend.LegendActivity;
import com.downhill.mobile.rom.IsoInfo;
import com.downhill.mobile.rom.RomStore;
import com.downhill.mobile.rom.ZipRomImporter;
import com.downhill.mobile.ui.Ui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Home screen: import the disc, launch it in an installed PS2 core, open the pad
 * lab / legend, export the pad and read the low-end-GPU tuning list.
 */
public class MainActivity extends Activity {

    private static final int REQ_PICK_ROM = 1001;
    private static final int REQ_PICK_PARTS = 1002;
    /** Release page that hosts the APK and the two CSO parts. */
    private static final String RELEASE_URL =
            "https://github.com/davimikeben/Downhill-Domination-PS2/releases/latest";

    private RomStore store;
    private ZipRomImporter importer;
    private TextView romStatus;
    private TextView emuStatus;
    private TextView playButton;
    private Dialog progressDialog;
    private ProgressBar progressBar;
    private TextView progressText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new RomStore(this);
        setContentView(buildScreen());
        refresh();
        verifyExistingImage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        if (importer != null) {
            importer.cancel();
        }
        dismissProgress();
        super.onDestroy();
    }

    // ------------------------------------------------------------------ UI --

    private View buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.screenBackground(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 26), Ui.dp(this, 18), Ui.dp(this, 28));
        scroll.addView(root);

        root.addView(header());
        root.addView(gameCard());
        root.addView(runCard());
        root.addView(padCard());
        root.addView(tuningCard());
        root.addView(footer());
        return scroll;
    }

    private View header() {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = Ui.dp(this, 20);
        h.setLayoutParams(lp);

        TextView tag = Ui.label(this, "PS2 · PORTÁTIL · TECLADO VIRTUAL", 11f, Ui.ACCENT, true);
        tag.setLetterSpacing(0.18f);
        h.addView(tag);

        TextView title = Ui.label(this, "DOWNHILL\nDOMINATION", 34f, Ui.TEXT, true);
        title.setLineSpacing(Ui.dp(this, 2), 1f);
        h.addView(title);

        TextView sub = Ui.label(this,
                "Bike de downhill, manobras e porrada. Este app monta o jogo, o pad com emoji "
                        + "e os ajustes para rodar em GPU fraca (Mali-G52).",
                13.5f, Ui.TEXT_DIM, false);
        sub.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.marginTop(sub, this, 8);
        h.addView(sub);
        return h;
    }

    private View gameCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.cardHeader(this, "1 · Arquivo do jogo"));

        romStatus = Ui.label(this, "", 13.5f, Ui.TEXT, false);
        romStatus.setLineSpacing(Ui.dp(this, 3), 1f);
        card.addView(romStatus);

        LinearLayout row = Ui.row(this);
        Ui.marginTop(row, this, 12);
        TextView pick = Ui.primaryButton(this, "📥  Importar ROM (.zip / .iso)");
        pick.setOnClickListener(v -> pickRom());
        row.addView(pick);
        card.addView(row);

        TextView download = Ui.secondaryButton(this, "⬇  Baixar o jogo do GitHub (2 partes)");
        Ui.marginTop(download, this, 10);
        download.setOnClickListener(v -> openRelease());
        card.addView(download);

        TextView join = Ui.secondaryButton(this, "🧩  Juntar as 2 partes baixadas");
        Ui.marginTop(join, this, 8);
        join.setOnClickListener(v -> pickParts());
        card.addView(join);

        TextView clear = Ui.label(this, "apagar a imagem importada", 12.5f, Ui.TEXT_DIM, false);
        clear.setPadding(0, Ui.dp(this, 12), 0, 0);
        clear.setOnClickListener(v -> confirmClear());
        card.addView(clear);
        return card;
    }

    private View runCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.cardHeader(this, "2 · Executar"));

        emuStatus = Ui.label(this, "", 13.5f, Ui.TEXT, false);
        emuStatus.setLineSpacing(Ui.dp(this, 3), 1f);
        card.addView(emuStatus);

        playButton = Ui.primaryButton(this, "▶  JOGAR");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 12);
        playButton.setLayoutParams(lp);
        playButton.setOnClickListener(v -> play());
        card.addView(playButton);

        LinearLayout row = Ui.row(this);
        Ui.marginTop(row, this, 10);
        TextView choose = Ui.secondaryButton(this, "⚙  Emulador / instalar núcleo");
        choose.setOnClickListener(v -> showEmulatorDialog());
        row.addView(choose);
        card.addView(row);
        return card;
    }

    private View padCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.cardHeader(this, "3 · Teclado virtual"));

        TextView body = Ui.label(this,
                "Pad completo do DualShock 2 com emoji em cada tecla, dois analógicos, "
                        + "gatilhos e vibração. Teste na tela cheia antes de jogar.",
                13.5f, Ui.TEXT_DIM, false);
        body.setLineSpacing(Ui.dp(this, 3), 1f);
        card.addView(body);

        LinearLayout row = Ui.row(this);
        Ui.marginTop(row, this, 12);
        TextView lab = Ui.primaryButton(this, "🎮  Testar teclado");
        lab.setOnClickListener(v -> startActivity(new Intent(this, PadLabActivity.class)));
        row.addView(lab);
        card.addView(row);

        LinearLayout row2 = Ui.row(this);
        Ui.marginTop(row2, this, 10);
        TextView legend = Ui.secondaryButton(this, "🗺  Legenda com emoji");
        legend.setOnClickListener(v -> startActivity(new Intent(this, LegendActivity.class)));
        row2.addView(legend);
        card.addView(row2);

        TextView export = Ui.secondaryButton(this, "🎨  Exportar pad para o RetroArch");
        Ui.marginTop(export, this, 10);
        export.setOnClickListener(v -> exportPad());
        card.addView(export);
        return card;
    }

    private View tuningCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.cardHeader(this, "4 · Otimização (Mali-G52)"));

        TextView body = Ui.label(this,
                "Ajustes que dão FPS de verdade numa GPU integrada: 1x nativo, OpenGL, MT-VU "
                        + "ligado e nada de filtro. Abra a lista e copie os valores.",
                13.5f, Ui.TEXT_DIM, false);
        body.setLineSpacing(Ui.dp(this, 3), 1f);
        card.addView(body);

        EmulatorTarget target = EmulatorLauncher.pick(this);
        final String id = target == null ? "armsx2" : target.id;

        LinearLayout row = Ui.row(this);
        Ui.marginTop(row, this, 12);
        TextView see = Ui.primaryButton(this, "⚡  Ver ajustes");
        see.setOnClickListener(v -> showTuning(id));
        row.addView(see);

        TextView copy = Ui.secondaryButton(this, "⧉  Copiar lista");
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(this, 10);
        copy.setLayoutParams(lp);
        copy.setOnClickListener(v -> copyTuning(id));
        row.addView(copy);
        card.addView(row);
        return card;
    }

    private View footer() {
        TextView t = Ui.label(this,
                "A imagem de 2,77 GB fica na memória privada do app (o zip de 2,42 GB não cabe "
                        + "num APK). O jogo roda no núcleo PS2 instalado no aparelho - é assim "
                        + "que os ports de PS2 para Android funcionam.",
                11.5f, 0xFF6C7791, false);
        t.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.marginTop(t, this, 8);
        return t;
    }

    // --------------------------------------------------------------- state --

    private void refresh() {
        boolean ready = store.isReady();
        if (romStatus != null) {
            if (ready) {
                String serial = store.serial();
                romStatus.setText("✅ Jogo pronto: " + store.gameFile().getName() + " · "
                        + gb(store.size()) + " GB"
                        + (store.isCompressed() ? " (comprimido, sem perda)" : " (ISO cru)")
                        + (TextUtils.isEmpty(serial) ? ""
                        : "\n🔑 Serial PS2: " + serial + "  (" + IsoInfo.regionOf(serial) + ")"));
            } else {
                long missing = store.missingSpace();
                if (missing > 0) {
                    romStatus.setText("⚠️ Faltam " + gb(missing) + " GB livres para importar a "
                            + "imagem de 2,77 GB.");
                } else {
                    romStatus.setText("❌ Nenhuma imagem importada. Toque em Importar e escolha o "
                            + "zip baixado do seu link (2,42 GB).");
                }
            }
        }
        if (emuStatus != null) {
            Set<String> installed = EmulatorLauncher.installedPackages(this);
            EmulatorTarget t = EmulatorTarget.firstInstalled(installed);
            emuStatus.setText(t == null
                    ? "❌ Nenhum núcleo PS2 instalado. Instale o ARMSX2 ou o AetherSX2 para o "
                    + "jogo abrir."
                    : "✅ Núcleo encontrado: " + t.label + "\n" + t.tuningNote);
        }
        if (playButton != null) {
            playButton.setAlpha(ready ? 1f : 0.45f);
        }
    }

    private void verifyExistingImage() {
        if (!store.isReady() || !TextUtils.isEmpty(store.serial())) {
            return;
        }
        new Thread(() -> {
            final IsoInfo info = store.sniffInfo();
            if (info == null) {
                return;
            }
            runOnUiThread(this::refresh);
        }).start();
    }

    private static String gb(long bytes) {
        return String.format(Locale.US, "%.2f", bytes / 1073741824.0);
    }

    // --------------------------------------------------------------- import --

    private void pickRom() {
        // No MIME filter on purpose: some file managers report a .zip as
        // "application/x-zip-compressed" or plain octet-stream, and a filtered
        // picker would hide the user's archive. The importer checks the content.
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        try {
            startActivityForResult(i, REQ_PICK_ROM);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum gerenciador de arquivos encontrado.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void openRelease() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASE_URL)));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, RELEASE_URL, Toast.LENGTH_LONG).show();
        }
    }

    private void pickParts() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        try {
            startActivityForResult(i, REQ_PICK_PARTS);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum gerenciador de arquivos encontrado.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void startJoin(Intent data) {
        final java.util.List<Uri> parts = new java.util.ArrayList<>();
        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                parts.add(clip.getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            parts.add(data.getData());
        }
        if (parts.isEmpty()) {
            return;
        }
        showProgress("Juntando as partes do jogo...");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        importer = new ZipRomImporter(this, store);
        importer.startJoin(parts, importCallback());
    }

    private ZipRomImporter.Callback importCallback() {
        return new ZipRomImporter.Callback() {
            @Override
            public void onProgress(long done, long total, String stage) {
                if (progressBar == null) {
                    return;
                }
                if (total > 0) {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(1000);
                    progressBar.setProgress((int) (1000L * done / total));
                    progressText.setText(stage + "\n" + gb(done) + " / " + gb(total) + " GB");
                } else {
                    progressBar.setIndeterminate(true);
                    progressText.setText(stage + "\n" + gb(done) + " GB");
                }
            }

            @Override
            public void onFinished(IsoInfo info, String error) {
                dismissProgress();
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (error != null) {
                    Toast.makeText(MainActivity.this, error, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, info != null && info.downhillDomination
                                    ? "Jogo completo: " + info.serial + " (" + info.region + ")"
                                    : "Arquivos juntados. Confira se é o jogo certo.",
                            Toast.LENGTH_LONG).show();
                }
                refresh();
            }
        };
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            return;
        }
        if (requestCode == REQ_PICK_PARTS) {
            startJoin(data);
            return;
        }
        if (requestCode != REQ_PICK_ROM) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            return;
        }
        startImport(uri);
    }

    private void startImport(Uri uri) {
        showProgress("Preparando a importação...");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        importer = new ZipRomImporter(this, store);
        importer.start(uri, importCallback());
    }

    private void confirmClear() {
        if (!store.isReady()) {
            Toast.makeText(this, "Nada para apagar.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Apagar a imagem?")
                .setMessage("Libera " + gb(store.size()) + " GB. Você poderá importar de novo.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (d, w) -> {
                    store.clear();
                    refresh();
                })
                .show();
    }

    // --------------------------------------------------------------- launch --

    private void play() {
        if (!store.isReady()) {
            Toast.makeText(this, "Importe a imagem do jogo primeiro.", Toast.LENGTH_LONG).show();
            return;
        }
        EmulatorTarget target = EmulatorLauncher.pick(this);
        if (target == null) {
            showEmulatorDialog();
            return;
        }
        Intent intent = EmulatorLauncher.build(this, target, store.gameFile());
        if (intent == null) {
            Toast.makeText(this, "O " + target.label + " não aceitou a imagem.", Toast.LENGTH_LONG)
                    .show();
            return;
        }
        try {
            startActivity(intent);
            Toast.makeText(this, "Abrindo no " + target.label + "...", Toast.LENGTH_SHORT).show();
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "Não foi possível abrir o " + target.label
                    + ". Configure o núcleo dentro do emulador.", Toast.LENGTH_LONG).show();
        }
    }

    private void showEmulatorDialog() {
        Set<String> installed = EmulatorLauncher.installedPackages(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 16), Ui.dp(this, 20), Ui.dp(this, 8));

        TextView intro = Ui.label(this, installed.isEmpty()
                ? "Instale um núcleo PS2. O ARMSX2 é o mais rápido em Mali-G52."
                : "Núcleos instalados:", 13.5f, Ui.TEXT_DIM, false);
        intro.setLineSpacing(Ui.dp(this, 3), 1f);
        box.addView(intro);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Emulador")
                .setView(box)
                .setNegativeButton("Fechar", null)
                .create();

        for (EmulatorTarget t : EmulatorTarget.ALL) {
            boolean has = t.isInstalled(installed);
            TextView b = has ? Ui.primaryButton(this, "▶  Abrir no " + t.label)
                    : Ui.secondaryButton(this, "⬇  Instalar " + t.label);
            Ui.marginTop(b, this, 10);
            b.setOnClickListener(v -> {
                dialog.dismiss();
                if (has) {
                    Intent i = EmulatorLauncher.build(this, t, store.gameFile());
                    if (i != null) {
                        try {
                            startActivity(i);
                        } catch (android.content.ActivityNotFoundException ignored) {
                            Toast.makeText(this, "Falha ao abrir " + t.label,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    try {
                        startActivity(EmulatorLauncher.storeIntent(this, t));
                    } catch (android.content.ActivityNotFoundException ignored) {
                        Toast.makeText(this, "Abra a loja e procure " + t.label,
                                Toast.LENGTH_LONG).show();
                    }
                }
            });
            box.addView(b);
        }
        dialog.show();
    }

    // --------------------------------------------------------------- export --

    private void exportPad() {
        Toast.makeText(this, "Gerando o pad...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                File[] files = OverlayExporter.export(this);
                runOnUiThread(() -> {
                    Toast.makeText(this, "3 arquivos prontos em files/export",
                            Toast.LENGTH_LONG).show();
                    startActivity(OverlayExporter.shareIntent(this, files));
                });
            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(this, "Falha ao exportar: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    // --------------------------------------------------------------- tuning --

    private void showTuning(String emulatorId) {
        List<TuningProfile.Setting> rows = TuningProfile.forTarget(emulatorId);
        StringBuilder sb = new StringBuilder();
        for (TuningProfile.Setting s : rows) {
            sb.append("• ").append(s.where).append(" › ").append(s.what)
                    .append("\n   = ").append(s.value).append("\n   (").append(s.why).append(")\n\n");
        }
        TextView tv = Ui.label(this, sb.toString(), 12.5f, Ui.TEXT, false);
        tv.setTextIsSelectable(true);
        tv.setLineSpacing(Ui.dp(this, 3), 1f);
        ScrollView scroll = new ScrollView(this);
        scroll.setPadding(Ui.dp(this, 18), Ui.dp(this, 14), Ui.dp(this, 18), Ui.dp(this, 8));
        scroll.addView(tv);
        new AlertDialog.Builder(this)
                .setTitle("Ajustes para GPU fraca")
                .setView(scroll)
                .setPositiveButton("Copiar", (d, w) -> copyTuning(emulatorId))
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void copyTuning(String emulatorId) {
        String text = TuningProfile.asText(emulatorId, TuningProfile.forTarget(emulatorId));
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("ajustes", text));
            Toast.makeText(this, "Lista copiada.", Toast.LENGTH_SHORT).show();
        }
    }

    // ------------------------------------------------------------- progress --

    private void showProgress(String message) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Ui.dp(this, 22), Ui.dp(this, 18), Ui.dp(this, 22), Ui.dp(this, 18));
        progressText = Ui.label(this, message, 13f, Ui.TEXT, false);
        progressText.setLineSpacing(Ui.dp(this, 3), 1f);
        box.addView(progressText);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setIndeterminate(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 8));
        lp.topMargin = Ui.dp(this, 14);
        progressBar.setLayoutParams(lp);
        box.addView(progressBar);

        progressDialog = new AlertDialog.Builder(this)
                .setTitle("Importando o jogo")
                .setView(box)
                .setNegativeButton("Cancelar", (d, w) -> {
                    if (importer != null) {
                        importer.cancel();
                    }
                })
                .setCancelable(false)
                .create();
        progressDialog.show();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        progressDialog = null;
        progressBar = null;
        progressText = null;
    }
}
