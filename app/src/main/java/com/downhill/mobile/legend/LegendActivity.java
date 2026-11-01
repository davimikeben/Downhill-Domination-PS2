package com.downhill.mobile.legend;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.downhill.mobile.pad.PadTheme;
import com.downhill.mobile.pad.Ps2Button;
import com.downhill.mobile.ui.Ui;

/**
 * Every key of the pad, with the emoji drawn on it and a plain-Portuguese
 * explanation of what it does in Downhill Domination.
 */
public class LegendActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(build());
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(Ui.screenBackground(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 18), Ui.dp(this, 26), Ui.dp(this, 18), Ui.dp(this, 30));
        scroll.addView(root);

        root.addView(Ui.label(this, "Legenda das teclas", 27f, Ui.TEXT, true));

        TextView sub = Ui.label(this,
                "O emoji é o mesmo que aparece no teclado virtual, então dá para reconhecer "
                        + "a tecla sem decorar o controle do PS2.",
                13f, Ui.TEXT_DIM, false);
        sub.setLineSpacing(Ui.dp(this, 3), 1f);
        Ui.marginTop(sub, this, 6);
        root.addView(sub);

        addGroup(root, "Direcional", Ps2Button.Group.DPAD);
        addGroup(root, "Botões de ação", Ps2Button.Group.FACE);
        addGroup(root, "Gatilhos (ombro)", Ps2Button.Group.SHOULDER);
        addGroup(root, "Sistema e analógicos", Ps2Button.Group.SYSTEM);
        addGroup(root, "Extras deste app", Ps2Button.Group.UTILITY);

        TextView tip = Ui.label(this,
                "Dica: ✖️ pedala, 🟪 freia, 🔺 pula e 🔴 soca. O resto é manobra.",
                12.5f, Ui.ACCENT, true);
        Ui.marginTop(tip, this, 6);
        root.addView(tip);
        return scroll;
    }

    private void addGroup(LinearLayout root, String name, Ps2Button.Group group) {
        TextView header = Ui.cardHeader(this, name);
        Ui.marginTop(header, this, 20);
        root.addView(header);
        for (Ps2Button b : Ps2Button.of(group)) {
            root.addView(row(b));
        }
    }

    private View row(Ps2Button b) {
        LinearLayout l = Ui.row(this);
        l.setBackground(Ui.rounded(0xFF161C2A, 0xFF101520, 14f, this, 0x1AFFFFFF, 1f));
        l.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        Ui.marginBottom(l, this, 8);

        TextView emoji = Ui.label(this, b.emoji, 26f, PadTheme.faceColor(b), false);
        emoji.setGravity(Gravity.CENTER);
        emoji.setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(this, 44),
                LinearLayout.LayoutParams.WRAP_CONTENT));
        l.addView(emoji);

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        cp.leftMargin = Ui.dp(this, 12);
        col.setLayoutParams(cp);
        col.addView(Ui.label(this, b.shortLabel, 14.5f, Ui.TEXT, true));

        TextView hint = Ui.label(this, b.hint, 12.5f, Ui.TEXT_DIM, false);
        hint.setLineSpacing(Ui.dp(this, 2), 1f);
        col.addView(hint);
        l.addView(col);
        return l;
    }
}
