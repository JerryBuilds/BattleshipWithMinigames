package chauvu_CSCI201_FinalProject;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

public class Globals {
	public static String[] fleetTypes = {"Redekopp", "Crowley", "Miller", "Kempe"};
	public static HashMap<String, Integer> Fleet2Integer = new HashMap<String, Integer>();
	public static int shipTypeNum = 3;
	public static int[] shipNumber = {1, 1, 1};
	public static int[] shipSize = {2, 3, 4};
	public static Color[] shipColor = {Color.red, Color.blue, Color.green};

	public static String[] horizontalShipImages = {"Image/2h.png", "Image/3h.png", "Image/4h.png", "Image/5h.png"};
	public static HashMap<Integer, String> horizontalShipImagePaths = new HashMap<Integer, String>();
	public static String[] verticalShipImages = {"Image/2v.png", "Image/3v.png", "Image/4v.png", "Image/5v.png"};
	public static HashMap<Integer, String> verticalShipImagePaths = new HashMap<Integer, String>();

	public static String[] RedekoppShips = {"R1","R2","R3","R4"};
	public static HashMap<String, Integer> RedekoppShipNumber = new HashMap<String, Integer>();
	public static String[] CrowleyShips = {"C1","C2","C3","C4"};
	public static HashMap<String, Integer> CrowleyShipNumber = new HashMap<String, Integer>();
	public static String[] MillerShips = {"M1","M2","M3","M4"};
	public static HashMap<String, Integer> MillerShipNumber = new HashMap<String, Integer>();
	public static String[] KempeShips = {"K1","K2","K3","K4"};
	public static HashMap<String, Integer> KempeShipNumber = new HashMap<String, Integer>();
	public static ArrayList<String[]> FleetList = new ArrayList<String[]>();
	//super powers: hit nearby 4 cells, 2 consecutive shots, shot reflection, shot block
	public static int powerNum = 5;

	
	public Globals(){
		//Fleet2Integer
		for(int i=0; i<fleetTypes.length; i++){
			Fleet2Integer.put(fleetTypes[i], i);
		}
		//ShipNumer
		for(int i=0; i<shipTypeNum; i++){
			RedekoppShipNumber.put(RedekoppShips[i], shipNumber[i]);
			CrowleyShipNumber.put(CrowleyShips[i], shipNumber[i]);
			MillerShipNumber.put(MillerShips[i], shipNumber[i]);
			KempeShipNumber.put(KempeShips[i], shipNumber[i]);
		}
		
	
		for(int i = 2; i < 6; i++){
			horizontalShipImagePaths.put(i, horizontalShipImages[i-2]);
			verticalShipImagePaths.put(i, verticalShipImages[i-2]);
		}
		
		FleetList.add(RedekoppShips);
		FleetList.add(CrowleyShips);
		FleetList.add(MillerShips);
		FleetList.add(KempeShips);
	}
}


