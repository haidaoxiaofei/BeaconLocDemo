package com.example.bigstone.beaconlocdemo.util;

import android.os.Environment;

import java.util.Arrays;
import java.util.List;

import hk.ust.cse.indoorloc.structure.Segment;

/**
 * Created by bigstone on 10/11/14.
 */
public class GlobalConfig {
    public static final int COMPRESS_RATE = 2;
    public static final float CONVERT_DEGREE = 0;
    public static final int MAP_SCALE = 67;
    public static List<String> floorNameArray = Arrays.asList("Ground");

    public static List<String> floorFolderArray = Arrays.asList("1000");
    public static String PRE_PATH = Environment.getExternalStorageDirectory()+"/gmission_data/.map/";
    private static String OUTPUT_BASE_PATH = Environment.getExternalStorageDirectory()+"/gmission_data/";


    public static String beaconFilePath = OUTPUT_BASE_PATH + "beacon/beacon.txt";
    public static String wallFilePath = OUTPUT_BASE_PATH + "layout/wall.txt";
    public static String routeFilePath = OUTPUT_BASE_PATH + "layout/route.txt";


    public static boolean displayWall = false;
    public static boolean displayRoute = false;
    public static boolean displayBeacon = true;
    public static boolean displayRange = true;
    public static boolean displayParticle = true;

    public static boolean attach = false;
    public static Segment.Type drawMode = Segment.Type.WALL;

    public static int INTERESTED_MESSAGE_TYPE = 1;
}
