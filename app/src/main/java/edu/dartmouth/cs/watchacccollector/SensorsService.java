/**
 * LocationService.java
 * <p/>
 * Created by Xiaochao Yang on Sep 11, 2011 4:50:19 PM
 */

package edu.dartmouth.cs.watchacccollector;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import meapsoft.FFT;
import weka.classifiers.WekaWrapper;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.core.converters.ConverterUtils.DataSource;

public class SensorsService extends Service implements SensorEventListener {

    private final String[] ACTIVITIES = {Globals.CLASS_LABEL_STANDING,
            Globals.CLASS_LABEL_WALKING, Globals.CLASS_LABEL_RUNNING,
            Globals.CLASS_LABEL_OTHER};

    private static final int mFeatLen = Globals.ACCELEROMETER_BLOCK_CAPACITY + 2;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Instances mDataset;
    private OnSensorChangedTask mAsyncTask;
    private WekaWrapper wekaWrapper;
    private Queue<Integer> votingPool;
    private int[] votingScores;

    private static ArrayBlockingQueue<Double> mAccBuffer;
    public static final DecimalFormat mdf = new DecimalFormat("#.##");

    @Override
    public void onCreate() {
        super.onCreate();

        mAccBuffer = new ArrayBlockingQueue<Double>(
                Globals.ACCELEROMETER_BUFFER_CAPACITY);
        wekaWrapper = new WekaWrapper();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mSensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_FASTEST);

        // Create the container for attributes
        ArrayList<Attribute> allAttr = new ArrayList<Attribute>();

        // Adding FFT coefficient attributes
        DecimalFormat df = new DecimalFormat("0000");

        for (int i = 0; i < Globals.ACCELEROMETER_BLOCK_CAPACITY; i++) {
            allAttr.add(new Attribute(Globals.FEAT_FFT_COEF_LABEL + df.format(i)));
        }
        // Adding the max feature
        allAttr.add(new Attribute(Globals.FEAT_MAX_LABEL));

        // Declare a nominal attribute along with its candidate values
        ArrayList<String> labelItems = new ArrayList<String>(3);
        labelItems.add(Globals.CLASS_LABEL_STANDING);
        labelItems.add(Globals.CLASS_LABEL_WALKING);
        labelItems.add(Globals.CLASS_LABEL_RUNNING);
        labelItems.add(Globals.CLASS_LABEL_OTHER);
        Attribute mClassAttribute = new Attribute(Globals.CLASS_LABEL_KEY, labelItems);
        allAttr.add(mClassAttribute);

        // Construct the dataset with the attributes specified as allAttr and
        // capacity 10000
        mDataset = new Instances(Globals.FEAT_SET_NAME, allAttr, Globals.FEATURE_SET_CAPACITY);

        // Set the last column/attribute (standing/walking/running) as the class
        // index for classification
        mDataset.setClassIndex(mDataset.numAttributes() - 1);

        mAsyncTask = new OnSensorChangedTask();
        mAsyncTask.execute();

        votingPool = new LinkedList<>();
        votingScores = new int[ACTIVITIES.length];

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mAsyncTask.cancel(true);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mSensorManager.unregisterListener(this);
        super.onDestroy();

    }

    private class OnSensorChangedTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... arg0) {
            Instance inst = new DenseInstance(mFeatLen);
            inst.setDataset(mDataset);
            int blockSize = 0;
            FFT fft = new FFT(Globals.ACCELEROMETER_BLOCK_CAPACITY);
            double[] accBlock = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];
            double[] re = accBlock;
            double[] im = new double[Globals.ACCELEROMETER_BLOCK_CAPACITY];

            double max = Double.MIN_VALUE;

            while (true) {
                try {
                    // need to check if the AsyncTask is cancelled or not in the while loop
                    if (isCancelled() == true) {
                        return null;
                    }
                    // Dumping buffer
                    accBlock[blockSize++] = mAccBuffer.take().doubleValue();

                    if (blockSize == Globals.ACCELEROMETER_BLOCK_CAPACITY) {
                        blockSize = 0;

                        // time = System.currentTimeMillis();
                        max = .0;
                        for (double val : accBlock) {
                            if (max < val) {
                                max = val;
                            }
                        }

                        fft.fft(re, im);

                        for (int i = 0; i < re.length; i++) {
                            double mag = Math.sqrt(re[i] * re[i] + im[i]
                                    * im[i]);
                            inst.setValue(i, mag);
                            im[i] = .0; // Clear the field
                        }

                        // Append max after frequency component
                        inst.setValue(Globals.ACCELEROMETER_BLOCK_CAPACITY, max);
                        inst.setDataset(mDataset);
                        int activityIndex = (int)wekaWrapper.classifyInstance(inst);

                        if (votingPool.size() < Globals.VOTING_POOL_FULL_SIZE) {
                            votingPool.offer(activityIndex);
                        }
                        else {
                            int removeActivityIndex = votingPool.poll();
                            votingScores[removeActivityIndex] --;
                        }
                        votingScores[activityIndex] ++;
                        String activity = getActivityWithMaxVotingScore();

                        Intent intent = new Intent();
                        intent.setAction("edu.dartmouth.pedometer.CUSTOM_INTENT");
                        intent.putExtra("type", activity);
                        sendBroadcast(intent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String getActivityWithMaxVotingScore(){
        int activityIndex = 0;
        for (int i=1; i<ACTIVITIES.length; i++) {
            if (votingScores[i] > votingScores[activityIndex])
                activityIndex = i;
        }
        return ACTIVITIES[activityIndex];
    }


    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            double m = Math.sqrt(event.values[0] * event.values[0]
                    + event.values[1] * event.values[1] + event.values[2]
                    * event.values[2]);

            // Inserts the specified element into this queue if it is possible
            // to do so immediately without violating capacity restrictions,
            // returning true upon success and throwing an IllegalStateException
            // if no space is currently available. When using a
            // capacity-restricted queue, it is generally preferable to use
            // offer.

            try {
                mAccBuffer.add(new Double(m));
            } catch (IllegalStateException e) {

                // Exception happens when reach the capacity.
                // Doubling the buffer. ListBlockingQueue has no such issue,
                // But generally has worse performance
                ArrayBlockingQueue<Double> newBuf = new ArrayBlockingQueue<Double>(
                        mAccBuffer.size() * 2);

                mAccBuffer.drainTo(newBuf);
                mAccBuffer = newBuf;
                mAccBuffer.add(new Double(m));
            }
        }
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
