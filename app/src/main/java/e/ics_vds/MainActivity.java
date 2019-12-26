package e.ics_vds;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    final String pubTopic = "ICsVDS/nodemcu", subscriptionTopic = "ICsVDS/app", username = "mwgdyyop", password = "1s4lzSl27zSV";
    Button btnConnect, btnSend, btnFan;
    TextView tvConnStat;
    EditText etMsg;
    public MqttAndroidClient mqttAndroidClient;
    final String brokerUri = "tcp://m12.cloudmqtt.com:15080";
    String clientId;
    boolean isMqttConnected = false, isFanOn = false;
    ProgressDialog pd;
    final int TARGET_LCD = 0, TARGET_FAN = 1;
    UserPrefs userPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userPrefs = new UserPrefs(this);
        String clientId = userPrefs.getMqttClientId();
        if(clientId == null){
            Calendar calendar = Calendar.getInstance();
            clientId = "ICsVDSClient"+Integer.toString(new Random().nextInt(50))+Long.toString(calendar.getTimeInMillis());
            userPrefs.seMqttClientId(clientId);
        }
        mqttAndroidClient = new MqttAndroidClient(this, brokerUri, clientId);
        btnConnect = findViewById(R.id.btn_connect);
        btnSend = findViewById(R.id.btn_send);
        btnFan = findViewById(R.id.btn_fan);
        etMsg = findViewById(R.id.et_msg);
        tvConnStat = findViewById(R.id.tv_connStat);
        pd = new ProgressDialog(this);
        pd.setTitle("Connecting");
        pd.setMessage("Please wait ...");
        btnConnect.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnFan.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.btn_connect){
            if(!internetIsConnected()){
                Toast.makeText(MainActivity.this, "No Internet Connection", Toast.LENGTH_SHORT).show();
                return;
            }
            pd.show();
            connectMqtt();
        }
        if(view.getId() == R.id.btn_send){
            String msg = etMsg.getText().toString();
            if(!internetIsConnected() || !isMqttConnected){
                Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
                return;
            }
            if(msg == null || msg.length() == 0){
                Toast.makeText(this, "No message", Toast.LENGTH_SHORT).show();
                return;
            }
            sendMessage(msg, TARGET_LCD);
        }
        if(view.getId() == R.id.btn_fan){
            if(!internetIsConnected() || !isMqttConnected){
                Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
                return;
            }
            if(isFanOn){
                sendMessage("0", TARGET_FAN);
            }else{
                sendMessage("1", TARGET_FAN);
            }
        }
    }

    private void connectMqtt(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(false);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    initMqttCallbacks();
//                    subscribeToTopic();
                    isMqttConnected = true;
                    btnConnect.setVisibility(View.GONE);
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                    tvConnStat.setText("Status: Connected");
                    Log.e("Mqtt", "connected");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this, "Connection Error", Toast.LENGTH_SHORT).show();
                    Log.e("Mqtt", "Failed to connect to: " + brokerUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            pd.dismiss();
            ex.printStackTrace();
        }
    }

    private void initMqttCallbacks(){
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {

            }

            @Override
            public void connectionLost(Throwable throwable) {
                tvConnStat.setText("Status: Disconnected");
//                btnConnect.setText("Reconnect");
//                btnConnect.setVisibility(View.VISIBLE);
                isMqttConnected = false;
                Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
                Log.e("Mqtt", "diconnected");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) {
                Log.e("MQTT", mqttMessage.toString());
                String message = mqttMessage.toString();
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
    }

    private void sendMessage(final String payload, final int target){
        try{
            pd.setTitle("Sending");
            pd.show();
            MqttMessage message = new MqttMessage(payload.getBytes());
            mqttAndroidClient.publish(pubTopic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
//                    Log.e("MQTT", "message sent successfully");
                    pd.dismiss();
                    if(target == TARGET_FAN){
                        if(isFanOn){
                            isFanOn = false;
                            btnFan.setText("Turn Fan On");
                        }else{
                            isFanOn = true;
                            btnFan.setText("Turn Fan Off");
                        }
                    }else{
                        Toast.makeText(MainActivity.this, "Message Sent", Toast.LENGTH_SHORT).show();
                        etMsg.getText().clear();
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                    pd.dismiss();
                    Toast.makeText(MainActivity.this,"Could not send message", Toast.LENGTH_SHORT).show();
                    if(e.getMessage() != null){
                        Log.e("Message error", e.getMessage());
                    }else{
                        e.printStackTrace();
                    }
                }
            });
        }catch(MqttException e){
            pd.dismiss();
            Toast.makeText(this,"Something went wrong", Toast.LENGTH_SHORT).show();
            if(e.getMessage() != null){
                Log.e("Exception subscribing", e.getMessage());
            }else{
                e.printStackTrace();
            }
        }
    }

    private boolean internetIsConnected(){
        ConnectivityManager cm =
                (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        return isConnected;
    }


    private void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.e("Mqtt","Subscribed to topic!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            if(ex.getMessage() != null){
                Log.e("Exception subscribing", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }



    private void disconnectMQTT(){
        try{
            mqttAndroidClient.disconnectForcibly(2, 2);
            Log.e("force disconn", "success");
        }catch(MqttException ex){
            if(ex.getMessage() != null){
                Log.e("Error disconnecting", ex.getMessage());
            }else{
                ex.printStackTrace();
            }
        }
    }

}
