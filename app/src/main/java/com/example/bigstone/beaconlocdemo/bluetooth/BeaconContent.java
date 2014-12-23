package com.example.bigstone.beaconlocdemo.bluetooth;

import android.util.Log;


import com.example.bigstone.beaconlocdemo.util.GlobalConfig;

import org.altbeacon.beacon.Beacon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import hk.ust.cse.indoorloc.bluetooth.LocationBeacon;
import hk.ust.cse.indoorloc.structure.Point;
import hk.ust.cse.indoorloc.util.BeaconUtil;

/**
 * Created by bigstone on 10/5/14.
 */
public class BeaconContent {
    public static List<BeaconItem> ITEMS = new ArrayList<BeaconItem>();
    public static Map<String, BeaconItem> ITEM_MAP = new HashMap<String, BeaconItem>();
    public static List<LinkedList<LocationBeacon>> beaconList = new ArrayList<LinkedList<LocationBeacon>>();
    private final static int REFERENCED_BEACON_NUMBER = 3;


    private static void addItem(BeaconItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.id, item);
//
//        BeaconCollection.ITEMS.add(item);
//        BeaconCollection.ITEM_MAP.put(item.id, item);
    }

    public static List<LocationBeacon> extractNearestBeacon(List<LocationBeacon> beacons){
        BeaconUtil.sortLocationBeaconsByDistance(beacons);
        if (beacons.size() < REFERENCED_BEACON_NUMBER){
            return beacons;
        } else {


            return beacons.subList(0, REFERENCED_BEACON_NUMBER);
        }
    }

    public static void updateLocalBeaconDistance(Collection<Beacon> beacons){
        List<LocationBeacon> focusBeacons = BeaconContent.getLocationBeacons();
        refreshBeaconReference(focusBeacons);
        for(LocationBeacon lbeacon : focusBeacons) {
            for (Beacon beacon : beacons){
                if (BeaconUtil.isSameLocationBeacon(beacon, lbeacon)){
                    lbeacon.distance = (float)beacon.getDistance();
                    break;
                }
            }

        }

    }

    public static void refreshBeaconReference(List<LocationBeacon> focusBeacons){
        for (LocationBeacon b : focusBeacons){
            b.isReference = false;
        }
    }

    public static List<LocationBeacon> extractNearestBeacon(List<LocationBeacon> beacons, int number){
        BeaconUtil.sortLocationBeaconsByDistance(beacons);
        if (beacons.size() < number){
            return beacons;
        } else {


            return beacons.subList(0, number);
        }
    }

    public static void refreshBeacons(List<Beacon> beaconList){
        Log.i("Strange", "Refresh BeaconList...");

        ITEMS.clear();
        ITEM_MAP.clear();
//        BeaconCollection.ITEMS.clear();
//        BeaconCollection.ITEM_MAP.clear();
        for (int i = 0; i < beaconList.size(); i++){
            addItem(new BeaconItem(String.valueOf(i+1), beaconList.get(i)));
        }
    }

    public static class BeaconItem {
        public String id;
        public String content;
        public Beacon beacon;
        public BeaconItem(String id, Beacon beacon) {
            this.id = id;
            this.beacon = beacon;
            content = BeaconUtil.digestBeacon(beacon);
        }


        @Override
        public String toString() {
            if (isFocused(this.beacon)){
                return content + "     **";
            } else {
                return content;
            }


        }
    }

    public static boolean isFocused(Beacon beacon){
        List<LocationBeacon> focusBeacons = getLocationBeacons();
        boolean focused = false;

        for(LocationBeacon lbeacon : focusBeacons) {
            if (BeaconUtil.isSameLocationBeacon(beacon, lbeacon)){
                focused  = true;
                break;
            }
        }

        return focused;
    }

    public static void initialData(){
        for (int i = 0; i < GlobalConfig.floorNameArray.size(); i++) {
            beaconList.add(new LinkedList<LocationBeacon>());
        }

        try {
            loadBeaconRecord();
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static LocationBeacon findLocationBeacon(Beacon beacon){
        List<LocationBeacon> focusBeacons = getLocationBeacons();
        LocationBeacon focused = null;

        for(LocationBeacon lbeacon : focusBeacons) {
            if (BeaconUtil.isSameLocationBeacon(beacon, lbeacon)){
                focused  = lbeacon;
                break;
            }
        }

        return focused;
    }



    private static void loadBeaconRecord() throws NumberFormatException, IOException{
        FileReader reader = new FileReader(GlobalConfig.beaconFilePath);
        BufferedReader br = new BufferedReader(reader);
        String rtRecord = null;
        while ((rtRecord = br.readLine()) != null) {

            String rtInfo[] = rtRecord.split(",");
            if(rtInfo.length < 8){
                continue;
            }
            Point position = new Point(rtInfo[1], rtInfo[2], rtInfo[0].trim());
            float radius = Float.valueOf(rtInfo[3]);

            int loc = GlobalConfig.floorFolderArray.indexOf(rtInfo[0].trim());


            beaconList.get(loc).add(new LocationBeacon(position, radius, rtInfo[4], rtInfo[5], rtInfo[6], rtInfo[7]));
        }

        reader.close();
    }
    public static void saveBeaconRecord() {

        try {
            File beaconFile = new File(GlobalConfig.beaconFilePath);
            if(!beaconFile.exists()) {
                beaconFile.getParentFile().mkdirs();
            }
            FileWriter fw = new FileWriter(GlobalConfig.beaconFilePath);

            for (List<LocationBeacon> areaBeacons : beaconList) {
                for (LocationBeacon b : areaBeacons) {
                    fw.write(b.toString()+"\n");
                }
            }

            fw.flush();
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }

    public static List<LocationBeacon> getLocationBeacons(){
        List<LocationBeacon> list = new ArrayList<LocationBeacon>();

        for (int i = 0; i < beaconList.size(); i++) {
            list.addAll(beaconList.get(i));
        }

        return list;
    }

    public static void removeLocationBeacon(LocationBeacon lbeacon){
        for (int i = 0; i < beaconList.size(); i++) {
            List<LocationBeacon> locationBeacons = beaconList.get(i);
            int index = locationBeacons.indexOf(lbeacon);
            if (index != -1){
                locationBeacons.remove(index);
            }
        }
    }


    // should optimaze this function
    public static List<LocationBeacon> findRegisteredBeacons(Collection<Beacon> beacons){

        List<LocationBeacon> focusBeacons = getLocationBeacons();
        List<LocationBeacon> registeredBeacons = new ArrayList<LocationBeacon>();

        for(LocationBeacon lbeacon : focusBeacons) {
            for (Beacon beacon : beacons){
                if (BeaconUtil.isSameLocationBeacon(beacon, lbeacon)){
                    registeredBeacons.add(lbeacon);
                    break;
                }
            }

        }

        return registeredBeacons;
    }
}
