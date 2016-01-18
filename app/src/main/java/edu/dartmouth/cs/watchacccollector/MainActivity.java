package edu.dartmouth.cs.watchacccollector;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import edu.dartmouth.cs.watchacccollector.accelerometer.Filter;

public class MainActivity extends WearableActivity implements SensorEventListener {

    private BoxInsetLayout mContainerView;

    /**
     * Filter class required to filter noise from accelerometer
     */
    private Filter filter = null;

    /**
     * SensorManager
     */
    private SensorManager mSensorManager;
    /**
     * Accelerometer Sensor
     */
    private Sensor mAccelerometer;

    /**
     * Step count to be displayed in UI
     */
    private int stepCount = 0;

    /**
     * Is accelerometer running?
     */
    private static boolean isAccelRunning = false;

    //Sensor data files
    private File mRawAccFile;
    private FileOutputStream mRawAccOutputStream;

    /*
	 * Various UI components
	 */
    private TextView stepsView;
    private CompoundButton accelButton;

    /*
     *
     */
    private int windowSampleCount = 0;
    private int allSampleCount = 0;
    private int holdingSteps = 0;
    private boolean hasAChanceWhenLastWindowEnds = true;
    private int continuousWindows = 0;
    private ArrayList<Double> xSamples = null;
    private ArrayList<Double> ySamples = null;
    private ArrayList<Double> zSamples = null;
    private ArrayList<Double> mSamples = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);

        // Init files
        mRawAccFile = new File(Environment.getExternalStorageDirectory(), "acc_raw.csv");
        try {
            mRawAccOutputStream = new FileOutputStream(mRawAccFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        stepsView = (TextView)findViewById(R.id.stepCount);
        //Set the buttons and the text accordingly
        accelButton = (ToggleButton) findViewById(R.id.StartButton);
        accelButton.setChecked(isAccelRunning);
        accelButton.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton btn,boolean isChecked) {
                        if(!isAccelRunning) {
                            startAccelerometer();
                            accelButton.setChecked(true);
                        }
                        else {
                            stopAccelerometer();
                            accelButton.setChecked(false);
                        }
                    }
                }
        );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try{
            mRawAccOutputStream.close();
        }catch (Exception ex)
        {
        }
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
        } else {
            mContainerView.setBackground(null);
        }
    }

    /**
     * start accelerometer
     */
    private void startAccelerometer() {
        isAccelRunning = true;
        hasAChanceWhenLastWindowEnds = true;
        Log.d("Start", "start!!");
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        //Set up filter
        //Following sets up smoothing filter from mcrowdviz
        int SMOOTH_FACTOR = 10;
        filter = new Filter(SMOOTH_FACTOR);
        //OR Use Butterworth filter from mcrowdviz
        //double CUTOFF_FREQUENCY = 0.3;
        //filter = new Filter(CUTOFF_FREQUENCY);
        stepCount = 0;
    }

    /**
     * stop accelerometer
     */
    private void stopAccelerometer() {
        isAccelRunning = false;
        mSensorManager.unregisterListener(this);

        //Free filter and step detector
        filter = null;

        stepCount += detectSteps(0, 0, 0);
        xSamples.clear();
        ySamples.clear();
        zSamples.clear();
        mSamples.clear();
        allSampleCount = 0;
        sendUpdatedStepCountToUI();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float accel[] = event.values;
            sendAccelValuesToUI(accel[0], accel[1], accel[2]);

            /**
             * TODO: Step Detection
             */
            //First, Get filtered values
            double filtAcc[] = filter.getFilteredValues(accel[0], accel[1], accel[2]);
            //Now, increment 'stepCount' variable if you detect any steps here
            stepCount += detectSteps(filtAcc[0], filtAcc[1], filtAcc[2]);
            //detectSteps() is not implemented
            sendUpdatedStepCountToUI();

        }

    }

    /* (non-Javadoc)
 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor, int)
 */
    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    /**
     * This should return number of steps detected.
     * @param filt_acc_x
     * @param filt_acc_y
     * @param filt_acc_z
     * @return
     */
    public int detectSteps(double filt_acc_x, double filt_acc_y, double filt_acc_z) {
        int addSteps = 0;
        if (xSamples == null) {
            xSamples = new ArrayList<Double>();
            ySamples = new ArrayList<Double>();
            zSamples = new ArrayList<Double>();
            mSamples = new ArrayList<Double>();
        }
        xSamples.add(filt_acc_x);
        ySamples.add(filt_acc_y);
        zSamples.add(filt_acc_z);
        mSamples.add(Math.sqrt(Math.pow(filt_acc_x, 2)
                + Math.pow(filt_acc_y, 2) + Math.pow(filt_acc_z, 2)));
        windowSampleCount ++;
        allSampleCount ++;

        if (!isAccelRunning || windowSampleCount >= 50) {
            boolean hasAChance = hasAChanceWhenLastWindowEnds;
            ArrayList<Double> smoothedMSamples = linearSmooth(mSamples, allSampleCount - windowSampleCount, 2);
            ArrayList<Double> peaks = findPeaks(smoothedMSamples);
            ArrayList<Double> valleys = findValleys(smoothedMSamples);
            double meanOfPeaks = getMean(peaks);
            double meanOfValleys = getMean(valleys);
            // Log.d("meanOfPeaks", String.valueOf(meanOfPeaks));
            // Log.d("meanOfValleys", String.valueOf(meanOfValleys));
            if (meanOfPeaks > 10.15) {
                //double upperThreshold = meanOfPeaks - 0.25 * (meanOfPeaks - meanOfValleys);
                //double bottomThreshold = meanOfValleys + 0.25 * (meanOfPeaks - meanOfValleys);
                double middleThreshold = (meanOfPeaks + meanOfValleys) / 2;
                //Log.d("upper", String.valueOf(upperThreshold));
                //Log.d("bottom", String.valueOf(bottomThreshold));

                for (int i=0; i<smoothedMSamples.size(); i++) {
                    if (smoothedMSamples.get(i) > middleThreshold + 0.5) {
                        if (hasAChance) {
                            //System.out.println("x: " + i +"y: " + smoothedMSamples.get(i));
                            addSteps++;
                        }
                        hasAChance = false;
                    }
                    if (smoothedMSamples.get(i) < middleThreshold) {
                        hasAChance = true;
                    }
                }
            }
            windowSampleCount = 0;
            hasAChanceWhenLastWindowEnds = hasAChance;
            if (allSampleCount >= 5000) {
                xSamples.clear();
                ySamples.clear();
                zSamples.clear();
                mSamples.clear();
                allSampleCount = 0;
            }


            // Log.d("addSteps", String.valueOf(addSteps));
            if(addSteps >= 4)
                addSteps = 0;

            if (addSteps == 0) {
                continuousWindows = 0;
                holdingSteps = 0;
            }
            else {
                continuousWindows += 1;
                holdingSteps += addSteps;
            }

            if (continuousWindows >= 3) {
                addSteps = holdingSteps;
                holdingSteps = 0;
                return addSteps;
            }

        }

        return 0;

    }


    private boolean isPeak(double before, double self, double after) {
        if (before < self && after < self) {
            return true;
        }
        return false;
    }


    private ArrayList<Double> findPeaks(ArrayList<Double> samples) {
        ArrayList<Double> peaks = new ArrayList<Double>();

        for (int i=1; i<samples.size()-1; i++) {
            if (isPeak(samples.get(i-1), samples.get(i), samples.get(i+1))) {
                peaks.add(samples.get(i));
            }
        }
        return peaks;
    }


    private ArrayList<Double> findValleys(ArrayList<Double> samples) {
        ArrayList<Double> valleys = new ArrayList<Double>();

        for (int i=1; i<samples.size()-1; i++) {
            if (isPeak(-samples.get(i-1), -samples.get(i), -samples.get(i+1))) {
                valleys.add(samples.get(i));
            }
        }
        return valleys;
    }


    private double getMean(ArrayList<Double> samples){
        double sum = 0;
        for (int i=0; i<samples.size(); i++) {
            sum += samples.get(i);
        }
        return sum / samples.size();
    }


    private ArrayList<Double> linearSmooth(ArrayList<Double> samples, int fromIndex, int boundOffset) {
        ArrayList<Double> smoothed = new ArrayList<Double>();
        int window = boundOffset * 2 + 1;

        // [0 1 2 3 4 5 6 7]
        // [* * * * * 5 6 7]
        double windowSum = 0;

        int firstSmoothedIndex = 0;
        // first window sum
        if (fromIndex < window) {
            for (int i=0; i<window; i++) {
                windowSum += samples.get(i);
            }
            firstSmoothedIndex = boundOffset;
        }
        else {
            for (int i=fromIndex-window+1; i<fromIndex+1; i++) {
                windowSum += samples.get(i);
            }
            firstSmoothedIndex = fromIndex-boundOffset;
        }
        smoothed.add(windowSum / window);

        // from second window sum
        for (int i=firstSmoothedIndex+boundOffset+1; i<samples.size(); i++) {
            windowSum = windowSum - samples.get(i - window) + samples.get(i);
            smoothed.add(windowSum / window);
        }

        return smoothed;
    }


    private void sendAccelValuesToUI(float accX, float accY, float accZ) {
        /**
         * save raw data to a file
         */
        String record = System.currentTimeMillis() + "," +
                accX + "," + accY + "," + accZ + "\n";
        try {
            mRawAccOutputStream.write(record.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        /**
         * show raw data on UI
         */
        //Log.d("ACC_RAW", record);
    }

    private void sendUpdatedStepCountToUI() {
        stepsView.setText("Steps=" + stepCount);
    }
}
