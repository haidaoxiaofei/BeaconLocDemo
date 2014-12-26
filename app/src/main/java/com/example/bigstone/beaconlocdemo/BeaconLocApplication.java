package com.example.bigstone.beaconlocdemo;

import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.os.RemoteException;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.altbeacon.beacon.powersave.BackgroundPowerSaver;
import org.altbeacon.beacon.startup.BootstrapNotifier;
import org.altbeacon.beacon.startup.RegionBootstrap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by bigstone on 23/12/14.
 */
public class BeaconLocApplication extends Application implements BootstrapNotifier, RangeNotifier {

    List<BootstrapNotifier> regionNotifiers = new ArrayList<BootstrapNotifier>();
    List<RangeNotifier> rangeNotifiers = new ArrayList<RangeNotifier>();

    private static final String TAG = "BeaconLocApplication";
    private Region mAllBeaconsRegion;
    private BackgroundPowerSaver mBackgroundPowerSaver;
    private BeaconManager mBeaconManager;
    private RegionBootstrap mRegionBootstrap;

    public void onCreate() {
        verifyBluetooth();

        mAllBeaconsRegion = new Region("all beacons", null, null, null);
        mBeaconManager = BeaconManager.getInstanceForApplication(this);

        mBeaconManager = BeaconManager.getInstanceForApplication(this);
        mBackgroundPowerSaver = new BackgroundPowerSaver(this);
        mRegionBootstrap = new RegionBootstrap(this, mAllBeaconsRegion);

        mBeaconManager.getBeaconParsers().add(new BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
        mBeaconManager.setForegroundBetweenScanPeriod(100);
        mBeaconManager.setForegroundScanPeriod(1500);
    }

    private void verifyBluetooth() {

        try {
            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Please enable bluetooth in settings and restart this application.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {

                        System.exit(0);
                    }
                });
                builder.show();
            }
        }
        catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                @Override
                public void onDismiss(DialogInterface dialog) {

                    System.exit(0);
                }

            });
            builder.show();

        }
    }

    @Override
    public void didEnterRegion(Region region) {
        Log.i("beacon", "didEnterRegion");
        for (BootstrapNotifier notifier : regionNotifiers){
            notifier.didEnterRegion(region);
        }

        try {
            Log.d(TAG, "entered region.  starting ranging");
            mBeaconManager.startRangingBeaconsInRegion(mAllBeaconsRegion);
            mBeaconManager.setRangeNotifier(this);
        } catch (RemoteException e) {
            Log.e(TAG, "Cannot start ranging");
        }
    }

    @Override
    public void didExitRegion(Region region) {
        Log.i("beacon", "didExitRegion");
        for (BootstrapNotifier notifier : regionNotifiers){
            notifier.didExitRegion(region);
        }
    }

    @Override
    public void didDetermineStateForRegion(int i, Region region) {
        Log.i("beacon", "didDetermineStateForRegion");
        for (BootstrapNotifier notifier : regionNotifiers){
            notifier.didDetermineStateForRegion(i, region);
        }
    }

    @Override
    public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
        Log.i("beacon", "didRangeBeaconsInRegion");

        for (RangeNotifier notifier : rangeNotifiers){
            notifier.didRangeBeaconsInRegion(beacons, region);
        }
    }

    public void registerRangeNotifier(RangeNotifier notifier){
        Log.i("beacon", "registerRangeNotifer");
        if (rangeNotifiers.contains(notifier)){
            return;
        }
        rangeNotifiers.add(notifier);
    }

    public void removeRangeNotifier(RangeNotifier notifier){
        Log.i("beacon", "registerRangeNotifer");

        boolean result = rangeNotifiers.remove(notifier);
    }
}
