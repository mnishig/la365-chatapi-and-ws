package red.txn.la365_chatapi_and_test;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.google.gson.Gson;

import java.io.IOException;

import javax.annotation.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;


public class MainActivity extends AppCompatActivity {

    TextView tv;

    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.setupUI();

        client = new OkHttpClient.Builder().build();
    }


    //
    void setupUI() {
        tv = findViewById(R.id.textView);

        Button btn = findViewById(R.id.btnAvailable);
        btn.setOnClickListener(v -> {
            Log.d("Availability", "clicked");
            this.getAvailability();
        });

        btn = findViewById(R.id.btnGetSession);
        btn.setOnClickListener(v -> {
            Log.d("GetSession", "clicked");
            this.postSessions();;
        });

        btn = findViewById(R.id.btnEnd);
        btn.setOnClickListener(v -> {
            Log.d("end", "clicked");
        });

        btn = findViewById(R.id.btnTyping);
        btn.setOnClickListener(v -> {
            Log.d("typing", "clicked");
        });

        btn = findViewById(R.id.btnSend);
        btn.setOnClickListener(v -> {
            Log.d("send", "clicked");
            this.postTalk();
        });

        btn = findViewById(R.id.btnReceive);
        btn.setOnClickListener(v -> {
            Log.d("Receive", "clicked");
            this.getSessions();
        });
    }

    final String baseUrl = "https://service.ap1.liveassistfor365.com/api/chat/v0/23320646";
    final String token = "KWitfh7on3ssV0oZLjyeTcTSSHMfKWGmuQGM7VgGr00nDrwrIZvxTNGXfeQyEMQiGO521FI9G72o5KbSHxNBFyKh7jvMy6JtXtWjLnHZxC5563RTbC981b0k9pMazlRl";

    ChatInfo ci;

    void getAvailability() {
        String url = baseUrl + "/availability";

        Request req = new Request.Builder()
                            .url(url)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Authorization", "Bearer " + this.token)
                            .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Log.d("body: ", body);

            }

        });

    }

    // retrieve chat data to receive messages/state
    void postSessions() {
        String url = baseUrl + "/sessions";
        String jsonStr = "{}";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonStr) )
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Log.d("body: ", body);

                Gson g = new Gson();
                ci = g.fromJson(body, ChatInfo.class);

                MainActivity.this.setupWebsocket();
            }

        });
    }

    void getSessions() {
        String url = baseUrl + "/sessions/" + ci.sessionId + "?state=" + ci.state;

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token)
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.code() != 200) {
                    Log.d("talk: ", "fail sending!!!!!");
                }

                String json = response.body().string();

                Gson g = new Gson();
                ChatInformation info = g.fromJson(json, ChatInformation.class);

                for (ChatInformation.Event event : info.data.events) {
                    if (event.type.equals("line")) {
                        Log.d("receive: ", event.text);

                        // add to TextView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //noinspection SingleStatementInBlock
                                tv.append("\n" + event.text);
                            }
                        });

                    } else if (event.type.equals("state")) {
                        ci.state = event.state;
                    }
                } // end for
            }

        });
    }


    void postTalk() {
        String url = baseUrl + "/sessions/" + ci.sessionId + "/line";
        String jsonStr = "{\"line\":\"お客のアンドロイド\"}";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonStr) )
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.code() != 200) {
                    Log.d("talk: ", "fail sending!!!!!");
                }

            }

        });
    }

    void putVisitorTyping() {
        String url = baseUrl + "/sessions/" + ci.sessionId + "/visitorTyping";
        String jsonStr = "{\"visitorTyping\":true }";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token)
                .put(RequestBody.create(MediaType.parse("application/json"), jsonStr) )
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.code() != 200) {
                    Log.d("visitorTyping: ", "send error!!!!!");
                }
            }

        });
    }

    void postEnd() {
        String url = baseUrl + "/sessions/" + ci.sessionId + "/end";
        String jsonStr = "{}";

        Request req = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + this.token)
                .post(RequestBody.create(MediaType.parse("application/json"), jsonStr) )
                .build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (response.code() != 200) {
                    Log.d("end: ", "send error!!!!!");
                }
            }

        });

    }

    WebSocket ws;
    void setupWebsocket() {
        final String wsUrl = "wss://service.ap1.liveassistfor365.com/api/chat/v0/23320646/sessions/";
        final String path  = "/subscribe?state=";

        Request.Builder builder = new Request.Builder();

        Request req = builder.url(wsUrl + this.ci.sessionId + path + this.ci.state)
                            .addHeader("Authorization", "Bearer " + this.token)
                            .build();

        OkHttpClient client = new OkHttpClient().newBuilder().build();

        //client.newCall(req).enqueue(new Callback() {

        client.newWebSocket(req, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                super.onOpen(webSocket, response);
                Log.d("ws-open:", response.toString());
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                super.onMessage(webSocket, text);
                Log.d("ws-msg", text);

                Gson g = new Gson();
                ChatInformation info = g.fromJson(text, ChatInformation.class);

                for (ChatInformation.Event event : info.data.events) {
                    if (event.type.equals("line")) {
                        Log.d("receive: ", event.text);

                        // add to TextView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //noinspection SingleStatementInBlock
                                tv.append("\n" + event.text);
                            }
                        });

                    } else if (event.type.equals("state")) {
                        ci.state = event.state;
                    }
                } // end for


            }

            @Override
            public void onMessage(WebSocket webSocket, ByteString bytes) {
                super.onMessage(webSocket, bytes);
                Log.d("ws-msg(bytes)", "byteString");
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                super.onClosing(webSocket, code, reason);
                Log.d("ws-closing", reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                super.onClosed(webSocket, code, reason);
                Log.d("ws-closed", reason);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, @Nullable Response response) {
                super.onFailure(webSocket, t, response);
                Log.d("ws-closed", response.message());
            }
        });

        client.dispatcher().executorService().shutdown();


    }

}


class ChatInfo {
    public String sessionId;
    public String contextId;
    public String state;
}

class ChatInformation {

    class Event {
        public String type;
        public String time;
        public String state;
        public String text;
        public String textType;
        public String source;
        public String sentBy;
    }
    class Info {
        public String agentName;
        public boolean isAgentTyping;
        public String lastUpdate;
        public int chatTimeout;
        public String startTime;
    }

    class ChatData {
        public Event[] events;
        public Info info;
    }

    public ChatData data;
    public String state;
}