package de.zwiebelchen.wirelessbarcodescanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import de.zwiebelchen.wirelessbarcodescanner.ServerService.LocalBinder;

public class MenuActivity extends Activity {
	
	private static final int RESULT_SETTINGS = 1;
	
	protected void noWifiEnabled() {

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.noWifiTitle);
		builder.setMessage(R.string.noWifiMessage);
		builder.setCancelable(true);
		builder.setNeutralButton(android.R.string.ok,
		        new DialogInterface.OnClickListener() {
		    @Override
			public void onClick(DialogInterface dialog, int id) {
		        MenuActivity.this.finish();
		    }
		});
	    AlertDialog alert = builder.create();
	    alert.show();
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
	        super.onCreate(savedInstanceState);
	        setContentView(R.layout.menu);
	        
	        Button cmdStartScanning = (Button) findViewById(R.id.cmdStartScanning);
	        cmdStartScanning.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					if (isConnected(MenuActivity.this)) {
						doScan();
					}
				}
	        	
	        });
	        
	        if (!isConnected(this)) {
	        	noWifiEnabled();
	        	if (mService != null) {
	        		unbindService(mConnection);
	    			mService = null;
	        	}
	        }
	        else {
	        	final WifiManager wifiMgr = (WifiManager) getSystemService(WIFI_SERVICE);
	        	final WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
	        	int ip = wifiInfo.getIpAddress();
	        	String ipAddress = Formatter.formatIpAddress(ip);
	        	
	        	TextView port = (TextView) findViewById(R.id.txtPort);
	        	TextView ipAddr = (TextView) findViewById(R.id.txtIpAddr);
	        	
	        	SharedPreferences sharedPrefs = PreferenceManager
	        			.getDefaultSharedPreferences(this);
	        	
	        	if (sharedPrefs.getString("port", null) == null || sharedPrefs.getString("port", null).equals("")) {
	        		Editor e = sharedPrefs.edit();
	        		e.putString("port", "1234");
	        		e.commit();
	        	}
	        	
	        	port.setText(sharedPrefs.getString("port", null));
	        	ipAddr.setText(ipAddress);
	        	
	        }
	        
	}
	
	private boolean isConnected(Context context) {
	    ConnectivityManager connectivityManager = (ConnectivityManager)
	        context.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo networkInfo = null;
	    if (connectivityManager != null) {
	        networkInfo =
	            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
	    }
	    return networkInfo == null ? false : networkInfo.isConnected();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.layout.settings, menu);
        return true;
    }
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == RESULT_SETTINGS) {
        	settingsChanged();
        	return;
        }
        else if (requestCode == IntentIntegrator.REQUEST_CODE){
        	IntentResult scanningResult = IntentIntegrator.parseActivityResult(
    				requestCode, resultCode, data);

        	SharedPreferences sharedPrefs = PreferenceManager
	                .getDefaultSharedPreferences(this);
        	
        	boolean batchScanning = sharedPrefs.getBoolean("batchScan", false);
        	
    		if (resultCode == RESULT_OK) {
    			if (scanningResult != null) {
    				if (mService != null) {
    					mService.sendString(scanningResult.getContents());
    				}
    			}
    			if (batchScanning) {
    				doScan();
    			}
    		}
        	
        }
        
    }
	
	private void settingsChanged() {
		TextView txtPort = (TextView) findViewById(R.id.txtPort);
		SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);
		txtPort.setText(sharedPrefs.getString("port", null));
		if (mService != null) {
			int oldPort = mService.getPort();
			int newPort = Integer.parseInt(sharedPrefs.getString("port", null));
			if (oldPort != newPort) {
				mService.setPort(newPort);
			}
			
		}
	}
 
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
 
        case R.id.menu_settings:
            Intent i = new Intent(this, SettingsActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            break;
            
        case R.id.startScanning:
        	doScan();
        	break; 
        }
 
        return true;
    }
    
    private void doScan() {
    	if (mService != null) {
    		IntentIntegrator scanIntegrator;
    		scanIntegrator = new IntentIntegrator(this);

    		scanIntegrator.initiateScan();
    	}
    }
    
	@Override
	protected void onStart() {
		super.onStart();
		Intent intent = new Intent(this, ServerService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mService != null) {
			unbindService(mConnection);
			mService = null;
		}
	}

	private ServerService mService = null;

	private final ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			LocalBinder binder = (LocalBinder) service;
			mService = binder.getService();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};

}
