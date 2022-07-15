package org.grits.toolbox.tools.spectrafiltering.utils;

import java.nio.ByteBuffer;

import java.util.Base64;

public class Base64SpectraUtil {

	/**
	 * Method that takes a spectrum as float matix (float[2][number of peaks])
	 * and converts it into a base64 encoded string.
	 *
	 * @param a_spectrum
	 *            float matrix of the spectra the first row (float[0]) are the
	 *            m/z values and the second row (float[1]) are the intensity
	 *            values
	 * @return Base64 encoded string of the spectra
	 * @throws IllegalArgumentException
	 *             if the float matrix has the wrong size
	 */
	public static String encodeBase64(float[][] a_spectrum) throws IllegalArgumentException {
		// some testing
		if (a_spectrum.length != 2) {
			throw new IllegalArgumentException(
			        "Must be an array with two columns, but has " + Integer.toString(a_spectrum.length));
		}

		// get the number or peaks
		int t_peakCount = a_spectrum[0].length;

		// create the byte buffer, for each peak we need 8 bytes
		ByteBuffer t_peakBuffer = ByteBuffer.allocate(t_peakCount * 8);

		// fill the buffer with the peaks
		for (int i = 0; i < t_peakCount; i++) {
			t_peakBuffer.putFloat(a_spectrum[0][i]);
			t_peakBuffer.putFloat(a_spectrum[1][i]);
		}

		// get the bytes
		byte[] t_bytes = t_peakBuffer.array();

		// base64 encode the bytes
		byte[] t_encode = Base64.getEncoder().encode(t_bytes);
		String t_base64 = new String(t_encode);
		return t_base64;
	}

}

