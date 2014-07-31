package com.longtailvideo.jwplayer.media.adaptive.manifest;

import java.io.InputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

import com.longtailvideo.jwplayer.utils.UrlUtils;

/**
 * A parser for M3U8 manifests. Used for finding both level and fragment
 * information.
 * 
 * @author tom
 */
public class ManifestParser extends Manifest {
	private static final String TAG = "ManifestParser";

	// Bandwidth value to use if no value was given
	private static final int DEFAULT_BANDWIDTH = 150000;
	// Width value to use if no resolution was given
	private static final int DEFAULT_WIDTH = 160;
	// Height value to use if no resolution was given
	private static final int DEFAULT_HEIGHT = 0;

	// The scanner which reads the input stream
	private Scanner mScanner;
	// The current line number
	private int mLine;
	// The discontinuity counter
	private int mDiscontinuity;
	// The base URL to use for URL resolving
	private String mBaseUrl;
	// Whether the following fragments are encrypted
	private boolean mEncryption;
	// The URL to the decryption key for the following fragments, or null
	private String mKeyUrl;
	// The IV for the following fragments, or null
	private byte[] mIv;
	// The sequence number of the next fragment
	private int mSequence;

	/**
	 * @param baseUrl
	 *            The base URL for resolving URLs in this manifest, or null if
	 *            only absolute URLs should be supported.
	 */
	public void setBaseUrl(String baseUrl) {
		mBaseUrl = baseUrl;
	}

	/**
	 * Parse a manifest from an input stream.
	 * 
	 * Note: This operation clears existing data.
	 * 
	 * @param in
	 *            The input stream which delivers the manifest data
	 * @throws ParseException
	 *             if the manifest is no valid M3U8 manifest.
	 */
	public void parse(InputStream in) throws ParseException {
		clear();
		mScanner = new Scanner(in);
		mLine = 1;
		mDiscontinuity = 0;
		mEncryption = false;
		mKeyUrl = null;
		mIv = null;
		mSequence = -1;

		// Get the header
		parseHeader();

		while (hasNextLine()) {
			String data = nextLine().trim();

			if (data.isEmpty()) {
				// Blank line
				continue;
			}

			if (isTag(data)) {
				// Try all known types of keys, in no particular order
				if (parseKey(data)) {
					continue;
				}
				if (parseSequence(data)) {
					continue;
				}
				if (parseFragment(data)) {
					continue;
				}
				if (parseDiscontinuity(data)) {
					continue;
				}
				if (parseLevel(data)) {
					continue;
				}
				if (parseEnd(data)) {
					continue;
				}

				// Unknown M3U8 tag; log it
				int colonPos = data.indexOf(":");
				if (colonPos >= 0) {
					data = data.substring(0, colonPos);
				}
				Log.w(TAG, "Ignored unsupported M3U8 tag " + data);

				continue;
			}

			if (data.startsWith("#")) {
				// A comment
				continue;
			}

			// Nothing usable
			// Error out, because the spec describes no non-tag lines outside
			// the URLs in the implemented tags.
			clear();
			throw new ParseException("Could not parse playlist file.", mLine);
		}
	}

	/**
	 * @return Whether there is another line.
	 */
	private boolean hasNextLine() {
		return mScanner.hasNextLine();
	}

	/**
	 * @return The next line.
	 */
	private String nextLine() {
		++mLine;
		return mScanner.nextLine();
	}

	/**
	 * Parse the M3U8 header.
	 * 
	 * @throws ParseException
	 *             If the next line is not an M3U8 header.
	 */
	private void parseHeader() throws ParseException {
		try {
			String header = nextLine().trim();
			// Header must be on the first line for this to be a valid manifest
			if (!"#EXTM3U".equals(header)) {
				throw new ParseException("No valid M3U header.", mLine);
			}
		}
		catch (NoSuchElementException e) {
			throw new ParseException("Empty manifest.", mLine);
		}
	}

	/**
	 * Check whether a line should be parsed as an M3U8 tag.
	 * 
	 * @param data
	 *            The data to identify.
	 * @return Whether the data should be parsed as an M3U8 tag.
	 */
	private static boolean isTag(String data) {
		if (data == null) {
			return false;
		}
		return data.startsWith("#EXT");
	}

	/**
	 * Parse an M3U8 attribute list to a map.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return The map containing the data from the attribute list. If an
	 *         attribute was specified more than once, only the last occurrence
	 *         is included.
	 */
	private static Map<String, String> parseAttributeList(String data) {
		// Examples:
		// NAME=value,
		// NAME="value",
		// NAME=value<end of line>
		// NAME="value"<end of line>
		final Pattern attributeListItemPattern = Pattern
				.compile("([A-Z-]*)=(\".*?\"|.*?)(?:,|$)");

		Map<String, String> attributes = new HashMap<String, String>();
		Matcher m = attributeListItemPattern.matcher(data);
		while (m.find()) {
			String name = m.group(1);
			String value = m.group(2);
			// Transform "value" into value
			if (value.startsWith("\"") && value.endsWith("\"")
					&& value.length() > 1) {
				value = value.substring(1, value.length() - 1);
			}
			attributes.put(name, value);
		}
		return attributes;
	}

	/**
	 * Try to parse a key tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether a key tag was successfully identified.
	 * @throws ParseException
	 *             if a key tag was identified, but it could not be parsed
	 *             correctly.
	 */
	private boolean parseKey(String data) throws ParseException {
		if (data.startsWith("#EXT-X-KEY:")) {
			String attributeList = data.substring(data.indexOf(":") + 1);
			Map<String, String> attributes = parseAttributeList(attributeList);

			String method = attributes.get("METHOD");
			if (method == null) {
				throw new ParseException("Key tag does not contain a method.",
						mLine);
			}
			if ("AES-128".equals(method)) {
				mEncryption = true;
				String url = attributes.get("URI");
				if (url == null) {
					throw new ParseException(
							"AES-128 was requested, but no key URI was given.",
							mLine);
				}

				url = UrlUtils.getAbsoluteUrl(mBaseUrl, url);

				mKeyUrl = url;
				String iv = attributes.get("IV");
				if (iv == null) {
					mIv = null;
				}
				else {
					if (iv.length() != 34) {
						throw new ParseException("Invalid IV.", mLine);
					}
					if (!iv.startsWith("0x")) {
						throw new ParseException("IV should start with 0x.",
								mLine);
					}

					try {
						byte[] ivBytes = new byte[16];
						for (int i = 0; i < 16; ++i) {
							// Can not use parseByte because bytes are signed in
							// Java -_-
							ivBytes[i] = (byte) Integer.parseInt(
									iv.substring(2 + i * 2, 4 + i * 2), 16);
						}
						mIv = ivBytes;
					}
					catch (NumberFormatException e) {
						throw new ParseException("Invalid hex IV.", mLine);
					}
				}
			}
			else {
				mEncryption = false;
			}
			return true;
		}
		return false;
	}

	/**
	 * Try to parse a sequence tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether a sequence was successfully identified.
	 * @throws ParseException
	 *             If a sequence was identified, but it could not be parsed
	 *             correctly.
	 */
	private boolean parseSequence(String data) throws ParseException {
		if (data.startsWith("#EXT-X-MEDIA-SEQUENCE:")) {
			if (mSequence >= 0) {
				throw new ParseException("Sequence set multiple times.", mLine);
			}
			try {
				mSequence = Integer
						.parseInt(data.substring(data.indexOf(":") + 1));
				if (mSequence < 0) {
					throw new NumberFormatException();
				}
			}
			catch (NumberFormatException e) {
				throw new ParseException("Invalid sequence number.", mLine);
			}
			return true;
		}
		return false;
	}

	/**
	 * Try to parse an inf tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether an inf tag was successfully identified.
	 * @throws ParseException
	 *             If the inf tag was identified, but could not be parsed.
	 */
	private boolean parseFragment(String data) throws ParseException {
		// Fragment tag
		final Pattern fragmentPattern = Pattern
				.compile("#EXTINF:([0-9]*(?:\\.[0-9]*)?)(?:,(.*))?");

		Matcher m = fragmentPattern.matcher(data);
		if (m.matches()) {
			// Start of a fragment
			String duration = m.group(1);
			// Find the URL line
			String url = null;
			while (hasNextLine()) {
				String line = nextLine().trim();

				if (line.isEmpty()) {
					// Blank line
					continue;
				}

				if (parseKey(line)) {
					continue;
				}

				if (line.startsWith("#")) {
					// Comment or unsupported tag
					continue;
				}

				url = line;
				break;
			}

			if (url == null) {
				throw new ParseException("Missing url for fragment.", mLine);
			}

			url = UrlUtils.getAbsoluteUrl(mBaseUrl, url);

			// As a last resort, try to find the sequence from the file name
			// Not according to spec, but fixes some streams.
			if (mSequence < 0) {
				Pattern lastNumberPattern = Pattern
						.compile("([0-9]+)\\.ts[^0-9/]*$");
				Matcher matcher = lastNumberPattern.matcher(url);
				if (matcher.find()) {
					mSequence = Integer.parseInt(matcher.group(1));
				}
				else {
					mSequence = 0;
				}
			}

			if (!mEncryption) {
				addFragment(new Frag(mSequence, Double.parseDouble(duration),
						url, mDiscontinuity));
			}
			else {
				addFragment(new Frag(mSequence, Double.parseDouble(duration),
						url, mDiscontinuity, mKeyUrl, mIv));
			}
			++mSequence;
			return true;
		}
		return false;
	}

	/**
	 * Try to parse a discontinuity tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether a discontinuity tag was successfully identified.
	 */
	private boolean parseDiscontinuity(String data) {
		if ("#EXT-X-DISCONTINUITY".equals(data)) {
			// Encountered a discontinuity tag
			++mDiscontinuity;
			return true;
		}
		return false;
	}

	/**
	 * Try to parse a stream-inf tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether a stream-inf tag could be parsed from this stream.
	 * @throws ParseException
	 *             If a stream-inf tag was identified, but it could not be
	 *             parsed.
	 */
	private boolean parseLevel(String data) throws ParseException {
		if (data.startsWith("#EXT-X-STREAM-INF:")) {
			// Start of a level
			int bandwidth = DEFAULT_BANDWIDTH;
			int width = DEFAULT_WIDTH;
			int height = DEFAULT_HEIGHT;
			boolean audio = false;

			Map<String, String> attributes = parseAttributeList(data);

			String bandwidthStr = attributes.get("BANDWIDTH");
			if (bandwidthStr != null) {
				try {
					bandwidth = Integer.parseInt(bandwidthStr);
					if (bandwidth < 0) {
						throw new NumberFormatException();
					}
				}
				catch (NumberFormatException e) {
					throw new ParseException(
							"Bandwidth is no valid decimal-integer.", mLine);
				}
			}

			String resolutionStr = attributes.get("RESOLUTION");
			if (resolutionStr != null) {
				final Pattern resolutionValuePattern = Pattern
						.compile("([0-9]+)x([0-9]+)");
				Matcher m = resolutionValuePattern.matcher(resolutionStr);
				if (!m.matches()) {
					throw new ParseException("Invalid value for resolution.",
							mLine);
				}
				width = Integer.parseInt(m.group(1));
				height = Integer.parseInt(m.group(2));
			}

			String codecsStr = attributes.get("CODECS");
			if (codecsStr != null) {
				audio = !codecsStr.contains("avc1");
			}

			String name = attributes.get("NAME");

			// Find the URL line
			String url = null;
			String line;
			while (hasNextLine()) {
				line = nextLine().trim();

				if (line.isEmpty()) {
					// Blank line
					continue;
				}

				url = line;
				break;
			}

			if (url == null) {
				throw new ParseException("Missing url for quality level.",
						mLine);
			}

			url = UrlUtils.getAbsoluteUrl(mBaseUrl, url);

			addLevel(new Level(bandwidth, width, height, audio, name, url));
			return true;
		}
		return false;
	}

	/**
	 * Try to parse an end tag.
	 * 
	 * @param data
	 *            The data to parse.
	 * @return Whether an end tag was found.
	 */
	private boolean parseEnd(String data) {
		if ("#EXT-X-ENDLIST".equals(data)) {
			mFinal = true;
			return true;
		}
		return false;
	}
}
