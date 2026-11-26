
package com.jarvis.ai;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.ViewGroup;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {

    LinearLayout layout, messages;
    EditText input;
    TextView status;
    ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 25, 20, 20);
        layout.setBackgroundColor(Color.rgb(5,11,24));

        TextView title = new TextView(this);
        title.setText("J.A.R.V.I.S");
        title.setTextSize(32);
        title.setTextColor(Color.CYAN);
        title.setGravity(Gravity.CENTER);
        layout.addView(title);

        status = new TextView(this);
        status.setText("המוח המקומי מוכן לחיבור");
        status.setTextColor(Color.LTGRAY);
        status.setGravity(Gravity.CENTER);
        layout.addView(status);

        ScrollView scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);

        layout.addView(scroll, new LinearLayout.LayoutParams(
            -1, 0, 1
        ));

        input = new EditText(this);
        input.setHint("כתוב ל-JARVIS...");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.GRAY);
        layout.addView(input);

        Button send = new Button(this);
        send.setText("שלח");
        layout.addView(send);

        send.setOnClickListener(v -> sendMessage());

        setContentView(layout);
        addMessage("שלום. אני JARVIS. במה אוכל לעזור?", false);
    }

    void addMessage(String text, boolean user) {
        TextView message = new TextView(this);
        message.setText(text);
        message.setTextSize(16);
        message.setTextColor(Color.WHITE);
        message.setPadding(15, 15, 15, 15);
        message.setBackgroundColor(user ?
            Color.rgb(18,61,80) : Color.rgb(16,44,67));

        LinearLayout.LayoutParams params =
            new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 8, 0, 8);
        messages.addView(message, params);
    }

    void sendMessage() {
        String question = input.getText().toString().trim();
        if (question.isEmpty()) return;

        input.setText("");
        addMessage(question, true);
        status.setText("JARVIS חושב...");

        executor.execute(() -> {
            try {
                URL url = new URL(
                    "http://127.0.0.1:8080/v1/chat/completions"
                );

                HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("POST");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(60000);
                connection.setDoOutput(true);
                connection.setRequestProperty(
                    "Content-Type", "application/json"
                );

                JSONObject body = new JSONObject();
                body.put("model", "local-model");

                JSONArray messagesArray = new JSONArray();

                JSONObject system = new JSONObject();
                system.put("role", "system");
                system.put("content",
                    "אתה JARVIS, עוזר אישי חכם. ענה בעברית.");
                messagesArray.put(system);

                JSONObject user = new JSONObject();
                user.put("role", "user");
                user.put("content", question);
                messagesArray.put(user);

                body.put("messages", messagesArray);

                try (OutputStream os = connection.getOutputStream()) {
                    os.write(body.toString().getBytes("UTF-8"));
                }

                int code = connection.getResponseCode();

                InputStream stream = code >= 200 && code < 300
                    ? connection.getInputStream()
                    : connection.getErrorStream();

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, "UTF-8")
                );

                StringBuilder result = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();

                if (code < 200 || code >= 300) {
                    throw new Exception("שגיאת שרת: " + code);
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
                    status.setText("JARVIS פעיל");
                });

                connection.disconnect();

            } catch (Exception e) {
                runOnUiThread(() -> {
                    addMessage("שגיאת חיבור: " + e.getMessage(), false);
                    status.setText("בעיית חיבור");
                });
            }
        });
    }
}
