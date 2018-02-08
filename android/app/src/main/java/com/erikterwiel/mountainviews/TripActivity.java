package com.erikterwiel.mountainviews;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.scanner.ScanActivity;

public class TripActivity extends AppCompatActivity {

    public static final String TAG = "TripActivity.java";

    private DeviceListener mListener;
    private CountDownTimer mTimer;
    private int mActions = 0;
    private CognitoCachingCredentialsProvider mCredentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "onCreate() called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip);

        mCredentialsProvider = getCredProvider(this);

        new SendNotification().execute();

        mTimer = new CountDownTimer(5000, 0) {
            @Override
            public void onTick(long l) {}
            @Override
            public void onFinish() {
                if (mActions > 2) {
                    new SendNotification().execute();
                } else {
                    mActions = 0;
                }
            }
        };
        mListener = new AbstractDeviceListener() {
            @Override
            public void onConnect(Myo myo, long timestamp) {
                Toast.makeText(TripActivity.this, "Myo Connected!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onDisconnect(Myo myo, long timestamp) {
                Toast.makeText(TripActivity.this, "Myo Disconnected!", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onPose(Myo myo, long timestamp, Pose pose) {
                Log.i(TAG, "Pose: " + pose);
                if (pose == Pose.DOUBLE_TAP || pose == Pose.FIST) {
                    if (mActions == 0) mTimer.start();
                    mActions += 1;
                }
            }
        };
        Hub hub = Hub.getInstance();
        if (!hub.init(this)) {
            Log.e(TAG, "Could not initialize the Hub.");
            finish();
            return;
        }
        hub.addListener(mListener);

        if (getIntent().getBooleanExtra("myo", false)) {
            Intent intent = new Intent(this, ScanActivity.class);
            startActivity(intent);
        }
    }

    private class SendNotification extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... inputs) {
            AmazonSNSClient snsClient = new AmazonSNSClient(mCredentialsProvider);
            snsClient.setRegion(Region.getRegion(Regions.US_EAST_1));
            String msg = getIntent().getStringExtra("username") + " has triggered their " +
                    "Myo Armband distress signal! Send help to them at Latitude: " +
                    getIntent().getDoubleExtra("latitude", 0) + ", Longitude: " +
                    getIntent().getDoubleExtra("longitude", 0);
            String subject = getIntent().getStringExtra("username") + " SIGNAL OF DISTRESS";
            PublishRequest publishRequest = new PublishRequest(
                    Constants.snsARNDistress, msg, subject);
            snsClient.publish(publishRequest);
            return null;
        }
    }
    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        CognitoCachingCredentialsProvider sCredProvider = new CognitoCachingCredentialsProvider(
                context.getApplicationContext(),
                Constants.cognitoUnauthPoolID,
                Regions.US_EAST_1);
        return sCredProvider;
    }
}
