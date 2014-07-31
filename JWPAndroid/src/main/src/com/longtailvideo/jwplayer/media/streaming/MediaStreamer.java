package com.longtailvideo.jwplayer.media.streaming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.channels.ClosedByInterruptException;
import java.util.Scanner;

import android.util.Log;

import com.longtailvideo.jwplayer.events.EventDispatcher;
import com.longtailvideo.jwplayer.events.MediaEvent;

/**
 * A small http server which accepts connections on a random port and allows its
 * user to send a stream of data to the connected client. Currently only one
 * client is accepted at a time.
 * 
 * Events:
 * 
 * mediaError: If the server fails
 * 
 * @author tom
 */
public class MediaStreamer extends EventDispatcher implements Runnable {
	// The default port to listen on.
	// If this port is not available, a random one is chosen.
	private static final int DEFAULT_PORT = 0x7487;

	// Tag for logging
	private static final String TAG = "MediaStreamer";

	// Whether to keep running the http server.
	private boolean mKeepRunning;

	// The current port on which the server is listening.
	// If -1, the server is not running yet.
	private int mPort = -1;

	// The handler for client connections.
	private OnConnectHandler mOnConnectHandler;

	// The thread on which the server is running.
	private Thread mThread;

	// The content type to output
	private final String mContentType;

	// The filename in the URL
	private final String mFileName;

	/**
	 * @param contentType
	 *            The value of the content-type http header.
	 * @param fileName
	 *            The filename (including extension) to use.
	 */
	public MediaStreamer(String contentType, String fileName) {
		mContentType = contentType;
		mFileName = fileName;
	}

	/**
	 * @return The URL to use to connect to the server from localhost.
	 * @throws IllegalStateException
	 *             if the server is not started yet.
	 */
	public String getUrl() throws IllegalStateException {
		if (mPort == -1) {
			throw new IllegalStateException("Streamer was not started yet.");
		}
		return "http://localhost:" + mPort + "/" + mFileName;
	}

	/**
	 * @param handler
	 *            the handler for client connections. Executed on the server
	 *            thread.
	 */
	public void setOnConnectHandler(OnConnectHandler handler) {
		mOnConnectHandler = handler;
	}

	/**
	 * An interface to be implemented by client connection handlers.
	 * 
	 * @author tom
	 */
	public interface OnConnectHandler {
		/**
		 * Handle a client connection.
		 * 
		 * @param stream
		 *            the output stream to the client.
		 */
		void onConnect(OutputStream stream);
	}

	/**
	 * Start the server, if it is not running yet.
	 * 
	 * After a successful call, calls to getUrl() are valid.
	 */
	public void start() {
		if (mThread == null) {
			mThread = new Thread(this);
			mKeepRunning = true;
			synchronized (this) {
				mThread.start();
				try {
					// Wait for the server to be started
					wait();
				}
				catch (InterruptedException e) {
					// Probably already stopping
					if (!mKeepRunning) {
						Log.e(TAG,
								"MediaStreamer was unexpectedly interrupted during startup.");
					}
				}
			}
		}
	}

	/**
	 * Stop the media streamer.
	 */
	public synchronized void stop() {
		mKeepRunning = false;
		if (mThread != null) {
			mThread.interrupt();
			try {
				wait(2000);
			}
			catch (InterruptedException e) {
				// Ignore interruption here
			}
			mThread = null;
		}
	}

	@Override
	public void run() {
		ServerSocket server;
		int port = DEFAULT_PORT;
		try {
			while (true) {
				try {
					server = new ServerSocket(port);
					server.setReuseAddress(true);
				}
				catch (BindException e) {
					// If the port is in use, try another one
					e.printStackTrace();
					// Select a random port in the range 1024 to 2^16 - 1
					port = (int) ((Math.random() * ((1 << 16) - 1024)) + 1024);
					continue;
				}
				catch (ClosedByInterruptException e) {
					// No error here, we were just interrupted.
					if (!mKeepRunning) {
						mThread = null;
						return;
					}
					Log.v(TAG,
							"Interrupted while connecting, but not stopping.");
					continue;
				}
				catch (IOException e) {
					Log.e(TAG,
							"Could not start MediaStreamer: " + e.getMessage());
					dispatchEvent(new MediaEvent(
							MediaEvent.JWPLAYER_MEDIA_ERROR,
							MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED));
					mThread = null;
					return;
				}
				mPort = port;
				break;
			}
		}
		finally {
			// Allow the original thread to continue at the end of the connect
			// block, whether successful or not.
			synchronized (this) {
				notifyAll();
			}
		}
		Log.v(TAG, "Listening on port " + port);

		handleConnections(server);
		mThread = null;
	}

	/**
	 * Handle connections from the media player.
	 * 
	 * @param server
	 *            The server socket which listens for player connections.
	 */
	private void handleConnections(ServerSocket server) {
		try {
			while (mKeepRunning) {
				try {
					Log.v(TAG, "Waiting for connection...");
					Socket s = server.accept();

					if (!s.getLocalAddress().isLoopbackAddress()) {
						// Only accept loopback connections
						Log.w(TAG,
								"Ignoring connection from "
										+ s.getLocalAddress());
						s.close();
						continue;
					}

					// Log the HTTP request
					Log.d(TAG, "--- Start of HTTP request ---");
					byte[] buffer = new byte[1024];
					int l = s.getInputStream().read(buffer);
					Scanner sc = new Scanner(new ByteArrayInputStream(buffer,
							0, l));
					String lastLine = "";
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						if (line.isEmpty() && lastLine.isEmpty()) {
							break;
						}
						Log.d(TAG, line);
						lastLine = line;
					}
					Log.d(TAG, "--- End of HTTP request ---");

					// Send the output
					Log.v(TAG, "Accepted connection.");
					OutputStream os = s.getOutputStream();
					sendHttpPreamble(os);
					if (mOnConnectHandler != null) {
						mOnConnectHandler.onConnect(os);
					}
					os.close();
					s.close();
				}
				catch (ClosedByInterruptException e) {
					// If we were interrupted because we are stopping,
					// mKeepRunning
					// will be false
					continue;
				}
				catch (IOException e) {
					Log.e(TAG, "An I/O error occurred in the MediaStreamer: "
							+ e.getMessage());
					dispatchEvent(new MediaEvent(
							MediaEvent.JWPLAYER_MEDIA_ERROR,
							MediaEvent.Error.ERROR_FILE_COULD_NOT_BE_PLAYED));
				}
			}
			Log.v(TAG, "MediaStreamer in port mPort stopped.");
		}
		finally {
			synchronized (this) {
				notifyAll();
			}
		}
	}

	/**
	 * Send an HTTP 200 OK response, with a simple content-type header.
	 * 
	 * @param os
	 *            The target stream.
	 * @throws IOException
	 *             If the data could not be sent over the stream.
	 */
	private void sendHttpPreamble(OutputStream os) throws IOException {
		final String preamble = "HTTP/1.0 200 OK\r\nContent-Type: "
				+ mContentType + "\r\n\r\n";
		os.write(preamble.getBytes());
		os.flush();
	}
}
