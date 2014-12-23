package com.example.bigstone.beaconlocdemo.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import hk.ust.cse.indoorloc.layout.RouteRegular;
import hk.ust.cse.indoorloc.structure.Point;
import hk.ust.cse.indoorloc.structure.Segment;

/**
 * Created by bigstone on 22/12/14.
 */
public class FileConfigSaverLoader {

    public static List<LinkedList<Segment>> loadWallRecord(String filePath) throws NumberFormatException, IOException {
        File beaconFile = new File(GlobalConfig.wallFilePath);
        if(!beaconFile.exists()) {
            return null;
        }

        List<LinkedList<Segment>> wallList = new ArrayList<LinkedList<Segment>>();
        for (int i = 0; i < GlobalConfig.floorNameArray.size(); i++) {
            wallList.add(new LinkedList<Segment>());
        }
        FileReader reader = new FileReader(filePath);
        BufferedReader br = new BufferedReader(reader);
        String rtRecord = null;
        while ((rtRecord = br.readLine()) != null) {

            String rtInfo[] = rtRecord.split(",");
            if(rtInfo.length < 5){
                continue;
            }
            String areaId = rtInfo[0].trim();
            Point sPoint = new Point(rtInfo[1], rtInfo[2], areaId);
            Point ePoint = new Point(rtInfo[3], rtInfo[4], areaId);

            int loc = GlobalConfig.floorFolderArray.indexOf(rtInfo[0].trim());


            wallList.get(loc).add(new Segment(sPoint, ePoint));
        }

        reader.close();

        return wallList;
    }
    public static List<LinkedList<Segment>> loadRouteRecord(String filePath) throws NumberFormatException, IOException{
        File beaconFile = new File(GlobalConfig.routeFilePath);
        if(!beaconFile.exists()) {
            return null;
        }

        List<LinkedList<Segment>> routeList = new ArrayList<LinkedList<Segment>>();
        for (int i = 0; i < GlobalConfig.floorNameArray.size(); i++) {
            routeList.add(new LinkedList<Segment>());
        }
        FileReader reader = new FileReader(filePath);
        BufferedReader br = new BufferedReader(reader);
        String rtRecord = null;
        while ((rtRecord = br.readLine()) != null) {

            String rtInfo[] = rtRecord.split(",");
            if(rtInfo.length < 5){
                continue;
            }
            String areaId = rtInfo[0].trim();

            Point sPoint = new Point(rtInfo[1], rtInfo[2], areaId);
            Point ePoint = new Point(rtInfo[3], rtInfo[4], areaId);

            int loc = GlobalConfig.floorFolderArray.indexOf(rtInfo[0].trim());



            routeList.get(loc).add(new Segment(sPoint, ePoint));
        }

        reader.close();
        return routeList;
    }
    public static void saveWallRecord(List<LinkedList<Segment>> wallList, String filePath) {
        RouteRegular r = new RouteRegular();
        r.regularizeRoute(wallList);
        try {
            File beaconFile = new File(filePath);
            if(!beaconFile.exists()) {
                beaconFile.getParentFile().mkdirs();
            }
            FileWriter fw = new FileWriter(filePath);
            for (int i = 0; i < wallList.size(); i++) {
                List<Segment> areaSegments = wallList.get(i);
                for (Segment w : areaSegments) {
                    fw.write(GlobalConfig.floorFolderArray.get(i) + "," + w.getString()+"\n");
                }
            }

            fw.flush();
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
    public static void saveRouteRecord(List<LinkedList<Segment>> routeList, String filePath) {
        RouteRegular r = new RouteRegular();
        r.regularizeRoute(routeList);
        try {
            File beaconFile = new File(filePath);
            if(!beaconFile.exists()) {
                beaconFile.getParentFile().mkdirs();
            }
            FileWriter fw = new FileWriter(filePath);
            for (int i = 0; i < routeList.size(); i++) {
                List<Segment> areaSegments = routeList.get(i);
                for (Segment w : areaSegments) {
                    fw.write(GlobalConfig.floorFolderArray.get(i) + "," + w.getString()+"\n");
                }
            }

            fw.flush();
            fw.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

    }
}
