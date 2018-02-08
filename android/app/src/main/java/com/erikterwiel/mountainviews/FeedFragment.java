package com.erikterwiel.mountainviews;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3Client;

import org.w3c.dom.Text;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import static com.google.android.gms.internal.zzahn.runOnUiThread;

public class FeedFragment extends Fragment {

    public static final String TAG = "FeedFragment.java";

    private LinearLayout mOuterLayout;
    private View mPhotoView;
    private View mReportView;
    private LinearLayout.LayoutParams mParams;
    private int mIndex;

    private List<Recent> mRecents;
    private Photo mPhoto;
    private Report mReport;
    private AmazonS3Client mS3Client;
    private AmazonDynamoDBClient mDDBClient;
    private DynamoDBMapper mMapper;
    private TransferUtility mTransferUtility;

    private LayoutInflater mInflater;
    private ViewGroup mContainer;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mInflater = inflater;
        mContainer = container;
        View outer = inflater.inflate(R.layout.fragment_feed, container, false);
        mOuterLayout = (LinearLayout) outer.findViewById(R.id.feed_layout);
        inflate();
        mParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mParams.setMargins(0, 0, 0, 0);

        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getActivity().getApplicationContext(),
                Constants.cognitoUnauthPoolID,
                Regions.US_EAST_1);
        mDDBClient = new AmazonDynamoDBClient(credentialsProvider);
        mMapper = new DynamoDBMapper(mDDBClient);
        mTransferUtility = getTransferUtility(getActivity());
        new PullRecents().execute();
        return outer;
    }

    private void inflate() {
        mPhotoView = mInflater.inflate(R.layout.layout_photo, mContainer, false);
        mReportView = mInflater.inflate(R.layout.layout_report, mContainer, false);
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

    private class PullRecents extends AsyncTask<Void, Void, Void> {
        private ProgressDialog mDialog;

        @Override
        protected void onPreExecute() {
            mDialog = ProgressDialog.show(getActivity(),
                    getString(R.string.home_dialog),
                    getString(R.string.home_wait));
        }

        @Override
        protected Void doInBackground(Void... inputs) {
            DynamoDBScanExpression scan = new DynamoDBScanExpression();
            mRecents = mMapper.scan(Recent.class, scan);
            mIndex = 1;
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mDialog.dismiss();
            if (mRecents.size() != 0) {
                switch (mRecents.get(mRecents.size() - mIndex).getType()) {
                    case "photo":
                        new PullPhoto().execute();
                        break;
                    case "report":
                        new PullReport().execute();
                        break;
                    case "plan":
                        break;
                    case "activity":
                        break;
                }
            }
        }
    }

    private class PullPhoto extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... inputs) {
            String s3Name = mRecents.get(mRecents.size() - mIndex).getIdentifier();
            mPhoto = mMapper.load(Photo.class, s3Name);
            File folder = new File("sdcard/Pictures/MountainViews/Input");
            if (!folder.exists()) folder.mkdir();
            File file = new File(folder, "input" + mIndex + ".png");
            Log.i(TAG, file.getAbsolutePath());
            TransferObserver observer = mTransferUtility.download(Constants.s3BucketName, s3Name, file);
            observer.setTransferListener(new DownloadListener());
            return null;
        }
    }

    private class DownloadListener implements TransferListener {
        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.i(TAG, state + "");
            if (state == TransferState.COMPLETED) {
                Bitmap bitmap = BitmapFactory.decodeFile(
                        "sdcard/Pictures/MountainViews/Input/input" + mIndex + ".png");
                inflate();
                TextView userName = (TextView) mPhotoView.findViewById(R.id.layout_photo_username);
                TextView location = (TextView) mPhotoView.findViewById(R.id.layout_photo_location);
                ImageView imageView = (ImageView) mPhotoView.findViewById(R.id.layout_photo_photo);
                TextView caption = (TextView) mPhotoView.findViewById(R.id.layout_photo_caption);
                userName.setText(mRecents.get(mRecents.size() - mIndex).getUsername());
                location.setText(mPhoto.getLocation());
                caption.setText(mPhoto.getCaption());
                imageView.setImageBitmap(bitmap);
                mOuterLayout.addView(mPhotoView, mParams);
                if (mRecents.size() - mIndex == 0) {
                    return;
                } else {
                    mIndex += 1;
                    switch (mRecents.get(mRecents.size() - mIndex).getType()) {
                        case "photo":
                            new PullPhoto().execute();
                            break;
                        case "report":
                            new PullReport().execute();
                            break;
                        case "plan":
                            break;
                        case "activity":
                            break;
                    }
                }
            }
        }
        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (bytesTotal != 0) {
                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                Log.i(TAG, Integer.toString(percentage) + "% downloaded");
            }
        }
        @Override
        public void onError(int id, Exception ex) {
            ex.printStackTrace();
            Log.i(TAG, "Error detected");
        }
    }

    private class PullReport extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... inputs) {
            mReport = mMapper.load(Report.class, mRecents.get(mRecents.size() - mIndex).getIdentifier());
            String s3Name = mReport.getPhotos().get(0);
            mPhoto = mMapper.load(Photo.class, s3Name);
            File folder = new File("sdcard/Pictures/MountainViews/Input");
            if (!folder.exists()) folder.mkdir();
            File file = new File(folder, "input" + mIndex + ".png");
            Log.i(TAG, file.getAbsolutePath());
            TransferObserver observer = mTransferUtility.download(Constants.s3BucketName, s3Name, file);
            observer.setTransferListener(new DownloadReportListener());
            return null;
        }
    }

    private class DownloadReportListener implements TransferListener {
        @Override
        public void onStateChanged(int id, TransferState state) {
            Log.i(TAG, state + "");
            if (state == TransferState.COMPLETED) {
                Bitmap bitmap = BitmapFactory.decodeFile(
                        "sdcard/Pictures/MountainViews/Input/input" + mIndex + ".png");
                inflate();
                LinearLayout layout = (LinearLayout) mReportView.findViewById(R.id.layout_report_layout);
                TextView userName = (TextView) mReportView.findViewById(R.id.layout_report_username);
                TextView title = (TextView) mReportView.findViewById(R.id.layout_report_title);
                TextView location = (TextView) mReportView.findViewById(R.id.layout_report_location);
                ImageView imageView = (ImageView) mReportView.findViewById(R.id.layout_report_photo);
                TextView expand = (TextView) mReportView.findViewById(R.id.layout_report_expand);
                RecyclerView recycler = (RecyclerView) mReportView.findViewById(R.id.layout_report_recycler);
                TextView report = (TextView) mReportView.findViewById(R.id.layout_report_report);
                layout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent reportIntent = new Intent(getActivity(), ReportActivity.class);
                        reportIntent.putExtra("report", mReport.getTitle());
                        reportIntent.putExtra("username", getActivity().getIntent().getStringExtra("username"));
                        startActivity(reportIntent);
                    }
                });
                userName.setText(mRecents.get(mRecents.size() - mIndex).getUsername() + " - Trip Report");
                title.setText(mReport.getTitle() + " - " + mReport.getDate());
                location.setText(mReport.getLocation() + " - " + mReport.getDistance());
                expand.setText(R.string.layout_report_expand);
                imageView.setImageBitmap(bitmap);
                mOuterLayout.addView(mReportView, mParams);
                if (mRecents.size() - mIndex != 0) {
                    mIndex += 1;
                    switch (mRecents.get(mRecents.size() - mIndex).getType()) {
                        case "photo":
                            new PullPhoto().execute();
                            break;
                        case "report":
                            new PullReport().execute();
                            break;
                        case "plan":
                            break;
                        case "activity":
                            break;
                    }
                }
            }
        }
        @Override
        public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
            if (bytesTotal != 0) {
                int percentage = (int) (bytesCurrent / bytesTotal * 100);
                Log.i(TAG, Integer.toString(percentage) + "% downloaded");
            }
        }
        @Override
        public void onError(int id, Exception ex) {
            ex.printStackTrace();
            Log.i(TAG, "Error detected");
        }
    }
}
