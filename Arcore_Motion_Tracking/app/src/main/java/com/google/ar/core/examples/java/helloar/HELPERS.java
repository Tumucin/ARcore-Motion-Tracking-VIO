package com.google.ar.core.examples.java.helloar;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class HELPERS extends AppCompatActivity implements SensorEventListener {
    /** IMU parameters */
    public int counterAccelerometer = 0, counterGyroscope = 0;/** Needed for calculating the rate of the sensors*/
    public double initialTimeStampIMU = 0, currentTimeStampIMU = 0, accelerometerRateHz = 0, gyroscopeRateHz = 0;
    public double currentAccelerometerTimeStamp = 0, currentGyroscopeTimeStamp = 1, currentTimeStampVIOAR = 0;
    public List<String> accelerometerValuesHistoryx = new ArrayList<>();
    public List<String> accelerometerValuesHistoryy = new ArrayList<>();
    public List<String> accelerometerValuesHistoryz = new ArrayList<>();
    public boolean enableAccelerometer = true, enableGyroscope = true;
    public List<String> gyroscopeValuesHistoryx = new ArrayList<>();
    public List<String> gyroscopeValuesHistoryy = new ArrayList<>();
    public List<String> gyroscopeValuesHistoryz = new ArrayList<>();
    public List<String> gyroscopeTimesHistoryx = new ArrayList<>();
    public List<String> VIOArValuesTimeHistory = new ArrayList<>();
    public Float[] currentImuValuesPlusTime = new Float[7];
    public Sensor sensorAccelerometer, sensorGyroscope;
    public TrackingState currentTrackingStateVIOAr;
    public HelloArActivity helloArActivity;
    public SensorManager sensorManager;
    public int sensorDelayTime = 10000; /** It is for accerometer and gyroscope */
    public double currentTimeIMU = 0;
    public Pose currentPoseVIOAr;

    /** File Manager Output Stream */
    public Dictionary<Integer, String> dataSetTypeDict = new Hashtable<Integer, String>();
    public Dictionary<Integer, String> userNameDict = new Hashtable<Integer, String>();
    public String imuFileNameToBeSaved, vioFileNameToBeSaved;

    /** CONSTRUCTOR */
    public HELPERS(HelloArActivity context){
        this.helloArActivity = context;
    }

    public void startRecording(View view){
        /**  Calls Sensor services*/
        callSensorManagerAndCallbackFunctions();
        view.setEnabled(false);
    }
    public void getFileNamesFromEditText(){
        /** This apps aims to save the IMU and VIO output data. So there should be 2 different file
         * names.
         * This function is used to get the file names from the user. But filenames are encoded.
         * The user have to write the names of the file in terms of numbers. You can check the dictionary
         * which is defined in defineAndCreateDictonaries().
         * You can also change the structure of the dictionary.
         * */
        defineAndCreateDictonaries();
        String imuFileNameString = helloArActivity.editTextIMUFileName.getText().toString();
        String vioFileNameString = helloArActivity.editTextVIOARFileName.getText().toString();

        imuFileNameToBeSaved = userNameDict.get(Character.getNumericValue(imuFileNameString.charAt(0)))
                                            +dataSetTypeDict.get(Character.getNumericValue(imuFileNameString.charAt(1)))
                                            +"imu"
                                            +imuFileNameString.charAt(2)
                                            +".csv";
        vioFileNameToBeSaved = userNameDict.get(Character.getNumericValue(vioFileNameString.charAt(0)))
                                            +dataSetTypeDict.get(Character.getNumericValue(vioFileNameString.charAt(1)))
                                            +"vio"
                                            +vioFileNameString.charAt(2)
                                            +".csv";
        /** After get the input from the user, make them invisible.*/
        helloArActivity.editTextIMUFileName.setVisibility(View.INVISIBLE);
        helloArActivity.editTextVIOARFileName.setVisibility(View.INVISIBLE);
    }
    public void defineAndCreateDictonaries(){
        /** This function defines the dictionary that will be used to create file names*/
        userNameDict.put(1, "User1");
        userNameDict.put(2, "User2");
        userNameDict.put(3, "User3");
        userNameDict.put(4, "User4");
        userNameDict.put(5, "User5");
        userNameDict.put(6, "User6");
        userNameDict.put(7, "User7");
        userNameDict.put(8, "User8");
        userNameDict.put(9, "User9");
        userNameDict.put(10, "User10");

        dataSetTypeDict.put(0,"WalkingHand");
        dataSetTypeDict.put(1,"SlowWalkingHand");
        dataSetTypeDict.put(2,"RunningHand");
        dataSetTypeDict.put(3,"WalkingPocket");
        dataSetTypeDict.put(4,"SlowWalkingPocket");
        dataSetTypeDict.put(5,"RunningPocket");
        dataSetTypeDict.put(6,"WalkingBody");
        dataSetTypeDict.put(7,"SlowWalkingBody");
        dataSetTypeDict.put(8,"RunningBody");
    }
    public void showSnackMessageOnScreen(View view){
        /** When StartRecording button is pressed, this function is called. And makes the Edit Text
         * invisible.
         * */
        String message = imuFileNameToBeSaved + " and " +vioFileNameToBeSaved +" are created.!!!";
        helloArActivity.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        helloArActivity.snackbar.show();
    }
    public void saveIMUVIOonClick(View view) {
        /** Accelerometer, gyroscope and VIO are saved to a file when the button is pressed.
         * The file names to be saved are created using dictionaries. You can go to
         * defineAndCreateDictonaries() and getFileNamesFromEditText()
         * */
        FileOutputStream fos = null;
        try {
            fos = helloArActivity.openFileOutput(vioFileNameToBeSaved,MODE_PRIVATE);
            fos.write(VIOArValuesTimeHistory.toString().getBytes());
            fos.close();
            view.setEnabled(false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fos = helloArActivity.openFileOutput(imuFileNameToBeSaved,MODE_PRIVATE);
            for (int i =0;i<gyroscopeValuesHistoryx.size();i++){
                fos.write(gyroscopeTimesHistoryx.get(i).replace(",",".").getBytes());
                fos.write(" ,".getBytes());
                fos.write(" ,".getBytes());
                fos.write(" ,".getBytes());
                fos.write(gyroscopeValuesHistoryx.get(i).replace(",",".").getBytes());
                fos.write(",".getBytes());
                fos.write(gyroscopeValuesHistoryy.get(i).replace(",",".").getBytes());
                fos.write(",".getBytes());
                fos.write(gyroscopeValuesHistoryz.get(i).replace(",",".").getBytes());
                fos.write(" ,".getBytes());
                fos.write(" ,".getBytes());
                fos.write(" ,".getBytes());
                fos.write(accelerometerValuesHistoryx.get(i).replace(",",".").getBytes());
                fos.write(",".getBytes());
                fos.write(accelerometerValuesHistoryy.get(i).replace(",",".").getBytes());
                fos.write(",".getBytes());
                fos.write(accelerometerValuesHistoryz.get(i).replace(",",".").getBytes());
                fos.write("\n".getBytes());
            }
            fos.close();
            view.setEnabled(false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        String message = imuFileNameToBeSaved + " and " +vioFileNameToBeSaved +" are saved.!!!";
        helloArActivity.snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        helloArActivity.snackbar.show();

    }
    public void calculateNumberOfFeaturePoints(PointCloud pointCloud){
        /** This function calculates the number of feature points on the screen */
        // Store getIds as string
        String stringPointCloudId = pointCloud.getIds().toString();
        stringPointCloudId = stringPointCloudId.replaceAll("[^0-9]+", " ");
        // Extract the number in string
        List<String> test = Arrays.asList(stringPointCloudId.trim().split(" "));
        helloArActivity.numberOfFeaturePoints = 0;
        for(int i = 0; i < test.size(); i++){
            helloArActivity.numberOfFeaturePoints += Double.parseDouble(test.get(i));
        }
        helloArActivity.numberOfFeaturePoints = helloArActivity.numberOfFeaturePoints / 2;
    }
    public void initializeLayout(){
        /** This function initialize the Layout. And gives names to each layout member*/
        helloArActivity.surfaceView = helloArActivity.findViewById(R.id.surfaceview);
        helloArActivity.textViewPose = helloArActivity.findViewById(R.id.Pose);
        helloArActivity.textViewFeaturesNumb = helloArActivity.findViewById(R.id.FeaturesNumb);
        helloArActivity.textViewSamplingRate = helloArActivity.findViewById(R.id.SamplingRate);
        helloArActivity.textViewTime = helloArActivity.findViewById(R.id.Time);
        helloArActivity.textViewTrackingState = helloArActivity.findViewById(R.id.TrackingState);
        helloArActivity.textViewAccelerometer = helloArActivity.findViewById(R.id.Accelerometer);
        helloArActivity.editTextVIOARFileName = helloArActivity.findViewById(R.id.VIOARFileName);
        helloArActivity.editTextIMUFileName = helloArActivity.findViewById(R.id.IMUFileName);
    }
    public void calculateVIOArRateHz(){
        /** Calculates the rate of the ARcore - VIO. The VIO rate should be approximately 30 HZ*/
        if (helloArActivity.counterFrameVIOAr == 5){
            helloArActivity.initialTimeStampVIOAr = (double)helloArActivity.currentFrameVIOAr.getTimestamp()/1000000000;
        }
        currentTimeStampVIOAR = (double)helloArActivity.currentFrameVIOAr.getTimestamp()/1000000000;
        try{
            helloArActivity.currentTimeVIOAr = currentTimeStampVIOAR - helloArActivity.initialTimeStampVIOAr;
            helloArActivity.frameRateVIOAr = 1*(double)helloArActivity.counterFrameVIOAr/(helloArActivity.currentTimeVIOAr);
        }
        catch(java.lang.ArithmeticException ignored){
        }
    }
    public void updateInformationOnScreen(){
        /** This function writes all of the required information to the labels on the screen. */
        calculateVIOArRateHz();
        currentPoseVIOAr = helloArActivity.currentFrameVIOAr.getCamera().getPose();
        currentTrackingStateVIOAr = helloArActivity.currentFrameVIOAr.getCamera().getTrackingState();
        helloArActivity.textViewPose.setText("POSE:"+currentPoseVIOAr.toString());
        helloArActivity.textViewTime.setText("TIME VIO Ar [s]:"+helloArActivity.currentTimeVIOAr);
        helloArActivity.textViewSamplingRate.setText("RATE VIO Ar[Hz]:"+helloArActivity.frameRateVIOAr+"\n"
                                                    +"VIO AR TimeStamp:"+currentTimeStampVIOAR);
        helloArActivity.textViewTrackingState.setText("STATE:"+currentTrackingStateVIOAr);
        helloArActivity.textViewFeaturesNumb.setText("Number Of Features:"+helloArActivity.numberOfFeaturePoints);
        helloArActivity.textViewAccelerometer.setText("AccelX:"+currentImuValuesPlusTime[4]+"\n"
                                                        +"AccelY:"+currentImuValuesPlusTime[5]+"\n"
                                                        +"AccelZ:"+currentImuValuesPlusTime[6]+"\n"
                                                        +"Accel Rate [Hz]"+accelerometerRateHz+"\n"
                                                        +"Accel TimeStamp:"+currentAccelerometerTimeStamp+"\n"
                                                        +"Gyrox:"+currentImuValuesPlusTime[1]+"\n"
                                                        +"Gyroy:"+currentImuValuesPlusTime[2]+"\n"
                                                        +"Gyroz:"+currentImuValuesPlusTime[3]+"\n"
                                                        +"Gyro Rate [Hz]"+gyroscopeRateHz+"\n"
                                                        +"Gyro TimeStamp"+currentGyroscopeTimeStamp+"\n"
                                                        +"IMU TIME:"+currentImuValuesPlusTime[0]);
    }
    public void callSensorManagerAndCallbackFunctions(){
        /** This function calls System Service and creates accelerometer and gyroscope sensors.
         * Links listener functions to sensors
         * */
        sensorManager = (SensorManager) this.helloArActivity.getSystemService(this.helloArActivity.SENSOR_SERVICE);
        if (sensorManager!=null){ // If service is available.
            sensorAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
            sensorGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            if (sensorAccelerometer!=null){ // If sensor accelerometer is available
                // Define the callbacks for each sensor.

                sensorManager.registerListener(this, sensorAccelerometer, sensorDelayTime);
                sensorManager.registerListener(gyroListener, sensorGyroscope,sensorDelayTime);
            }
        } else { /** If service is not available. */
            Toast.makeText(this, "Sensor service is not detected", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        /** This is the callback function for ACCELEROMETER only. */
        if(counterAccelerometer > 10 && accelerometerRateHz > 101.0){
            /** If accelerometer Rate is greater than 100 Hz, then make it slow*/
            enableAccelerometer = true;
        }

        if(event.sensor.getType()==Sensor.TYPE_LINEAR_ACCELERATION && enableAccelerometer){
            /** Updates and saved the current values*/
            currentImuValuesPlusTime[0] = (float)event.timestamp/1000000000;
            currentImuValuesPlusTime[4] = event.values[0]/9.81F;
            currentImuValuesPlusTime[5] = event.values[1]/9.81F;
            currentImuValuesPlusTime[6] = event.values[2]/9.81F;
            accelerometerValuesHistoryx.add(String.format("%.6f", event.values[0]/9.81F));
            accelerometerValuesHistoryy.add(String.format("%.6f", event.values[1]/9.81F));
            accelerometerValuesHistoryz.add(String.format("%.6f", event.values[2]/9.81F));

            currentAccelerometerTimeStamp = (double)event.timestamp/1000000000;
            counterAccelerometer++;
        }
        accelerometerRateHz = calculateSensorRateHz(event, counterAccelerometer);
        enableAccelerometer = true;
    }

    private double calculateSensorRateHz(SensorEvent event, int counter) {
        /** Calculate the sensor rate. This function is used for accelerometer and gyroscope.
         * */
        double calculatedRateHz = 0;
        if (counterAccelerometer == 3){
            initialTimeStampIMU = (double)event.timestamp/1000000000;
        }
        currentTimeStampIMU = (double)event.timestamp/1000000000;
        currentTimeIMU = currentTimeStampIMU - initialTimeStampIMU;
        try{
            calculatedRateHz = 1*(double)counter/(currentTimeStampIMU - initialTimeStampIMU);
        }
        catch(java.lang.ArithmeticException ignored){
        }

        return calculatedRateHz;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    public SensorEventListener gyroListener = new SensorEventListener() {
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        public void onSensorChanged(SensorEvent event) {
            /** This callback function is used for GYROSCOPE only. It saves the gyroscope data to
             * gyroscopeHistory Lists. In addition to that it saved the VIO output to VIOArValuesTimeHistory
             * */
            if(counterGyroscope > 100 && gyroscopeRateHz > 101.0){
                /** If gyroscope Rate is greater than 100 Hz, then make it slow*/
                enableGyroscope = true;
            }
            if(event.sensor.getType()==Sensor.TYPE_GYROSCOPE && enableGyroscope){
                currentImuValuesPlusTime[0] = (float)event.timestamp/1000000000;

                gyroscopeValuesHistoryx.add(String.format("%.6f", event.values[0]));
                gyroscopeValuesHistoryy.add(String.format("%.6f", event.values[1]));
                gyroscopeValuesHistoryz.add(String.format("%.6f", event.values[2]));

                currentImuValuesPlusTime[1] = event.values[0];
                currentImuValuesPlusTime[2] = event.values[1];
                currentImuValuesPlusTime[3] = event.values[2];
                currentGyroscopeTimeStamp = (double)event.timestamp/1000000000;
                gyroscopeTimesHistoryx.add(String.format("%f",currentGyroscopeTimeStamp));
                counterGyroscope++;
                try{

                    VIOArValuesTimeHistory.add(String.format("%f", currentImuValuesPlusTime[0]).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add("");
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().tx()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().ty()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().tz()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().qx()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().qy()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().qz()).replace(",",".").replace("\\s",""));
                    VIOArValuesTimeHistory.add(String.format("%.6f", helloArActivity.currentFrameVIOAr.getCamera().getPose().qw()).replace(",",".").replace("\\s","")+"\n");
                }
                catch (java.lang.NullPointerException exception){
                }
            }
            gyroscopeRateHz = calculateSensorRateHz(event, counterGyroscope);
            enableGyroscope = true;
        }
    };

}
