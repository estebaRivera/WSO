package com.longtailvideo.jwplayer.utils;

import java.util.List;

import android.net.Uri;

/**
 * Utility class for dealing with URLs.
 * 
 * @author tom
 */
public class UrlUtils {
	/**
	 * Remove the last segment of the path and the eventual querystring from a
	 * URL.
	 * 
	 * @param fullUrl
	 *            The full URL.
	 * @return The base URL.
	 */
	public static String getBaseUrl(String fullUrl) {
		// Get the base URL: Remove the last path segment
		Uri baseUrl = Uri.parse(fullUrl);
		List<String> segs = baseUrl.getPathSegments();
		Uri.Builder b = baseUrl.buildUpon().path("/").query("");
		for (String s : segs.subList(0, segs.size() - 1)) {
			b.appendPath(s);
		}
		return b.build().toString();
	}

	/**
	 * Apply a relative URL onto a base URL.
	 * 
	 * Works only for http and https URLs.
	 * 
	 * @param baseUrl
	 *            The URL to use as a base.
	 * @param url
	 *            The URL to make absolute if necessary.
	 * @return The absolute URL.
	 */
	public static String getAbsoluteUrl(String baseUrl, String url) {
		String absUrl = url;
		if (!url.startsWith("http:") && !url.startsWith("https:")) {
			if (url.startsWith("/")) {
				// Absolute URL without host
				Uri.Builder b = Uri.parse(baseUrl).buildUpon();
				b.encodedPath(url);
				absUrl = b.build().toString();
			}
			else {
				// Relative URL
				absUrl = Uri.withAppendedPath(Uri.parse(baseUrl), url)
						.toString();
			}
		}
		return absUrl;
	}
}
