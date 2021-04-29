package dev.swarm.dji.com.swarmdev;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;
import static java.lang.Math.ceil;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.tan;
import static java.lang.Math.toRadians;
import static java.util.Collections.max;
import static java.util.Collections.min;
import static java.util.Collections.reverse;
import static java.util.Collections.swap;

/**
 * Created by Walter Morawa on 11/11/2016.
 */
public class WaypointCalculation {

    static boolean switchPoints;

    /**
     * The waypoint algorithm for one drone
     * @param polylist list of {@link LatLng latitudes and longtitudes}
     *                 given by the user to be converted to <b>UTM</b>
     * @param sidelap amount of overlap between each leg of the flight
     * @param polydegreeUser user inputted slope in degrees
     * @param altitude the desired altitude
     * @return an ArrayList of {@link LatLng} waypoints
     */
    static ArrayList<LatLng> onedroneWP(ArrayList<LatLng> polylist, double sidelap,
                                        double polydegreeUser, double altitude) {
        ArrayList<LatLng> wpAlgorithmList = new ArrayList<>();

        //Check if points cross UTM zone boundary. This will cause error
        boolean utmZoneCrossed = false;
        boolean allLongsPositive = false;
        boolean allLongsNegative = false;
        for (LatLng latLng: polylist){
            if (abs(latLng.longitude%6)>3){
                allLongsPositive = true;
                Log.d("test1512 positives", Boolean.toString(allLongsPositive));
                Log.d("test1512 positives", Double.toString(latLng.longitude%6));

            } else {
                allLongsNegative = true;
                Log.d("test1512 negative", Boolean.toString(allLongsNegative));
                Log.d("test1512 negative", Double.toString(latLng.longitude%6));

            }
            if (allLongsNegative && allLongsPositive){
                utmZoneCrossed = true;
                Log.d("test1512", Boolean.toString(utmZoneCrossed));
            }
        }

        Log.d("test166y", "did we make it?13");
        //Convert polylist input (arraylist LatLng) to UTM (arraylist LatLng)
        ArrayList<Double> polylistUTM = WSGToAndFromUTMConversion.convertWSGlisttoUTM(polylist);
        Log.d("test166y", "did we make it?14");

        ArrayList<WSGToAndFromUTMConversion> zoneLetterList = new ArrayList<>();

      for (int i = 0; i < polylistUTM.size(); i++){
            Log.d("test198", Double.toString(polylistUTM.get(i)));
        }

        for (LatLng l: polylist){
            Log.d("test198", Double.toString(l.latitude));
            Log.d("test198", Double.toString(l.longitude));
        }

        //Convert polylistUTM ArrayList<Double> to double
        double points [] = arraylistToArray(polylistUTM);
        Log.d("test166y", "did we make it?15");

        //Check that area isn't too small.
        ArrayList<Double> pointsX = splitPoints(points, 'x');
        ArrayList<Double> pointsY = splitPoints(points, 'y');
        double phantom3lens= 1.71111111122;
        double width = altitude* phantom3lens;
        double shift = shiftFun(width, sidelap);
        // Determine user slope from degrees input.
        double slopeUser = tan(toRadians(polydegreeUser));
        Log.d("test166y", "did we make it?16");

        // i. Determine slope perpendicular to slopeUser
        double slopeperp = tan(toRadians(polydegreeUser+90));

        // ii. Determine intersection points made from boundary points
        // with slopeUser and a line with slope perpendicular to slope User.
        ArrayList<Double> xPerp = new ArrayList<>();
        ArrayList<Double> yPerp = new ArrayList<>();
        for (int i = 0; i < pointsX.size(); i++){
            xPerp.add((pointsY.get(i)- slopeUser*pointsX.get(i))/(slopeperp- slopeUser));
            yPerp.add(slopeperp*((pointsY.get(i)- slopeUser*pointsX.get(i))/(slopeperp- slopeUser)));
        }
        // iii. Declare max and min variables to be used in calculation as starting point
        double xMax = max(xPerp);
        double xMin = min(xPerp);
        double yMax = max(yPerp);
        double yMin = min(yPerp);
        Log.d("test166y", "did we make it?17");

        // iv. Calcualte n, number of waypoint lines.
        double distperp = pow(pow(xMax-xMin,2)+ pow(yMax-yMin,2), .5);
        xPerp.clear();
        yPerp.clear();
        pointsX.clear();
        pointsY.clear();
        if (distperp<shift) {
            //don't run the code
        } else if (utmZoneCrossed){
            //Check if polylist crosses UTM zone boundary
            Log.d("test1512", "Uh oh UTM zone Boundary");
        }
        else {
            Log.d("test166y", "did we make it?18");
            //Run the WP algorithm on inputs and points.
            double wp[] = WP(points, sidelap, polydegreeUser, altitude);
            Log.d("test166y", "did we make it?19");

            for (double d: wp){
                Log.d("test1099", Double.toString(d));
            }

            //Convert the WPs from UTM back to WSG
            for (int i = 0; i < wp.length - 1; i += 2) {
                ArrayList<String> missionary = new ArrayList<>();
                WSGToAndFromUTMConversion utmObject = new WSGToAndFromUTMConversion(polylist.get(0).latitude, polylist.get(0).longitude);
                Log.d("test166y", "did we make it?20");

                missionary.add(Integer.toString(utmObject.zone));
                missionary.add(" ");
                missionary.add(Character.toString(utmObject.letter));
                missionary.add(" ");
                missionary.add(Double.toString(wp[i]));
                missionary.add(" ");
                missionary.add(Double.toString(wp[i + 1]));

                String input = missionary.get(0) + missionary.get(1) + missionary.get(2) + missionary.get(3) + missionary.get(4) + missionary.get(5) + missionary.get(6);


                LatLng wpLatLng = WSGToAndFromUTMConversion.UTMtoWSGConversion(input);
                Log.d("test166y", "did we make it?21");

                //Store each wp as a list
                wpAlgorithmList.add(wpLatLng);
                missionary.clear();

            }
        }
        return wpAlgorithmList;
    }

    /**
     * Parallel Flight Algorithm
     * @param numdrones The number of drones
     * @param OrderedPoints an array of points
     * @param slopeUser user inputted slope in radians
     * @return {@code ArrayList parallelOut} variable
     */
    public static ArrayList<ArrayList<Double>> parallel (int numdrones,
                                                         double [] OrderedPoints, double slopeUser) {
        ArrayList<Double> orderedPointsX = splitPoints(OrderedPoints, 'x');
        ArrayList<Double> orderedPointsY = splitPoints(OrderedPoints, 'y');
        //Initialize parallel array
        ArrayList<ArrayList<Double>>parallelOut = new ArrayList<>();
        // for each drone
        for (int i=0; i<numdrones; i++){
            ArrayList<Double> WPfile = new ArrayList<>();
            if (i==0){ // Add first point to first drone
                WPfile.add(orderedPointsX.get(0));
                WPfile.add(orderedPointsY.get(0));
            }

            //Declare boolean for every other iteration
            boolean parallelswitch = false;
            // for every two sets of x,y points
            for (int j = 2*i +1; j < orderedPointsX.size()-1; j+=2*numdrones){
                if (slopeUser >= 0 && slopeUser < 90) { //positive slope
                    if (!parallelswitch) {
                        // min and max are the same for x and y
                        WPfile.add(min(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(min(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        WPfile.add(max(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(max(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        parallelswitch = true;
                    }
                    else {
                        WPfile.add(max(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(max(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        WPfile.add(min(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(min(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        parallelswitch = false;
                    }
                }

                if (slopeUser >= 90) { //negative slope
                    if (!parallelswitch) {
                        // min and max are the same for x and y
                        WPfile.add(min(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(max(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        WPfile.add(max(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(min(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        parallelswitch = true;
                    }
                    else {
                        WPfile.add(max(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(min(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        WPfile.add(min(orderedPointsX.get(j), orderedPointsX.get(j + 1)));
                        WPfile.add(max(orderedPointsY.get(j), orderedPointsY.get(j + 1)));
                        parallelswitch = false;
                    }
                }
            }

            parallelOut.add(WPfile);
        } // for loop

        return parallelOut;
    } // End parallel method




    /**
     * Extracts an arraylist of X or Y coordinates from an array of points
     * @param points the {@code Array} of X & Y coordinates being passed onto the method
     * @param pointsSplit a character specifying X or Y coordinates to be extracted
     * @return the arraylist of coordinates
     */
    private static ArrayList<Double> splitPoints(double[] points, char pointsSplit)
    { // for loop to split x points
        int counter;
        if (Character.toLowerCase(pointsSplit) == 'x')
            counter = 0;
        else counter = 1;
        ArrayList<Double> pointsX = new ArrayList<>();
        for (int i=counter; i<points.length; i+=2){
            for (int j=counter; j<points.length; j+=2) {
                if (i!=j){
                    if (points[i] == points[j]) {
                        points[i] = points[i] + 1;
                        if (Character.toLowerCase(pointsSplit) == 'x') {
                            i = counter;
                            j = counter;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < points.length; i++) {
            if (i % 2 == counter) {
                pointsX.add(points[i]);
            }
        }
        return pointsX;
    }

    /**
     * Calculates the slope of the polygon
     * @param pointsX an {@link ArrayList} of type {@code double} of X coordinates
     * @param pointsY an {@link ArrayList} of type {@code double} of Y coordinates
     * @return the polygon slope
     */
    public static ArrayList<Double> slopeFun (ArrayList<Double> pointsX, ArrayList<Double> pointsY){
        ArrayList<Double> polyslope = new ArrayList<>();
        for (int i = 0; i < pointsX.size() - 1; i++) {
            polyslope.add((pointsY.get(i + 1) - pointsY.get(i)) / (pointsX.get(i + 1) - pointsX.get(i)));
        }
        polyslope.add((pointsY.get(0) - pointsY.get(pointsY.size() - 1)) / (pointsX.get(0) - pointsX.get(pointsX.size() - 1)));
        return polyslope;
    }

    /**
     * Calculates the intercepts of boundary lines
     * @param pointsX an {@linkplain ArrayList} of {@code double} x coordinates
     * @param pointsY an {@Link ArrayList} of {@code double} y coordinates
     * @return an ArrayList of {@code double} intercept points
     */
    public static ArrayList<Double> intfun (ArrayList<Double> pointsX, ArrayList<Double> pointsY){
        ArrayList<Double> polyInt = new ArrayList<>();
        for (int i = 0; i < pointsX.size() - 1; i++) {
            polyInt.add(pointsY.get(i) - (pointsY.get(i + 1) - pointsY.get(i)) / (pointsX.get(i + 1) - pointsX.get(i)) * pointsX.get(i));
        }
        polyInt.add(pointsY.get(0) - (pointsY.get(0) - pointsY.get(pointsY.size() - 1)) / (pointsX.get(0) - pointsX.get(pointsX.size() - 1)) * pointsX.get(0));
        return polyInt;
    }

    /**
     * Determine each boundary line's X & Y coordinate mins for the purpose of bounding points.
     * @param points an Array of X or Y points
     * @return an ArrayList of points
     */
    public static ArrayList<Double> polyMinFun (ArrayList<Double> points){
        ArrayList<Double> polyMin = new ArrayList<>();
        for (int i = 0; i < points.size()-1; i++) {
            polyMin.add(min(points.get(i), points.get(i+1)));
        }
        polyMin.add(min(points.get(0), points.get(points.size()-1)));
        return polyMin;
    }

    /**
     * Determine each boundary line's X & Y coordinate maximums for the purpose of bounding points.
     * @param points an Array of X or Y points
     * @return an ArrayList of points
     */
    public static ArrayList<Double> polyMaxFun (ArrayList<Double> points){
        ArrayList<Double> polyMax = new ArrayList<>();
        for (int i = 0; i < points.size()-1; i++) {
            polyMax.add(max(points.get(i), points.get(i+1)));
        }
        polyMax.add(max(points.get(0), points.get(points.size()-1)));
        return polyMax;
    }

    /**
     * Converts a {@code List} of doubles to an {@code Array} of doubles
     * @param doubles the List of doubles
     * @return the arraylist of doubles
     */
    public static double[] arraylistToArray(List<Double> doubles)
    {
        double[] ret = new double[doubles.size()];
        for (int n=0; n < doubles.size();n++) {
            ret[n] = doubles.get(n);
        }
        return ret;
    }

    /**
     * @param WIDTH the width
     * @param sidelap amount of overlap between each leg of the flight
     * @return the shift
     */
    public static double shiftFun(double WIDTH, double sidelap){
        double shift = WIDTH * (1 - sidelap);
        return shift;
    }

    /**
     * Calculates the number of waypoint lines
     * @param oaymax overall Y max Coordinate of boundary points for determining number of
     *               waypoint lines
     * @param oaymin overall Y min coordinate of boundary points for determining number of waypoint lines
     * @param WIDTH the width
     * @param sidelap amount of overlap between each leg of the flight
     * @return
     */
    public static int gernfun (double oaymax, double oaymin, double WIDTH, double sidelap){
        double distperp = oaymax - oaymin;

        double n = distperp / (WIDTH * (1 - sidelap));
        int gern = (int) ceil(n);
        return gern;
    }

    /**
     * Calculate the area of a polygon.
     * @see <a href>http://www.mathopenref.com/coordpolygonarea2.html</a>
     * @param polylist list of polygon X & Y coordinates
     * @return the total area in hectares
     */
    public static double polygonArea (ArrayList<LatLng> polylist){
        ArrayList<Double> dwpAlgorithmList = WSGToAndFromUTMConversion.convertWSGlisttoUTM(polylist);
        double points [] = arraylistToArray(dwpAlgorithmList);
        ArrayList<Double> pointsX = splitPoints(points, 'x');
        ArrayList<Double> pointsY = splitPoints(points, 'y');
        int j = pointsX.size()-1;
        double area = 0;
        for (int i=0; i<pointsX.size(); i++){
            area += (pointsX.get(j)+pointsX.get(i))*(pointsY.get(j)-pointsY.get(i));
            j=i; //j is previous vertex to i
        }
        //double areaf = area/2; //in m^2
        double areaH = area/20000; // in hecatres
        double areai = abs(round(areaH*1000));
        return areai/1000; // final answer in hectares
    }

    /**
     * Calculates the distance which the drone will have to fly.
     * @param wpAlgorithmList the list of waypoints from the algorithm
     * @return the distance the drone will have to fly in meters
     */
    public static double droneDistFun(ArrayList<LatLng> wpAlgorithmList){
        ArrayList<Double> dwpAlgorithmList = WSGToAndFromUTMConversion.convertWSGlisttoUTM(wpAlgorithmList);
        double points [] = arraylistToArray(dwpAlgorithmList);
        ArrayList<Double> pointsX = splitPoints(points, 'x');
        ArrayList<Double> pointsY = splitPoints(points, 'y');
        double droneDist = 0;
        for (int i=0; i<pointsX.size()-1; i+=2){
            droneDist = droneDist + pow(pow(pointsX.get(i+1)-pointsX.get(i),2)+ pow(pointsY.get(i+1)-pointsY.get(i),2), .5);
            droneDist = droneDist + pow(pow(pointsX.get(1)-pointsX.get(pointsX.size()-1),2)+ pow(pointsY.get(1)-pointsY.get(pointsY.size()-1),2), .5);
        }
        return round(droneDist); //final answer in meters
    }

    /**
     * The waypoint algorithm
     * @param points an array of boundary points
     * @param sidelap amount of overlap between each leg of the flight
     * @param polydegreeUser the slope selected by the user in degrees
     * @param altitude the altitude
     * @return
     */

    public static double [] WP (final double [] points, final double sidelap, final double polydegreeUser,
                                final double altitude) {

        // for loop to split x and y points
        ArrayList<Double> pointsX = splitPoints(points, 'x');
        ArrayList<Double> pointsY = splitPoints(points, 'y');
        Log.d("test166y", "did we make it?22");

        // 1. Determine slope and intercept of boundary lines.
        ArrayList<Double> polyslope = slopeFun(pointsX, pointsY);
        ArrayList<Double> polyint = intfun(pointsX, pointsY);
        Log.d("test166y", "did we make it?23");

        // 2. Determine each boundary line's y-coordinate and x-coordinate min & max for the purpose of bounding points.
        ArrayList<Double> ymin = polyMinFun(pointsY);
        ArrayList<Double> ymax = polyMaxFun(pointsY);
        ArrayList<Double> xmin = polyMinFun(pointsX);
        ArrayList<Double> xmax = polyMaxFun(pointsX);
        Log.d("test166y", "did we make it?24");

        // 3. Determine area (height and width) that camera (based on drone type) can capture at any given altitude.
        double width = altitude* 1.71111111122;
        //TODO add drone lens constants. The following is for Phantom 3 advanced and professional.
        //double height = altitude* (1.28333333325);

        // 4. Declare arrays for finding x and y coordinates for waypoints.
        ArrayList<Double> x = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();

        // 5. Declare final ArrayList to be returned with x and y waypoint coordinates.
        ArrayList<Double> orderedPointsX = new ArrayList<>();
        ArrayList<Double> orderedPointsY = new ArrayList<>();
        // 5. The remainding code is used to determine x and y coordinates for waypoints depending on the user inputs.

        // a. When the user inputted slope is 0:
        if (polydegreeUser == 0){
            Log.d("test166y", "did we make it?25");

            // Define overall y min and y max of boundary points for determining number of waypoint lines.
            double oaymin = min(pointsY);
            double oaymax = max(pointsY);
            Log.d("test166y", "did we make it?26");

            // Calcualte n, total number of waypoint lines.
            int gern = gernfun(oaymax, oaymin, width, sidelap);
            Log.d("test166y", Double.toString(oaymax));
            Log.d("test166y", Double.toString(oaymin));
            Log.d("test166y", Double.toString(width));
            Log.d("test166y", Double.toString(sidelap));

            // Determine distance of y intercepts between waypoint lines.
            double shift = shiftFun(width, sidelap);
            Log.d("test166y", "did we make it?28");

            // for loop to determine intersection points between boundary lines and waypoint lines.
            for (int j = 0; j < gern; j++){ // Waypoint lines
                for (int i = 0; i < pointsX.size(); i++) {   // Boundary lines
                    double indexj = j;
                    double intersx = (oaymin + j*shift-pointsY.get(i)+polyslope.get(i)*pointsX.get(i))/polyslope.get(i);
                    double intersy = polyslope.get(i)*
                            ((oaymin + j*shift-pointsY.get(i)+polyslope.get(i)*pointsX.get(i)) /polyslope.get(i) -pointsX.get(i))+pointsY.get(i);

                    // Bound points by predefined xmax and xmin.
                    if (intersx >= xmin.get(i)-.01){
                        if (intersx <= xmax.get(i)+.01){
                            if (intersy >= ymin.get(i)-.01){
                                if(intersy <= ymax.get(i)+.01){
                                    y.add(intersy);
                                    y.add(indexj);
                                    x.add(intersx);
                                    x.add(indexj);

                                }
                            }
                        }
                    }
                } // End boundary line for loop
            } // End waypoint line for loop

            // Order Waypoints
            // Determine indicies of last intersection point for each way point line.
            // Declare array to hold indicies.
            int [] arrofI = new int[gern-1];
            Log.d("test166y", "did we make it?31");

            // Set initial mostRecentISeen to the first point.
            int mostRecentISeen = (int)Math.round(x.get(1));
            // Determine indicies, where each index represents the last intersection on each waypoint line.
            for (int i = 3; i < x.size(); i += 2){
                if (x.get(i) != mostRecentISeen){
                    arrofI[mostRecentISeen] = i-2;
                    mostRecentISeen = (int)Math.round(x.get(i));
                    Log.d("test166y", "did we make it?32");

                }
            }
            // Determine max and min for each waypoint line.
            // Store intersection points for first waypoint line.
            ArrayList<Double> bigorsmallx = new ArrayList<>();
            ArrayList<Double> bigorsmally = new ArrayList<>();
            Log.d("test166y", "did we make it?33");

            for (int i=1; i < x.size(); i+=2){
                if (i<=arrofI[0]){
                    bigorsmallx.add(x.get(i-1));
                    bigorsmally.add(y.get(i-1));
                }
            }

            // Store max then min of intersection points from the first waypoint line in new array, OrderedPoints.
            orderedPointsX.add(max(bigorsmallx));
            orderedPointsX.add(min(bigorsmallx));
            Log.d("test166y", "did we make it?34");

            int ixmax = bigorsmallx.indexOf(max(bigorsmallx));
            int ixmin = bigorsmallx.indexOf(min(bigorsmallx));
            orderedPointsY.add(bigorsmally.get(ixmax));
            orderedPointsY.add(bigorsmally.get(ixmin));
            Log.d("test166y", "did we make it?35");


            // Store intersection points of 2nd:2nd to last waypoint lines.
            ArrayList<Double> bigorsmallx2 = new ArrayList<>();
            ArrayList<Double> bigorsmally2 = new ArrayList<>();
            for (int j = 0; j < arrofI.length-1; j++){
                for (int i=1; i < x.size(); i+=2){
                    if (i>arrofI[j] & i<=arrofI[j+1]){
                        bigorsmallx2.add(x.get(i-1));
                        bigorsmally2.add(y.get(i-1));
                    }
                }
                Log.d("test166y", "did we make it?36");

                // Store max and min of 2nd:2nd to last waypoint lines in array, OrderedPoints.
                orderedPointsX.add(max(bigorsmallx2));
                orderedPointsX.add(min(bigorsmallx2));
                int ixmax2 = bigorsmallx2.indexOf(max(bigorsmallx2));
                int ixmin2 = bigorsmallx2.indexOf(min(bigorsmallx2));
                orderedPointsY.add(bigorsmally2.get(ixmax2));
                orderedPointsY.add(bigorsmally2.get(ixmin2));
                Log.d("test166y", "did we make it?37");

                bigorsmallx2.clear(); // Clear the array each time so that only values from each waypoint line are evaluated for max and min.
                bigorsmally2.clear();

            } // End for loop for storing intersection points.

            // Store intersection points for last waypoint line.
            ArrayList<Double> bigorsmallx3 = new ArrayList<>();
            ArrayList<Double> bigorsmally3 = new ArrayList<>();
            for (int i=1; i < x.size(); i+=2){
                if (i>arrofI[arrofI.length-1]){
                    bigorsmallx3.add(x.get(i-1));
                    bigorsmally3.add(y.get(i-1));
                    Log.d("test166y", "did we make it?38");

                }
            }

            // Store max and min of the last waypoint lines in array, OrderedPoints.
            orderedPointsX.add(max(bigorsmallx3));
            orderedPointsX.add(min(bigorsmallx3));
            int ixmax3 = bigorsmallx3.indexOf(max(bigorsmallx3));
            int ixmin3 = bigorsmallx3.indexOf(min(bigorsmallx3));
            orderedPointsY.add(bigorsmally3.get(ixmax3));
            orderedPointsY.add(bigorsmally3.get(ixmin3));
            Log.d("test166y", "did we make it?39");

            // Swap every other x point to ensure drone flies max--> min then min--> max.
            for (int i=2; i < orderedPointsX.size()-1;i+=4){
                swap(orderedPointsX, i, i+1);
                swap(orderedPointsY, i, i+1);
            }

        // b. When the user inputted slope is 90:

        } else if (polydegreeUser == 90){

            // Define overall x min and x max of boundary points for determining number of waypoint lines.
            double oaxmin = min(pointsX);
            double oaxmax = max(pointsX);

            // Calcualte n, total number of waypoint lines.
            int gern = gernfun(oaxmax, oaxmin, width, sidelap);

            // Determine distance of y intercepts between waypoint lines.
            double shift = shiftFun(width, sidelap);

            // for loop to determine intersection points between boundary lines and waypoint lines.
            for (int j = 0; j < gern; j++){ // Waypoint lines
                for (int i = 0; i < pointsX.size(); i++) {   // Boundary lines
                    double indexj = j;
                    double intersy = polyslope.get(i)*(oaxmin+j*shift)+polyint.get(i);
                    double intersx = (polyslope.get(i)* (oaxmin+j*shift) +polyint.get(i) - polyint.get(i))/polyslope.get(i);

                    // Bound points by predefined xmax and xmin.
                    if (intersx >= xmin.get(i)-.01){
                        if (intersx <= xmax.get(i)+.01) {
                            if (intersy >= ymin.get(i)-.01){
                                if(intersy <= ymax.get(i)+.01){
                                    y.add(intersy);
                                    y.add(indexj);
                                    x.add(intersx);
                                    x.add(indexj);
                                }
                            }
                        }
                    }
                } // End boundary line for loop
            } // End waypoint line for loop
            // Order Waypoints
            // Determine indicies of last intersection point for each way point line.
            // Declare array to hold indicies.
            int [] arrofI = new int[gern-1];
            // Set initial mostRecentISeen to the first point.
            int mostRecentISeen = (int)Math.round(y.get(1));
            // Determine indicies, where each index represents the last intersection on each waypoint line.
            for (int i = 3; i < y.size(); i += 2){
                if (y.get(i) != mostRecentISeen){
                    arrofI[mostRecentISeen] = i-2;
                    mostRecentISeen = (int)Math.round(y.get(i));
                }
            }
            // Determine max and min for each waypoint line.
            // Store intersection points for first waypoint line.
            ArrayList<Double> bigorsmally = new ArrayList<>();
            ArrayList<Double> bigorsmallx = new ArrayList<>();
            for (int i=1; i < y.size(); i+=2){
                if (i<=arrofI[0]){
                    bigorsmally.add(y.get(i-1));
                    bigorsmallx.add(x.get(i-1));
                }
            }

            // Store max then min of intersection points from the first waypoint line in new array, OrderedPoints.
            orderedPointsY.add(max(bigorsmally));
            orderedPointsY.add(min(bigorsmally));
            int iymax = bigorsmally.indexOf(max(bigorsmally));
            int iymin = bigorsmally.indexOf(min(bigorsmally));
            orderedPointsX.add(bigorsmallx.get(iymax));
            orderedPointsX.add(bigorsmallx.get(iymin));

            // Store intersection points of 2nd:2nd to last waypoint lines.
            ArrayList<Double> bigorsmally2 = new ArrayList<>();
            ArrayList<Double> bigorsmallx2 = new ArrayList<>();
            for (int j = 0; j < arrofI.length-1; j++){
                for (int i=1; i < y.size(); i+=2){
                    if (i>arrofI[j] & i<=arrofI[j+1]){
                        bigorsmally2.add(y.get(i-1));
                        bigorsmallx2.add(x.get(i-1));
                    }
                }

                // Store max and min of 2nd:2nd to last waypoint lines in array, OrderedPoints.
                orderedPointsY.add(max(bigorsmally2));
                orderedPointsY.add(min(bigorsmally2));
                int iymax2 = bigorsmally2.indexOf(max(bigorsmally2));
                int iymin2 = bigorsmally2.indexOf(min(bigorsmally2));
                orderedPointsX.add(bigorsmallx2.get(iymax2));
                orderedPointsX.add(bigorsmallx2.get(iymin2));

                bigorsmally2.clear(); // Clear the array each time so that only values from each waypoint line are evaluated for max and min.
                bigorsmallx2.clear();

            } // End for loop for storing intersection points.

            // Store intersection points for last waypoint line.
            ArrayList<Double> bigorsmally3 = new ArrayList<>();
            ArrayList<Double> bigorsmallx3 = new ArrayList<>();
            for (int i=1; i < y.size(); i+=2){
                if (i>arrofI[arrofI.length-1]){
                    bigorsmally3.add(y.get(i-1));
                    bigorsmallx3.add(x.get(i-1));
                }
            }

            // Store max and min of the last waypoint lines in array, OrderedPoints.
            orderedPointsY.add(max(bigorsmally3));
            orderedPointsY.add(min(bigorsmally3));
            int iymax3 = bigorsmally3.indexOf(max(bigorsmally3));
            int iymin3 = bigorsmally3.indexOf(min(bigorsmally3));
            orderedPointsX.add(bigorsmallx3.get(iymax3));
            orderedPointsX.add(bigorsmallx3.get(iymin3));

            // Finally, swap every other x point to ensure drone flies max--> min then min--> max.
            for (int i=2; i < orderedPointsY.size()-1;i+=4){
                swap(orderedPointsY, i, i+1);
                swap(orderedPointsX, i, i+1);
            }

// c. When the user inputted slope is not 0 and not 90:

        } else {

            // Determine user slope from degrees input.
            double slopeUser = Math.tan(Math.toRadians(polydegreeUser));

            // Calcualte n, total number of waypoint lines.

            // i. Determine slope perpendicular to slopeUser
            double slopeperp = Math.tan(Math.toRadians(polydegreeUser+90));

            // ii. Determine intersection points made from boundary points
            // with slopeUser and a line with slope perpendicular to slope User.
            ArrayList<Double> xperp = new ArrayList<>();
            ArrayList<Double> yperp = new ArrayList<>();
            for (int i = 0; i < pointsX.size(); i++){
                xperp.add((pointsY.get(i) - (slopeUser * pointsX.get(i))) / (slopeperp - slopeUser));
                yperp.add(slopeperp*((pointsY.get(i)- slopeUser*pointsX.get(i))/(slopeperp- slopeUser)));
            }
            // iii. Declare max and min variables to be used in calculation as starting point
            double xMax = max(xperp);
            double xMin = min(xperp);
            double yMax = max(yperp);
            double yMin = min(yperp);

            // iv. Calcualte n, number of waypoint lines.
            double distperp = Math.pow(Math.pow(xMax-xMin,2)+Math.pow(yMax-yMin,2), .5);
            double n = distperp/(width*(1-sidelap));
            int gern = (int) Math.ceil(n);

            // Determine distance of y intercepts between waypoint lines.
            double shift = 0;
            if (slopeperp>0){
                shift = width* (1-sidelap) / Math.sin(Math.toRadians(polydegreeUser-90));
            }
            if (slopeperp<0){
                shift = width* (1-sidelap) / Math.sin(Math.toRadians(90-polydegreeUser));
            }
            // for loop to determine intersection points between boundary lines and waypoint lines.
            for (int j = 0; j < gern; j++){ // Waypoint lines
                for (int i = 0; i < pointsX.size(); i++) {   // Boundary lines
                    double indexj = j;
                    double intersxn = (-polyint.get(i) + yMin - slopeUser * xMax + j * shift) / (polyslope.get(i) - slopeUser);
                    double intersyn = polyslope.get(i) * ((polyslope.get(i) * pointsX.get(i) - pointsY.get(i) - slopeUser * xMax + yMin + j * shift)
                            / (polyslope.get(i) - slopeUser) - pointsX.get(i)) + pointsY.get(i);
                    double intersxp = (polyslope.get(i) * pointsX.get(i) - pointsY.get(i) - slopeUser * xMin + yMin + j * shift)
                            / (polyslope.get(i) - slopeUser);
                    double intersyp = polyslope.get(i) * ((polyslope.get(i) * pointsX.get(i) - pointsY.get(i) - slopeUser * xMin + yMin + j * shift)
                            / (polyslope.get(i) - slopeUser) - pointsX.get(i)) + pointsY.get(i);
                    // Bound points by predefined x/ymax and x/ymin.
                    if (slopeperp < 0){
                        if (intersxn >= xmin.get(i)-.01){
                            if (intersxn <= xmax.get(i)+.01){
                                if (intersyn >= ymin.get(i)-.01) {
                                    if (intersyn <= ymax.get(i) + .01) {
                                        x.add(intersxn);
                                        x.add(indexj);
                                        y.add(intersyn);
                                        y.add(indexj);
                                    }
                                }
                            }
                        }
                    }


                    // Bound points by predefined xmax and xmin.
                    if (slopeperp > 0){
                        if (intersxp >= xmin.get(i)-.01){
                            if (intersxp <= xmax.get(i)+.01){
                                if (intersyp >= ymin.get(i)-.01){
                                    if (intersyp <= ymax.get(i)+.01){
                                        x.add(intersxp);
                                        x.add(indexj);
                                        y.add(intersyp);
                                        y.add(indexj);
                                    }
                                }
                            }
                        }
                    }

                } // End boundary line for loop
            } // End waypoint line for loop

            // Order Waypoints

            // Determine indicies of last intersection point for each way point line.

            // Declare array to hold indicies.
            int [] arrofI = new int[gern-1];
            // Set initial mostRecentISeen to the first point.
            int mostRecentISeen = (int)Math.round(x.get(1));
            // Determine indicies, where each index represents the last intersection on each waypoint line.
            for (int i = 3; i < x.size(); i += 2){
                if (x.get(i) != mostRecentISeen){
                    arrofI[mostRecentISeen] = i-2;
                    mostRecentISeen = (int)Math.round(x.get(i));
                }
            }

            // Determine max and min for each waypoint line.
            // Store intersection points for first waypoint line.
            ArrayList<Double> bigorsmallx = new ArrayList<>();
            ArrayList<Double> bigorsmally = new ArrayList<>();
            for (int i=1; i < x.size(); i+=2){
                if (i<=arrofI[0]){
                    bigorsmallx.add(x.get(i-1));
                    bigorsmally.add(y.get(i-1));
                }
            }

            // Store max then min of intersection points from the first waypoint line in new array, OrderedPoints.
            orderedPointsX.add(max(bigorsmallx));
            orderedPointsX.add(min(bigorsmallx));
            int ixmax = bigorsmallx.indexOf(max(bigorsmallx));
            int ixmin = bigorsmallx.indexOf(min(bigorsmallx));
            orderedPointsY.add(bigorsmally.get(ixmax));
            orderedPointsY.add(bigorsmally.get(ixmin));

            // Store intersection points of 2nd:2nd to last waypoint lines.
            ArrayList<Double> bigorsmallx2 = new ArrayList<>();
            ArrayList<Double> bigorsmally2 = new ArrayList<>();

            for (int j = 0; j < arrofI.length-1; j++){
                for (int i=1; i < x.size(); i+=2){
                    if (i>arrofI[j] & i<=arrofI[j+1]){
                        bigorsmallx2.add(x.get(i-1));
                        bigorsmally2.add(y.get(i-1));
                    }
                }

                // Store max and min of 2nd:2nd to last waypoint lines in array, OrderedPoints.
                orderedPointsX.add(max(bigorsmallx2));
                orderedPointsX.add(min(bigorsmallx2));
                int ixmax2 = bigorsmallx2.indexOf(max(bigorsmallx2));
                int ixmin2 = bigorsmallx2.indexOf(min(bigorsmallx2));
                orderedPointsY.add(bigorsmally2.get(ixmax2));
                orderedPointsY.add(bigorsmally2.get(ixmin2));

                bigorsmallx2.clear(); // Clear the array each time so that only values from each waypoint line are evaluated for max and min.
                bigorsmally2.clear();

            } // End for loop for storing intersection points.

            // Store intersection points for last waypoint line.
            ArrayList<Double> bigorsmallx3 = new ArrayList<>();
            ArrayList<Double> bigorsmally3 = new ArrayList<>();
            for (int i=1; i < x.size(); i+=2){
                if (i>arrofI[arrofI.length-1]){
                    bigorsmallx3.add(x.get(i-1));
                    bigorsmally3.add(y.get(i-1));
                }
            }

            // Store max and min of the last waypoint lines in array, OrderedPoints.
            orderedPointsX.add(max(bigorsmallx3));
            orderedPointsX.add(min(bigorsmallx3));
            int ixmax3 = bigorsmallx3.indexOf(max(bigorsmallx3));
            int ixmin3 = bigorsmallx3.indexOf(min(bigorsmallx3));
            orderedPointsY.add(bigorsmally3.get(ixmax3));
            orderedPointsY.add(bigorsmally3.get(ixmin3));

            // Swap every other x point to ensure drone flies max--> min then min--> max.
            for (int i=2; i < orderedPointsX.size()-1;i+=4){
                swap(orderedPointsX, i, i+1);
                swap(orderedPointsY, i, i+1);
            }

        } // End else statement

        // Convert orderedPointsX and orderedPointsY to a 2d array
        ArrayList<Double> OrderedPoints1 = new ArrayList<>();
        for (int i=0;i<orderedPointsX.size();i++){
            OrderedPoints1.add(orderedPointsX.get(i));
            OrderedPoints1.add(orderedPointsY.get(i));
        }
        //Remove any points that are less than 15 meters apart.
        for (int i=0; i<OrderedPoints1.size()-3; i+=2){
            double dist = Math.pow(Math.pow(OrderedPoints1.get(i)-OrderedPoints1.get(i+2),2)+Math.pow(OrderedPoints1.get(i+1)-OrderedPoints1.get(i+3),2), .5);
            if (dist<5){
                OrderedPoints1.remove(i);
                //This is really deleting i and then i+1, but since the index updates, we just remove i twice.
                OrderedPoints1.remove(i);
                i=0;
            }
        }





        double [] arr = arraylistToArray(OrderedPoints1);

        xmin.clear();
        ymin.clear();
        xmax.clear();
        ymax.clear();
        polyint.clear();
        polyslope.clear();
        pointsX.clear();
        pointsY.clear();
        x.clear();
        y.clear();
        orderedPointsX.clear();
        orderedPointsY.clear();

        return arr;

    } // End method

    /**
     * This one looks like it does the same thing as everything else i won't touch it //TODO derp!
     * @param polylist an arraylist of polygon latitude and longitudes
     * @param numdrones the number of drones
     * @param altitude the altitude input by the user
     * @return an a ArrayList of latitudes and longitudes
     */
    public static ArrayList <ArrayList<LatLng>> block(ArrayList<LatLng> polylist,
                                                      int numdrones, double altitude) {

        //Convert polylist input (arraylist LatLng) to UTM (arraylist LatLng)
        ArrayList<Double> polylistUTM = WSGToAndFromUTMConversion.convertWSGlisttoUTM(polylist);

        WSGToAndFromUTMConversion pos2 = new WSGToAndFromUTMConversion(polylist.get(0).latitude, polylist.get(0).longitude);
        //TODO change to be like single drone case, for on edge of utm boundary 90 degrees
        int zoner = pos2.zone;
        char lett = pos2.letter;

        //Convert polylistUTM ArrayList<Double> to double
        double points [] = arraylistToArray(polylistUTM);

        // for loop to split x and y points
        ArrayList<Double> pointsX = splitPoints(points, 'x');
        ArrayList<Double> pointsY = splitPoints(points, 'y');

        // 1. Determine slope and intercept of boundary lines.
        ArrayList<Double> polyslope = slopeFun(pointsX, pointsY);
        ArrayList<Double> polyInt = intfun(pointsX, pointsY);

        // 2. Determine each boundary line's y-coordinate and x-coordinate min & max for the purpose of bounding points.
        ArrayList<Double> ymin = polyMinFun(pointsY);
        ArrayList<Double> ymax = polyMaxFun(pointsY);
        ArrayList<Double> xmin = polyMinFun(pointsX);
        ArrayList<Double> xmax = polyMaxFun(pointsX);

        double Xmin = min(pointsX);
        double Xmax = max(pointsX);
        double dist = Xmax - Xmin;
        double shift = dist/numdrones;

        ArrayList<Double> holdpointsX1 = new ArrayList<>();
        ArrayList<Double> holdpointsX2 = new ArrayList<>();

        ArrayList<Double> holdpointsY1 = new ArrayList<>();
        ArrayList<Double> holdpointsY2 = new ArrayList<>();

        ArrayList<Integer> indexForJ1 = new ArrayList<>();
        ArrayList<Integer> indexforj2 = new ArrayList<>();

        double width = altitude* 1.7111;
        double margin;
        if (.2*width>10) {
            margin = 0.2 * width;
        }
        else {
            margin = 10;
        }
        //make new points that are removed into an array
        ArrayList<Double> pointslist = new ArrayList<>();
        for (int z=0; z < pointsX.size();z++){
            pointslist.add(pointsX.get(z));
            pointslist.add(pointsY.get(z));
        }

        double pointsd[] = arraylistToArray(pointslist);

        pointsX.clear();
        pointsY.clear();

        pointsX = splitPoints(pointsd, 'x');
        pointsY = splitPoints(pointsd, 'y');

        ymin = polyMinFun(pointsY);
        ymax = polyMaxFun(pointsY);
        xmin = polyMinFun(pointsX);
        xmax = polyMaxFun(pointsX);

        //Determine slope and intercept of boundary lines, after removing points that are causing v-shapes
        polyslope = slopeFun(pointsX, pointsY);
        polyInt = intfun(pointsX, pointsY);

        for (int j = 0; j < pointsX.size(); j++) {
            for (int i = 1; i < numdrones; i++) {
                //Find intersections between boundary lines and automatically generated block lines (vertical lines)
                double newPolyLineX1 = Xmin + i * shift - margin;
                double newPolyLineX2 = Xmin + i * shift + margin;
                double newPolyLineY1 = polyslope.get(j) * (Xmin + i * shift - margin) + polyInt.get(j);
                double newPolyLineY2 = polyslope.get(j) * (Xmin + i * shift + margin) + polyInt.get(j);

                // Bound intersections points by predefined x/ymax and x/ymin to input these into points
                if (boundIntersection(ymin, ymax, xmin, xmax, j, newPolyLineX1, newPolyLineY1)) {
                    indexForJ1.add(j);
                    holdpointsX1.add(newPolyLineX1);
                    holdpointsY1.add(newPolyLineY1);
                }

                if (boundIntersection(ymin, ymax, xmin, xmax, j, newPolyLineX2, newPolyLineY2)) {
                    indexForJ1.add(j);
                    holdpointsX1.add(newPolyLineX2);
                    holdpointsY1.add(newPolyLineY2);
                }
            } // End for loop i

            if (pointsX.get(j) < xmax.get(j)+1 && pointsX.get(j) > xmax.get(j)-1){
                reverse(holdpointsX1);
                reverse(holdpointsY1);
                reverse(indexForJ1);
            }

            for (int i=0; i < holdpointsX1.size(); i++){
                holdpointsX2.add(holdpointsX1.get(i));
                holdpointsY2.add(holdpointsY1.get(i));
                indexforj2.add(indexForJ1.get(i));
            }

            holdpointsX1.clear();
            holdpointsY1.clear();
            indexForJ1.clear();
        } // End for loop j

        for (int i = 0; i < holdpointsX2.size(); i++) {
            pointsX.add(indexforj2.get(i) + i + 1, holdpointsX2.get(i));
            pointsY.add(indexforj2.get(i) + i + 1, holdpointsY2.get(i));
        }

        ArrayList<Double> newpoly = new ArrayList<>();
        ArrayList <ArrayList<LatLng>> blockout = new ArrayList<>();


        for (int i = 0; i < numdrones; i++) {
            ArrayList<LatLng> wpAlgorithmList2 = new ArrayList<>();
            for (int j = 0; j < pointsX.size(); j++) {
                if (pointsX.get(j) >= Xmin + i * shift - 2 && pointsX.get(j) <= Xmin + (i + 1) * shift + 2) {
                    newpoly.add(pointsX.get(j));
                    newpoly.add(pointsY.get(j));
                } //End if statement

            } // End for loop j

            double newpolyd [] = arraylistToArray(newpoly);

            //Convert the WPs from UTM back to WSG
            for (int j = 0; j < newpolyd.length - 1; j += 2) {
                ArrayList<String> missionary = new ArrayList<>();
                missionary.add(Integer.toString(zoner));
                missionary.add(" ");
                missionary.add(Character.toString(lett));
                missionary.add(" ");
                missionary.add(Double.toString(newpolyd[j]));
                missionary.add(" ");
                missionary.add(Double.toString(newpolyd[j + 1]));

                String input = missionary.get(0) + missionary.get(1) + missionary.get(2) + missionary.get(3) + missionary.get(4) + missionary.get(5) + missionary.get(6);

                LatLng wpLatLng = WSGToAndFromUTMConversion.UTMtoWSGConversion(input);
                //Store each wp as a list
                wpAlgorithmList2.add(wpLatLng);
                missionary.clear();

            }

            blockout.add(wpAlgorithmList2);
            newpoly.clear(); // Clear the array each time so that only values from each waypoint line are evaluated for max and min.

        } //End for loop i

        return blockout;
    } // End block method

    /**
     * Complex repeating if statement. Binds intersections points by predefined x/ymax and x/ymin to input these into points
     * @param ymin The smallest Y
     * @param ymax The biggest Y
     * @param xmin The smallest X
     * @param xmax The biggest X
     * @param j counter
     * @param newPolyLineX1 a double polygon X line
     * @param newPolyLineY1 a double polygon Y line
     * @return a boolean true or false whether there's a bounding points intersection
     */
    private static boolean boundIntersection(final ArrayList<Double> ymin,
                                             final ArrayList<Double> ymax, final ArrayList<Double> xmin, final ArrayList<Double> xmax, int j, double newPolyLineX1, double newPolyLineY1) {
        return newPolyLineX1 <= xmax.get(j) - .01 && newPolyLineX1 >= xmin.get(j) + .01 &&
                newPolyLineY1 <= ymax.get(j) - .01 && newPolyLineY1 >= ymin.get(j) + .01;
    }
}
