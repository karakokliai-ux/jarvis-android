package com.jarvis.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    LinearLayout root, messages;
    EditText input;
    TextView status;
    Button send;
    ScrollView scroll;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    int bg = Color.rgb(7, 12, 25);
    int cyan = Color.rgb(0, 220, 255);

    int dp(float v) {
        return (int)(v * getResources().getDisplayMetrics().density);
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(12));
        root.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(0, dp(12), 0, dp(18));

        TextView title = new TextView(this);
        title.setText("J.A.R.V.I.S");
        title.setTextSize(30);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(cyan);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setText("● SYSTEM ONLINE");
        status.setTextColor(Color.rgb(80, 220, 160));
        status.setTextSize(12);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(6), 0, 0);

        header.addView(title);
        header.addView(status);
        root.addView(header);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(25, 65, 85));
        root.addView(line, new LinearLayout.LayoutParams(
            -1, dp(1)));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(12), 0, dp(12));

        scroll.addView(messages);

        root.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        bottom.setPadding(0, dp(12), 0, dp(4));

        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setTextSize(15);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        input.setHint("כתוב ל-JARVIS...");
        input.setPadding(dp(16), dp(12), dp(16), dp(12));
        input.setBackground(shape(
            Color.rgb(19, 30, 49), 24));

        bottom.addView(input, new LinearLayout.LayoutParams(
            0, -2, 1));

        send = new Button(this);
        send.setText("שלח");
        send.setTextColor(Color.BLACK);
        send.setTextSize(14);
        send.setAllCaps(false);
        send.setBackground(shape(cyan, 24));

        LinearLayout.LayoutParams bp =
            new LinearLayout.LayoutParams(dp(75), dp(50));
        bp.setMargins(dp(8), 0, 0, 0);

        bottom.addView(send, bp);
        root.addView(bottom);

        setContentView(root);

        send.setOnClickListener(v -> sendMessage());

        input.setOnEditorActionListener((v, action, event) -> {
            sendMessage();
            return true;
        });

        addMessage("שלום. אני JARVIS. המערכות מוכנות. במה אוכל לעזור?", false);
    }

    void addMessage(String text, boolean user) {

        LinearLayout row = new LinearLayout(this);
        row.setGravity(user ? Gravity.RIGHT : Gravity.LEFT);

        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setTextColor(Color.WHITE);
        bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
        bubble.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);

        bubble.setBackground(shape(
            user ? Color.rgb(12, 75, 100) :
            Color.rgb(24, 39, 60), 18));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(
                -2, -2);

        params.setMargins(
            0, dp(5), 0, dp(5));

        params.width = Math.min(
            getResources().getDisplayMetrics().widthPixels
            - dp(70), dp(310));

        bubble.setLayoutParams(params);

        row.addView(bubble);

        messages.addView(row);

        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    void sendMessage() {

        String question = input.getText().toString().trim();

        if (question.isEmpty()) return;

        input.setText("");

        addMessage(question, true);

        status.setText("● JARVIS THINKING...");
        status.setTextColor(cyan);

        send.setEnabled(false);

        executor.execute(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(
                    "http://127.0.0.1:8080/v1/chat/completions"
                );

                connection = (HttpURLConnection)
                    url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);

                connection.setRequestProperty(
                    "Content-Type", "application/json");

                JSONObject body = new JSONObject();
                body.put("model", "local-model");

                JSONArray messagesArray = new JSONArray();

                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content",
                    "אתה JARVIS, עוזר אישי מתקדם. " +
                    "דבר בעברית ברורה, היה חכם, מדויק, " +
                    "ידידותי ותמציתי.");

                messagesArray.put(system);

                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", question);

                messagesArray.put(user);

                body.put("messages", messagesArray);

                try (OutputStream os =
                    connection.getOutputStream()) {

                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = connection.getResponseCode();

                InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8"));

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                if (code < 200 || code >= 300) {
                    throw new Exception("שרת החזיר שגיאה " + code);
                }

                JSONObject response =
                    new JSONObject(result.toString());

                String answer = response
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

                runOnUiThread(() -> {
                    addMessage(answer, false);
                    status.setText("● SYSTEM ONLINE");
                    status.setTextColor(
                        Color.rgb(80, 220, 160));
                    send.setEnabled(true);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {
                    addMessage(
                        "שגיאת חיבור: " + e.getMessage(),
                        false);

                    status.setText("● CONNECTION ERROR");
                    status.setTextColor(Color.RED);
                    send.setEnabled(true);
                });

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
