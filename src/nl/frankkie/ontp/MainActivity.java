package nl.frankkie.ontp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Sync Android Clock with NTP-server
 *
 * http://stackoverflow.com/questions/8049912/how-can-i-get-the-network-time-from-the-automatic-setting-called-use-netw
 * http://stackoverflow.com/questions/5300999/set-the-date-from-a-shell-on-android
 * http://www.pool.ntp.org/zone/@
 *
 * Parts: 1) find closest server; 2) get time from server; 3) set time on
 * Android
 *
 * @author FrankkieNL
 */
public class MainActivity extends Activity {

    public static boolean autoSelectSuggestedServer = true;
    public NtpServer globalNtpServer;
    public NtpServer selectedServer;
    public List<NtpServer> ntpServers;
    public MainActivity thisAct;

    public void jsonRecursion(JSONObject jSONObject) throws JSONException {
        NtpServer temp = new NtpServer(jSONObject.getString("servername"), jSONObject.getString("displayname"));
        ntpServers.add(temp);
        if (jSONObject.has("children")) {
            for (int i = 0; i < jSONObject.getJSONArray("children").length(); i++) {
                jsonRecursion(jSONObject.getJSONArray("children").getJSONObject(i));
            }
        }
    }

    public void initNtpServers() {
        globalNtpServer = new NtpServer("global", "Global");
        selectedServer = globalNtpServer;
        try {
            InputStream open = getResources().getAssets().open("ntpservers.json");
            //http://stackoverflow.com/questions/2492076/android-reading-from-an-input-stream-efficiently
            BufferedReader r = new BufferedReader(new InputStreamReader(open));
            StringBuilder total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line);
            }
            JSONObject global = new JSONObject(total.toString());
            //*puts on hard-hat* time for some recursion
            //Did you mean recursion? ~ Google
            ntpServers = new ArrayList<NtpServer>();
            jsonRecursion(global);
        } catch (IOException ex) {
            //if this happens, kinda big problem.
            Toast.makeText(thisAct, "Cannot access Resources, this is a big problem. Please re-install this app or contact the Developer.", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        } catch (JSONException ex) {
            Toast.makeText(thisAct, "Cannot parse JSON Resources, this is a big problem. Please re-install this app or contact the Developer.", Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        thisAct = this;
        initUI();
        initNtpServers();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //start LocationFinderThingy
        getLocation();
    }

    public void updateLocationSuggestion(Location location) {
        String country = geocodeLocationToCountryname(location);
        TextView tv = (TextView) findViewById(R.id.tv_location_suggested);
        if (country != null) {
            for (NtpServer server : ntpServers) {
                if (server.displayName.equalsIgnoreCase(country) || country.contains(server.displayName) /**
                         * The Netherlands vs Netherlands *
                         */
                        ) {
                    tv.setText("Suggested NTP Server: " + server.displayName);
                    //Set the selected server to the suggested server
                    if (autoSelectSuggestedServer) {
                        selectedServer = server;
                        announceSelectedServer();
                    }
                }
            }
        } else {
            tv.setText("Suggested NTP Server: global");
        }
    }

    public String geocodeLocationToCountryname(Location location) {
        if (!Geocoder.isPresent()) {
            Log.e("oNTP", "Geocoder is not present");
            return null;
        }
        Log.e("oNTP", "Geocoder is present :-)");
        final Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.ENGLISH);
        String country = null;
        try {
            List<Address> places = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 10);
            for (Address a : places) {
                if (a.getCountryName() != null) {
                    country = a.getCountryName();
                    break;
                }
            }
        } catch (IOException ex) {
            Log.e("oNTP", "Geocoder Error", ex);
        }
        return country;
    }

    public void getLocation() {
        if (!Geocoder.isPresent()) {
            Log.e("oNTP", "Geocoder is not present");
            return;
        }
        Log.e("oNTP", "Geocoder is present :-)");
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        for (String provider : lm.getAllProviders()) {
            Location lastKnownLocation = lm.getLastKnownLocation(provider);
            if (lastKnownLocation != null) { //fixes nullpointerexception
                if (lastKnownLocation.getAccuracy() > 0 && lastKnownLocation.getAccuracy() < 500) {
                    if (lastKnownLocation.getTime() > (System.currentTimeMillis() - 24 * 60 * 60 * 1000)) {
                        //location good enough
                        updateLocationSuggestion(lastKnownLocation);
                        break;
                    }
                }
            }
        }
        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_LOW);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setCostAllowed(false);
        c.setHorizontalAccuracy(Criteria.ACCURACY_LOW);
        c.setPowerRequirement(Criteria.NO_REQUIREMENT);
        c.setSpeedRequired(false);
        lm.requestSingleUpdate(c, new LocationListener() {

            public void onLocationChanged(Location location) {
                updateLocationSuggestion(location);
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            public void onProviderEnabled(String provider) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }

            public void onProviderDisabled(String provider) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        }, null);
    }

    /**
     * User has selected a server from the list. Set that server as selected
     * server, announce that the change has been made.
     *
     * @param index the index that user selected in the list
     */
    public void selectedServer(int index) {
        selectedServer = ntpServers.get(index);
        announceSelectedServer();
    }

    /**
     * Announce which server is selected
     */
    public void announceSelectedServer() {
        TextView tvSelected = (TextView) findViewById(R.id.tv_location_selected);
        tvSelected.setText("Selected Server:  " + selectedServer.displayName + " (" + selectedServer.serverName + ")");
        Toast.makeText(thisAct, "Selected " + selectedServer.displayName + " (" + selectedServer.serverName + ")", Toast.LENGTH_SHORT).show();
    }

    public void selectLocationManual() {
        //http://www.pool.ntp.org/zone/@
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        String[] items = null;
        ArrayList<String> arr = new ArrayList<String>();
        for (NtpServer server : ntpServers) {
            arr.add(server.displayName);
        }
        items = arr.toArray(new String[0]);
        b.setItems(items, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
                //select
                selectedServer(arg1);
            }
        });
        b.create().show();
    }

    public void showSelectLocationDialog() {
        selectLocationManual();
    }

    public void initUI() {
        setContentView(R.layout.main);
        Button btnGetTime = (Button) findViewById(R.id.btn_get_time);
        btnGetTime.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                GetNTPTimeTask t = new GetNTPTimeTask();
                t.execute("");
            }
        });

        Button btnLocation = (Button) findViewById(R.id.btn_location);
        btnLocation.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                showSelectLocationDialog();
            }
        });

        Button btnSync = (Button) findViewById(R.id.btn_sync);
        btnSync.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                SyncNTPTimeTask t = new SyncNTPTimeTask();
                t.execute("");
            }
        });
    }

    public class GetNTPTimeTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... params) {
            getAndShowTime();
            return null;
        }

    }

    public class SyncNTPTimeTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... params) {
            syncTime();
            return null;
        }

    }

    public Handler handler = new Handler();

    public void syncTime() {
        try {
            long serverTime = getCurrentNetworkTime();
            //adb shell date -s "yyyymmdd.[[[hh]mm]ss]"
            Time timeServer = new Time();
            timeServer.set(serverTime);
            final String timeServerString = timeServer.format("%Y%m%d.%H%M%S");
            Time timeLocal = new Time();
            timeLocal.setToNow();
            final String timeLocalString = timeLocal.format("%Y %m %d %H:%M:%S");
            //List<String> shellOutput = Shell.run("sh", new String[]{"date -s " + timeServerString}, null, true);            
            if (!Shell.SU.available()) {
                handler.post(new Runnable() {

                    public void run() {
                        showAlertDialog("Root access not available, unable to sync the time.");
                    }
                });
            } else {
                List<String> shellOutput = Shell.run("su", new String[]{"date -s " + timeServerString}, null, true);
                if (shellOutput != null) {
                    for (String line : shellOutput) {
                        Log.v("oNTP", "[shell] " + line);
                    }
                }
                handler.post(new Runnable() {

                    public void run() {
                        Toast.makeText(thisAct, "Time Synced.\nSelected Server: " + selectedServer.displayName + " (" + selectedServer.serverName + ")", Toast.LENGTH_LONG).show();
                    }
                });
            }
        } catch (final Exception e) {
            handler.post(new Runnable() {

                public void run() {
                    showAlertDialog("Check you Internet-connection!\n" + e);
                    //ShowException.showException(e, MainActivity.this);
                }
            });
        }
    }

    public void getAndShowTime() {
        try {
            long serverTime = getCurrentNetworkTime();
            Time timeServer = new Time();
            timeServer.set(serverTime);
            String timeServerString = timeServer.format("%Y %m %d %H:%M:%S"); //See: man strftime
            Time timeLocal = new Time();
            timeLocal.setToNow();
            String timeLocalString = timeLocal.format("%Y %m %d %H:%M:%S");
            showAlertDialog("Server: " + timeServerString + "\nLocal: " + timeLocalString + "\nSelected Server: " + selectedServer.displayName + " (" + selectedServer.serverName + ")");
        } catch (final Exception e) {
            handler.post(new Runnable() {

                public void run() {
                    showAlertDialog("Check you Internet-connection!\n" + e);
                    //ShowException.showException(e, MainActivity.this);
                }
            });
        }
    }

    public void showAlertDialog(final String s) {
        handler.post(new Runnable() {

            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Time");

                builder.setMessage(s);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        //
                    }
                });
                builder.create().show();
            }
        });
    }

    public static final String TIME_SERVER_GOV = "time-a.nist.gov";

    public long getCurrentNetworkTime() throws UnknownHostException, IOException {
        return getCurrentNetworkTime(selectedServer.serverName + ".pool.ntp.org");
    }

    public static long getCurrentNetworkTime(String timeServer) throws UnknownHostException, IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        InetAddress inetAddress = InetAddress.getByName(timeServer);
        TimeInfo timeInfo = timeClient.getTime(inetAddress);
        //long returnTime = timeInfo.getReturnTime();   //local device time
        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time

        Date time = new Date(returnTime);
        Log.d("oNTP", "Time from " + timeServer + ": " + time);

        return returnTime;
    }
}
