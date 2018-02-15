package com.erikterwiel.mountainviews;

import android.content.Context;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3Client;

import org.w3c.dom.Text;

import java.io.IOException;

public class ReportActivity extends AppCompatActivity {

    private Photo mPhoto;
    private Report mReport;
    private AmazonS3Client mS3Client;
    private AmazonDynamoDBClient mDDBClient;
    private DynamoDBMapper mMapper;
    private TransferUtility mTransferUtility;

    private TextView mUsername;
    private TextView mTitle;
    private TextView mLocation;
    private RecyclerView mRecycler;
    private TextView mReportText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mUsername = (TextView) findViewById(R.id.report_username);
        mTitle = (TextView) findViewById(R.id.report_title);
        mLocation = (TextView) findViewById(R.id.report_location);
        mRecycler = (RecyclerView) findViewById(R.id.report_recycler);
        mReportText = (TextView) findViewById(R.id.report_body);
        mReport = new Report();

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                Constants.cognitoUnauthPoolID,
                Regions.US_EAST_1);
        mDDBClient = new AmazonDynamoDBClient(credentialsProvider);
        mMapper = new DynamoDBMapper(mDDBClient);
        mTransferUtility = getTransferUtility(this);

        new PullReport().execute();


    }

    private class PullReport extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... inputs) {
            mReport = mMapper.load(Report.class, getIntent().getStringExtra("report"));
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mUsername.setText(getIntent().getStringExtra("username") + " - Trip Report");
            mTitle.setText(mReport.getTitle() + " - " + mReport.getDate());
            mLocation.setText(mReport.getLocation() + " - " + mReport.getDistance());
            mReportText.setText(mReport.getReport());
        }
    }

    public TransferUtility getTransferUtility(Context context) {
        mS3Client = getS3Client(context.getApplicationContext());
        TransferUtility mTransferUtility = new TransferUtility(
                mS3Client, context.getApplicationContext());
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
