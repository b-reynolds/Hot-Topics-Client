package io.benreynolds.hottopics;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    private WebsocketClient mWebsocketClient = WebsocketClient.getInstance();

    private EditText txtUsername;
    private Button btnConnect;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUsername = findViewById(R.id.txtUsername);
        btnConnect = findViewById(R.id.btnConnect);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}