package de.zwiebelchen.wirelessbarcodescanner;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ServerService extends Service {

	private ServerSocket server;
	private Vector<Socket> clients;
	private Thread t;
	private boolean isRunning = true;
	private int port = 1234;

	private final IBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		ServerService getService() {
			return ServerService.this;
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	private void startServer() {
		t = new Thread() {
			@Override
			public void run() {
				try {
					server = new ServerSocket(port);
					isRunning = true;
					Log.i("ServerIP", server.getInetAddress().toString());
					clients = new Vector<Socket>();
					Socket s;
					while ((s = server.accept()) != null && isRunning) {
						clients.add(s);
					}
				} catch (IOException e) {
					if (!e.getMessage().equals("Socket closed")) {
						Log.e("BarcodeTyper.ServerService", "Error in Thread !", e);
					}
				}
			}
		};
		t.start();
	}
	
	private void stopServer() {
		isRunning = false;
		
		if (t != null) {
			if (t.isAlive()) {
				if (!clients.isEmpty()) {
					for (Socket client : clients) {
						try {
							client.close();
						} catch (Exception e) {
							Log.e("Writing failed", e.getMessage());
						}
					}
					clients = null;
				}
				if (!server.isClosed()) {
					try {
						server.close();
					}
					catch (Exception e) {
						Log.e("Writing failed", e.getMessage());
					}
				}
				t = null;
				server = null;
			}
			
		}
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		startServer();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		stopServer();
	}
	
	public void setPort(int port) {
		
		this.port = port;
		
		if (isRunning) {
			stopServer();
		}
		
		startServer();
	}
	
	public int getPort() {
		return port;
	}
	
	public void sendString(String message) {
		if (!clients.isEmpty()) {
			for (Socket client : clients) {
				try {
					PrintWriter pw = new PrintWriter(
							client.getOutputStream());
					pw.write(message);
					pw.write("\n");
					pw.flush();
				} catch (Exception e) {
					Log.e("Writing failed", e.getMessage());
				}
			}
		}
	}
}
