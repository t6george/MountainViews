package com.erikterwiel.mountainviews;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;
import com.thalmic.myo.scanner.ScanActivity;

public class StartActivity extends AppCompatActivity {

    public static final String TAG = "StartActivity.java";

    private DeviceListener mListener;
    private boolean mBluetooth = true;

    private EditText mDestination;
    private TextView mText;
    private FloatingActionButton mHome;
    private FloatingActionButton mStart;
    private FloatingActionButton mConnect;
    private CognitoCachingCredentialsProvider mCredentialsProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        mDestination = (EditText) findViewById(R.id.start_destination);
        mText = (TextView) findViewById(R.id.start_text);
        mHome = (FloatingActionButton) findViewById(R.id.start_home);
        mStart = (FloatingActionButton) findViewById(R.id.start_start);
        mConnect = (FloatingActionButton) findViewById(R.id.start_connect);
        mCredentialsProvider = getCredProvider(this);

        mHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDestination.getText().toString().equals("")) {
                    Toast.makeText(StartActivity.this, "Please enter a destination.",
                            Toast.LENGTH_LONG).show();
                } else {
                    new SendNotification().execute();
                }
            }
        });

        mConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mBluetooth) {
                    mBluetooth = false;
                    mConnect.setImageResource(R.drawable.ic_bluetooth_white_48dp);
                    mText.setText(R.string.start_myo_no);
                } else {
                    mBluetooth = true;
                    mConnect.setImageResource(R.drawable.ic_bluetooth_connected_white_48dp);
                    mText.setText(R.string.start_myo);
                }
            }
        });
    }

    private class SendNotification extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... inputs) {
            AmazonSNSClient snsClient = new AmazonSNSClient(mCredentialsProvider);
            snsClient.setRegion(Region.getRegion(Regions.US_EAST_1));
            String msg = getIntent().getStringExtra("username") + " has started a " +
                    "trip to " + mDestination.getText().toString() + ", go to " +
                    "http://mountainviews.ca or check your app to track their progress!";
            String subject = getIntent().getStringExtra("username") + " Started A " +
                    "Trip!";
            PublishRequest publishRequest = new PublishRequest(
                    Constants.snsARN, msg, subject);
//            snsClient.publish(publishRequest);
            Intent tripIntent = new Intent(StartActivity.this, TripActivity.class);
            tripIntent.putExtra("myo", mBluetooth);
            startActivity(tripIntent);
            return null;
        }
    }

    public TransferUtility getTransferUtility(Context context) {
        AmazonS3Client s3Client = getS3Client(context.getApplicationContext());
        TransferUtility mTransferUtility = new TransferUtility(
                s3Client, context.getApplicationContext());
        return mTransferUtility;
    }

    public static AmazonS3Client getS3Client(Context context) {
        AmazonS3Client sS3Client = new AmazonS3Client(getCredProvider(context.getApplicationContext()));
        return sS3Client;
    }

    private static CognitoCachingCredentialsProvider getCredProvider(Context context) {
        CognitoCachingCredentialsProvider sCredProvider = new CognitoCachingCredentialsProvider(
                context.getApplicationContext(),
                Constants.cognitoUnauthPoolID,
                Regions.US_EAST_1);
        return sCredProvider;
    }
}
