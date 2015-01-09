package com.example.bigstone.beaconlocdemo;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.bigstone.beaconlocdemo.bluetooth.BeaconContent;
import com.example.bigstone.beaconlocdemo.util.FileConfigSaverLoader;
import com.example.bigstone.beaconlocdemo.util.GlobalConfig;
import com.example.bigstone.beaconlocdemo.util.MultiPointTouchListener;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.util.ByteArrayBuffer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;

import hk.ust.cse.indoorloc.bluetooth.BeaconLocate;
import hk.ust.cse.indoorloc.bluetooth.BluetoothMessage;
import hk.ust.cse.indoorloc.bluetooth.LocationBeacon;
import hk.ust.cse.indoorloc.layout.RouteFinder;
import hk.ust.cse.indoorloc.particlefilter.LocationUpdateTimerTask;
import hk.ust.cse.indoorloc.particlefilter.MessageBufferManager;
import hk.ust.cse.indoorloc.particlefilter.Particle;
import hk.ust.cse.indoorloc.particlefilter.ParticleFilter;
import hk.ust.cse.indoorloc.stepcount.SimpleStepDetector;
import hk.ust.cse.indoorloc.structure.Point;
import hk.ust.cse.indoorloc.structure.Segment;


public class IndoorLocalizationActivity extends Activity implements RangeNotifier {
    private ImageView imageView;
    private TextView info;
    private Bitmap myImg;
    private Matrix matrix;
    private MultiPointTouchListener mtpl;

    private String imgFilePath;


    private Canvas canvas;
    private Paint paint;
    private Point cPoint = new Point();
    private String location = "1000";

    public static int INTERESTED_MESSAGE_TYPE = 2; // 1 for mixed localization, 2 for iBeacon, 3 for Sensor result, 4 for step detection

    private boolean hasInited = false;

    private boolean hasMoved = true;
    private boolean needStable = true; //only update location when detected moving

    private int count = 0;
    private BeaconLocate beaconLocater = new BeaconLocate();
    public static List<LinkedList<Segment>> wallList = new ArrayList<LinkedList<Segment>>();
    public static List<LinkedList<Segment>> routeList = new ArrayList<LinkedList<Segment>>();


    private Handler mHandler = new Handler(){
        public void handleMessage(Message msg)
        {

            if (msg.what == INTERESTED_MESSAGE_TYPE){

                cPoint = (Point)msg.obj;
                Log.i("cLocation", cPoint.getString());
                if(Float.isNaN(cPoint.x) || Float.isNaN(cPoint.y)){
                    Toast.makeText(getApplicationContext(), "Relocating", Toast.LENGTH_SHORT).show();
                    LocationUpdateTimerTask.restart();
                    return;
                }
                updateFrame();

            }

            if (msg.what == 4){
                hasMoved = true;
            }

        };
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_indoor_localization);
        info = (TextView)findViewById(R.id.info_txt);
        beaconLocater.setMAP_SCALE(67);
        initialData();
        setupMap();
        setupSensor();


        initialData();

        ParticleFilter pf = new ParticleFilter(wallList.get(0 /* areaId, if you add changing floor function, please update this*/), GlobalConfig.MAP_SCALE);


        LocationUpdateTimerTask pointCalculateTimerTask = new LocationUpdateTimerTask(mHandler, wallList.get(0), pf);
        Timer timer = new Timer("update", true);
        timer.schedule(pointCalculateTimerTask, 2000, 500);
    }

    public static void initialData(){
        BeaconContent.initialData();
        for (int i = 0; i < GlobalConfig.floorNameArray.size(); i++) {
            wallList.add(new LinkedList<Segment>());
            routeList.add(new LinkedList<Segment>());
        }

        try {
            wallList = FileConfigSaverLoader.loadWallRecord(GlobalConfig.wallFilePath);
            routeList = FileConfigSaverLoader.loadRouteRecord(GlobalConfig.routeFilePath);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    private void updateFrame(){
        if(cPoint.areaId == null){
            info.setText("No position record!");
            return;
        }

        if(hasInited && location.equals(cPoint.areaId)){
            this.refreshMap();
        } else {
            setImageView(cPoint.areaId);
        }



        if ((INTERESTED_MESSAGE_TYPE==1 || INTERESTED_MESSAGE_TYPE == 3) && GlobalConfig.displayParticle){
            displayParticles();
        }

        drawPoint(cPoint.x / GlobalConfig.COMPRESS_RATE, cPoint.y / GlobalConfig.COMPRESS_RATE, Point.PointType.LOCATION);
        drawCircle(cPoint, ParticleFilter.SAFE_RANGE);

        location = cPoint.areaId;
        info.setText(location+" area:"+ cPoint.x+","+ cPoint.y+":count:" + count++);
    }

    private void displayParticles(){
        for(Particle p :ParticleFilter.particleList){
            if (p.getWeight() != 0){
                drawPoint(p.getPoint().x/GlobalConfig.COMPRESS_RATE, p.getPoint().y/GlobalConfig.COMPRESS_RATE, Point.PointType.PARTICLE);
            }
        }

    }





    private void setupMap(){
        imageView = (ImageView) findViewById(R.id.imageView);
        mtpl = new MultiPointTouchListener();
        imageView.setOnTouchListener(mtpl);
    }
    private void setupSensor() {

        // start sensor listener


        SensorManager sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensorList = new ArrayList<Sensor>();
        sensorList.addAll(sensorManager
                .getSensorList(Sensor.TYPE_ACCELEROMETER));
        sensorList.add(sensorManager.getSensorList(Sensor.TYPE_ORIENTATION)
                .get(0));

        for (Sensor s : sensorList) {
            sensorManager.registerListener(SimpleStepDetector.instance(), s,
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        SimpleStepDetector.instance().setDifferenceDelta(1);
        SimpleStepDetector.instance().setMinPeak(3);
        SimpleStepDetector.instance().setStepDetectionDelta(700);

        SimpleStepDetector.mHandler = this.mHandler;
        SimpleStepDetector.instance().start();


    }

    private void refreshMap(){
        Bitmap rotated = Bitmap.createBitmap(myImg, 0, 0, myImg.getWidth(), myImg.getHeight(),
                matrix, true);
        canvas = new Canvas(rotated);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawBitmap(rotated, 0, 0, paint);
        paint.setAntiAlias(true);
        imageView.setImageBitmap(rotated);

        if (GlobalConfig.displayBeacon){
            drawBeacons();
        }


        drawSegments();
    }
    private void drawBeacons(){
        List<LocationBeacon> beacons = BeaconContent.beaconList.get(GlobalConfig.floorFolderArray.indexOf(cPoint.areaId));
        for (LocationBeacon b : beacons) {
            if (b.isReference){
                drawPoint(b.location.x/GlobalConfig.COMPRESS_RATE, b.location.y/GlobalConfig.COMPRESS_RATE, Point.PointType.PIN_BEACON);
                if (GlobalConfig.displayRange){
                    drawCircle(b.location, b.distance * GlobalConfig.MAP_SCALE);
                }

            } else {
                drawPoint(b.location.x/GlobalConfig.COMPRESS_RATE, b.location.y/GlobalConfig.COMPRESS_RATE, Point.PointType.BEACON);
            }
        }
    }
    private void drawSegments(){
        List<Segment> segments;
        if (GlobalConfig.displayWall) {
            segments = wallList.get(GlobalConfig.floorFolderArray.indexOf(cPoint.areaId));
            for (Segment s : segments) {
                drawRoute(s, Segment.Type.WALL);
            }
        }

        if (GlobalConfig.displayRoute) {
            segments = routeList.get(GlobalConfig.floorFolderArray.indexOf(cPoint.areaId));
            for (Segment s : segments) {
                drawRoute(s, Segment.Type.ROUTE);
            }
        }

    }
    private void setImageView(String floorFolderName) {
        imgFilePath = GlobalConfig.PRE_PATH + floorFolderName+"/map.jpg";
        Log.i("imgFilePath", imgFilePath);

        File mapFile = new File(imgFilePath);
        if(!mapFile.exists()){
            mapFile.getParentFile().mkdirs();
            new RemoteMapRetrieveTask().execute(floorFolderName, imgFilePath);
        } else {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = GlobalConfig.COMPRESS_RATE;

            myImg = BitmapFactory.decodeFile(imgFilePath, options).copy(
                    Bitmap.Config.ARGB_8888, true);
            matrix = new Matrix();
            matrix.postRotate(GlobalConfig.CONVERT_DEGREE);



            refreshMap();
            hasInited = true;
        }

    }
    private void drawPoint(double x, double y, Point.PointType type) {
        if (type == Point.PointType.LOCATION) {
            paint.setColor(Color.RED);
            paint.setStrokeWidth(2);
        }

        if (type == Point.PointType.BEACON) {
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(2);
        }

        if (type == Point.PointType.PIN_BEACON) {
            paint.setColor(Color.GREEN);
            paint.setStrokeWidth(2);
        }

        if (type == Point.PointType.PARTICLE) {
            paint.setStrokeWidth(1);
            paint.setColor(Color.BLACK);
        }

        canvas.drawPoint((float) x, (float) y, paint);
        imageView.invalidate();

    }
    private void drawRoute(Segment s, Segment.Type type) {
        if(s == null)return;
        if (type == Segment.Type.WALL){
            paint.setColor(Color.RED);
        }
        if (type == Segment.Type.ROUTE){
            paint.setColor(Color.GREEN);
        }
        if (type == Segment.Type.CHOOSEN){
            paint.setColor(Color.BLUE);
        }
        paint.setStrokeWidth(1);
        canvas.drawLine(s.sPoint.x / GlobalConfig.COMPRESS_RATE, s.sPoint.y / GlobalConfig.COMPRESS_RATE,
                s.ePoint.x / GlobalConfig.COMPRESS_RATE, s.ePoint.y / GlobalConfig.COMPRESS_RATE, paint);
        imageView.invalidate();
    }
    private void drawCircle(Point center, float r){
        paint.setColor(Color.YELLOW);
        paint.setStrokeWidth(1);
        canvas.drawCircle(center.x/GlobalConfig.COMPRESS_RATE, center.y/GlobalConfig.COMPRESS_RATE, r/GlobalConfig.COMPRESS_RATE, paint);
        imageView.invalidate();
    }
    public class RemoteMapRetrieveTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... arg0) {
            String imgUrl =getString(R.string.IP) + getString(R.string.img_url);
            imgUrl = imgUrl.replace("*areaId*", arg0[0]);

            try {
                URL imageUrl = new URL(imgUrl);
                URLConnection ucon = imageUrl.openConnection();

                InputStream is = ucon.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(is);

                ByteArrayBuffer baf = new ByteArrayBuffer(1024);
                int current = 0;
                while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
                }
                OutputStream out = new BufferedOutputStream(new FileOutputStream(arg0[1]));
                out.write(baf.toByteArray());
                out.close();
                is.close();


            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return arg0[0];
        }

        @Override
        protected void onPostExecute(final String content) {
            setImageView(content);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    @Override
    protected void onPause() {
        super.onPause();
        // Tell the Application not to pass off ranging updates to this activity
        ((BeaconLocApplication)this.getApplication()).removeRangeNotifier(this);
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Tell the Application to pass off ranging updates to this activity
        ((BeaconLocApplication)this.getApplication()).registerRangeNotifier(this);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_indoor_localization, menu);
        return true;
    }
//    private void logToDisplay(final String line) {
//        runOnUiThread(new Runnable() {
//            public void run() {
//                TextView editText = (TextView)IndoorLocalizationActivity.this
//                        .findViewById(R.id.rangingText);
//                editText.append(line+"\n");
//            }
//        });
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        BeaconContent.updateLocalBeaconDistance(beacons);
        List<LocationBeacon> pinBeacons =
                BeaconContent.extractNearestBeacon(BeaconContent.findRegisteredBeacons(beacons));

        Point center = beaconLocater.estimateLocation(pinBeacons);

        if (GlobalConfig.attach){

            int locationIndex = GlobalConfig.floorFolderArray.indexOf(center.areaId);
            if (locationIndex != -1){
                Segment s = RouteFinder.findNearestSegment(routeList.get(locationIndex), center);
                center = s.projectPoint(center);
            }

        }


        if (needStable){
            if (hasMoved){
                long time_long = System.nanoTime();
                MessageBufferManager.addBluetoothMessage(new BluetoothMessage(time_long, center));

                Message msg = new Message();
                msg.what = 2;
                msg.obj = center;
                mHandler.sendMessage(msg);
                hasMoved = false;
            }

        } else {
            long time_long = System.nanoTime();
            MessageBufferManager.addBluetoothMessage(new BluetoothMessage(time_long, center));

            Message msg = new Message();
            msg.what = 2;
            msg.obj = center;
            mHandler.sendMessage(msg);
        }


    }
}
