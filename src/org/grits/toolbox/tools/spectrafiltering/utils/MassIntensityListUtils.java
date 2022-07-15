package org.grits.toolbox.tools.spectrafiltering.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for handling m/z - intensity lists. The lists must be
 * {@code double[][]} type, which the first parenthesis must indicate m/z value
 * (0) or intensity (1) and the second parenthesis must indicate index of the
 * list.
 * 
 * @author Masaaki Matsubara (matsubara@uga.edu)
 *
 */
public class MassIntensityListUtils {

	/**
	 * Adds a m/z - intensity list to the other one for profile mode.
	 * 
	 * @param dMassIntenListRef
	 *            m/z - intensity list to be added
	 * @param dMassIntenListToAdd
	 *            m/z - intensity list to add
	 * @return A resulting m/z - intensity list
	 */
	public static double[][] addMassIntensityListForProfile(double[][] dMassIntenListRef,
			double[][] dMassIntenListToAdd) {
		// Search maximum interval except for big gap
		int nMass = dMassIntenListToAdd[0].length;
		double dMaxInterval = -1.0D;
		double dMinInterval = Double.MAX_VALUE;
		for (int j = 1; j < nMass; j++)
			if (dMassIntenListToAdd[1][j] != 0.0D || dMassIntenListToAdd[1][j - 1] != 0.0D) {
				double dInterval = dMassIntenListToAdd[0][j] - dMassIntenListToAdd[0][j - 1];
				dMaxInterval = Math.max(dMaxInterval, dInterval);
				dMinInterval = Math.min(dMinInterval, dInterval);
			}

		List<double[]> lMassIntenNew = new ArrayList<>();
		Set<Integer> lUsedIndexes = new HashSet<>();
		int nRef = dMassIntenListRef[0].length;
		int j = 0;
		for (int i = 0; i < nRef; i++) {
			double dMassRef = dMassIntenListRef[0][i];
			double dIntenRef = dMassIntenListRef[1][i];
			// Search nearest two points
			while (j < nMass && dMassRef > dMassIntenListToAdd[0][j])
				j++;
			boolean bNoHigh = true;
			boolean bNoLow = true;
			double dMassHigh = -1.0D;
			double dIntenHigh = 0.0D;
			double dMassLow = -1.0D;
			double dIntenLow = 0.0D;
			if (j < nMass && Math.abs(dMassIntenListToAdd[0][j] - dMassRef) <= dMaxInterval) {
				bNoHigh = false;
				dMassHigh = dMassIntenListToAdd[0][j];
				dIntenHigh = dMassIntenListToAdd[1][j];
				if (!lUsedIndexes.contains(j))
					lUsedIndexes.add(j);
			}
			if (dMassRef == dMassHigh) {
				lMassIntenNew.add(new double[] { dMassRef, dIntenRef + dIntenHigh });
				continue;
			}
			if (j > 0 && Math.abs(dMassIntenListToAdd[0][j - 1] - dMassRef) <= dMaxInterval) {
				bNoLow = false;
				dMassLow = dMassIntenListToAdd[0][j - 1];
				dIntenLow = dMassIntenListToAdd[1][j - 1];
				if (!lUsedIndexes.contains(j - 1))
					lUsedIndexes.add(j - 1);
			}
			if (dMassRef == dMassLow) {
				lMassIntenNew.add(new double[] { dMassRef, dIntenRef + dIntenLow });
				continue;
			}

			if (bNoHigh && bNoLow) {
				lMassIntenNew.add(new double[] { dMassRef, dIntenRef });
				continue;
			}
			if (!bNoHigh && bNoLow)
				dMassLow = dMassHigh - dMaxInterval;
			if (bNoHigh && !bNoLow)
				dMassHigh = dMassLow + dMaxInterval;

			// Add intensity averaged with the distances
			dIntenRef += ((dIntenHigh - dIntenLow) / (dMassHigh - dMassLow) * (dMassRef - dMassLow)) + dIntenLow;
			lMassIntenNew.add(new double[] { dMassRef, dIntenRef });
		}
		for (j = 0; j < nMass; j++)
			if (!lUsedIndexes.contains(j))
				lMassIntenNew.add(new double[] { dMassIntenListToAdd[0][j], dMassIntenListToAdd[1][j] });

		sortMassIntenList(lMassIntenNew);

		return convertMassIntensityList(lMassIntenNew);
	}

	/**
	 * Adds a m/z - intensity list to the other one for centroid mode.
	 * 
	 * @param dMassIntenListRef
	 *            m/z - intensity list to be added
	 * @param dMassIntenListToAdd
	 *            m/z - intensity list to add
	 * @param dAcc
	 * @param bIsPPM
	 * @return A resulting m/z - intensity list
	 */
	public static double[][] addMassIntensityListForCentroid(double[][] dMassIntenListRef,
			double[][] dMassIntenListToAdd, double dAcc, boolean bIsPPM) {
		int nMass_i = dMassIntenListRef[0].length;
		int nMass_j = dMassIntenListToAdd[0].length;

		// Search the indexes having the same m/z value
		Map<Integer, List<Integer>> mapIToJ = new HashMap<>();
		mapIToJ.put(0, new ArrayList<>());
		Map<Integer, List<Integer>> mapJToI = new HashMap<>();
		int i = 0, j = 0;
		while (i < nMass_i && j < nMass_j) {
			// Skip if zero intensity
			if (dMassIntenListRef[1][i] == 0.0D) {
				i++;
				mapIToJ.put(i, new ArrayList<>());
				continue;
			}

			double dMass_i = dMassIntenListRef[0][i];
			double dMass_j = dMassIntenListToAdd[0][j];
			// Compare the m/z values
			// TODO: use the same accuracy as MS1 or not
			if (isSamePeaks(dMass_i, dMass_j, dAcc, bIsPPM)) {
				mapIToJ.get(i).add(j);
				if (!mapJToI.containsKey(j))
					mapJToI.put(j, new ArrayList<>());
				mapJToI.get(j).add(i);
				j++;
			} else if (dMass_i < dMass_j) {
				i++;
				mapIToJ.put(i, new ArrayList<>());
				if (!mapIToJ.get(i).isEmpty())
					j = mapIToJ.get(i).get(0);
			} else if (dMass_i > dMass_j) {
				j++;
			}
		}

		// Calculate weights with distances between the corresponding peaks
		Map<String, Double> mapIndexToWeight = new HashMap<>();
		for (int J : mapJToI.keySet()) {

			List<Integer> lIs = mapJToI.get(J);
			if (lIs.size() == 1)
				continue;

			Map<Integer, Double> mapIToWeight = new HashMap<>();
			double dTotWeights = 0;
			double dMass_j = dMassIntenListToAdd[0][J];
			for (int I : lIs) {
				double dMass_i = dMassIntenListRef[0][I];
				double dDist = Math.abs(dMass_i - dMass_j);
				double dWeight = Math.exp(-dDist);
				mapIToWeight.put(I, dWeight);
				dTotWeights += dWeight;
			}
			// Put averaged weights
			for (int I : lIs)
				mapIndexToWeight.put(I + "-" + J, mapIToWeight.get(I) / dTotWeights);
		}

		// Create new mass and intensity list
		List<double[]> lMassIntenNew = new ArrayList<>();
		for (i = 0; i < nMass_i; i++) {

			double dMass_i = dMassIntenListRef[0][i];
			double dInten_i = dMassIntenListRef[1][i];
			List<Integer> lJs = mapIToJ.get(i);
			if (lJs == null || lJs.isEmpty()) {
				lMassIntenNew.add(new double[] { dMass_i, dInten_i });
				continue;
			}
			// Add intensities and average masses with its intensities
			for (int J : lJs) {
				double dMass_j = dMassIntenListToAdd[0][J];
				double dInten_j = dMassIntenListToAdd[1][J];
				// Weight
				if (mapIndexToWeight.containsKey(i + "-" + J))
					dInten_j *= mapIndexToWeight.get(i + "-" + J);

				double dIntenTot = dInten_i + dInten_j;
				dMass_i = dMass_i * dInten_i / dIntenTot + dMass_j * dInten_j / dIntenTot;
				dInten_i = dIntenTot;
			}
			lMassIntenNew.add(new double[] { dMass_i, dInten_i });
		}
		// Add remained j peaks
		for (j = 0; j < nMass_j; j++)
			if (!mapJToI.containsKey(j))
				lMassIntenNew.add(new double[] { dMassIntenListToAdd[0][j], dMassIntenListToAdd[1][j] });
		sortMassIntenList(lMassIntenNew);

		return convertMassIntensityList(lMassIntenNew);
	}

	private static void sortMassIntenList(List<double[]> lMassIntenList) {
		// Sort with their m/z values
		Collections.sort(lMassIntenList, new Comparator<double[]>() {
			@Override
			public int compare(double[] o1, double[] o2) {
				if (o1[0] < o2[0])
					return -1;
				if (o1[0] > o2[0])
					return 1;
				return 0;
			}
		});
	}

	private static double[][] convertMassIntensityList(List<double[]> lMassIntenList) {
		int nNewMass = lMassIntenList.size();
		double[][] dNewMassIntensityList = new double[2][nNewMass];
		for (int i = 0; i < nNewMass; i++) {
			double[] dMassInten = lMassIntenList.get(i);
			dNewMassIntensityList[0][i] = dMassInten[0];
			dNewMassIntensityList[1][i] = dMassInten[1];
		}
		return dNewMassIntensityList;
	}

	/**
	 * Judges the two given m/z values are the same based on the given accuracy
	 * value.
	 * 
	 * @param dMz1
	 *            A m/z value of first peak
	 * @param dMz2
	 *            A m/z value of second peak
	 * @param accValue
	 *            An accuracy to judge the two values are the same
	 * @param bIsPPM
	 *            A flag to consider the given accuracy as PPM value
	 * @return {@code true} if the given m/z values are judged as the same peak
	 */
	public static boolean isSamePeaks(double dMz1, double dMz2, double accValue, boolean bIsPPM) {
		if (bIsPPM)
			accValue = ((dMz1 + dMz2) / 2 / 1000000) * accValue;

		return (Math.abs(dMz1 - dMz2) <= accValue);
	}

	/**
	 * Sorts m/z - intensity list in order of m/z value
	 * 
	 * @param dMassIntenList
	 *            m/z - intensity list to sort
	 * @return The sorted m/z - intensity list in order of m/z value
	 */
	public static double[][] sortMassIntensityListWithMz(final double[][] dMassIntenList) {
		int n = dMassIntenList[0].length;
		List<Integer> lIndexes = new ArrayList<>();
		for (int i = 0; i < n; i++)
			lIndexes.add(i);
		Collections.sort(lIndexes, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if (dMassIntenList[0][o1] < dMassIntenList[0][o2])
					return -1;
				if (dMassIntenList[0][o1] > dMassIntenList[0][o2])
					return 1;
				return 0;
			}
		});
		double[][] dMassIntenNew = new double[2][n];
		for (int i = 0; i < n; i++) {
			dMassIntenNew[0][i] = dMassIntenList[0][lIndexes.get(i)];
			dMassIntenNew[1][i] = dMassIntenList[1][lIndexes.get(i)];
		}
		return dMassIntenNew;
	}

	/**
	 * Eliminates peaks with zero intensity from the given m/z - intensity list. In
	 * the profile mode, there are peaks with zero intensity
	 * 
	 * @param dMassIntenList
	 *            double[][] of m/z - intensity list to be eliminated peaks with
	 *            zero intensity
	 * @return A new double[][] of m/z - intensity list eliminated peaks with zero
	 *         intensity
	 */
	public static double[][] eliminateSequentialZeroIntensities(double[][] dMassIntenList) {
		// Collect non-zero peaks
		List<Integer> lIds = new ArrayList<>();
		int n = dMassIntenList[0].length;
		for (int i = 0; i < n; i++) {
			// Ignore zero intensity between two zero intensities
			if (dMassIntenList[1][i] == 0.0f && (i > 0 && dMassIntenList[1][i - 1] == 0.0f)
					&& (i < n - 1 && dMassIntenList[1][i + 1] == 0.0f))
				continue;
			lIds.add(i);
		}
		double[][] dMassIntenNew = new double[2][lIds.size()];
		for (int i = 0; i < lIds.size(); i++) {
			dMassIntenNew[0][i] = dMassIntenList[0][lIds.get(i)];
			dMassIntenNew[1][i] = dMassIntenList[1][lIds.get(i)];
		}
		return dMassIntenNew;
	}

	/**
	 * Divides intensities of the given m/z - intensity list with the given value.
	 * 
	 * @param dMassIntenList
	 *            m/z - intensity list to divide intensities
	 * @param fToDevide
	 *            the value to divide intensities
	 * @return m/z - intensity list with divided intensities
	 */
	public static double[][] normalizeIntensities(double[][] dMassIntenList, float fToDevide) {
		int n = dMassIntenList[0].length;
		double[][] dMassIntenNew = new double[2][n];
		for (int i = 0; i < n; i++) {
			dMassIntenNew[0][i] = dMassIntenList[0][i];
			dMassIntenNew[1][i] = dMassIntenList[1][i];
			if (dMassIntenNew[1][i] != 0.0D)
				dMassIntenNew[1][i] /= fToDevide;
		}
		return dMassIntenNew;
	}

	/**
	 * Extracts the given numbers of highest peaks in the given m/z - intensity
	 * list.
	 * 
	 * @param dMassIntenList
	 *            m/z - intensity list to extract highest peaks
	 * @param nPeaks
	 *            The number of highest peaks to be extracted
	 * @return m/z - intensity list of the highest peaks in the given list
	 */
	public static double[][] extractHighestPeaks(final double[][] dMassIntenList, int nPeaks) {
		int nMass = dMassIntenList[0].length;
		List<Integer> lHighestIndexes = new ArrayList<>();
		for (int i = 0; i < nMass; i++) {
			if (lHighestIndexes.size() < nPeaks) {
				lHighestIndexes.add(i);
				continue;
			}
			// Find lowest peak in the highest peaks
			Integer iLowest = lHighestIndexes.get(0);
			for (Integer j : lHighestIndexes) {
				if (j == iLowest)
					continue;
				if (dMassIntenList[1][iLowest] > dMassIntenList[1][j])
					iLowest = j;
			}
			if (dMassIntenList[1][iLowest] >= dMassIntenList[1][i])
				continue;
			lHighestIndexes.remove(iLowest);
			lHighestIndexes.add(i);
		}
		double[][] dHighestPeaks = new double[2][nPeaks];
		for (int i = 0; i < lHighestIndexes.size(); i++) {
			int iOld = lHighestIndexes.get(i);
			dHighestPeaks[0][i] = dMassIntenList[0][iOld];
			dHighestPeaks[1][i] = dMassIntenList[1][iOld];
		}
		return dHighestPeaks;
		// return sortMassIntensityListWithMz(dHighestPeaks);
	}

	public static double[][] extractPeaksNearToTheHighestPeak(double[][] dMassIntenList, double interval) {
		// Find a highest peak index
		int nMass = dMassIntenList[0].length;
		int iHighest = 0;
		for (int i = 0; i < nMass; i++)
			if (dMassIntenList[1][iHighest] < dMassIntenList[1][i])
				iHighest = i;
		// Find a group of peaks
		double dHighestMz = dMassIntenList[0][iHighest];
		int iMax = iHighest, iMin = iHighest;
		if (iHighest > 1)
			for (int i = iHighest - 1; i >= 0; i--) {
				if (dMassIntenList[1][i] == 0)
					break;
				if (Math.abs(dHighestMz - dMassIntenList[0][i]) > interval)
					break;
				iMin = i;
			}
		if (iHighest < nMass - 1)
			for (int i = iHighest + 1; i < nMass; i++) {
				if (dMassIntenList[1][i] == 0)
					break;
				if (Math.abs(dHighestMz - dMassIntenList[0][i]) > interval)
					break;
				iMax = i;
			}
		nMass = iMax - iMin + 1;
		double[][] dHighestPeaks = new double[2][nMass];
		for (int i = 0; i < nMass; i++) {
			int iOld = i + iMin;
			dHighestPeaks[0][i] = dMassIntenList[0][iOld];
			dHighestPeaks[1][i] = dMassIntenList[1][iOld];
		}
		return dHighestPeaks;
	}

	/**
	 * Returns minimum interval of the given list of m/z values
	 * 
	 * @param lMzValues
	 * @return The m/z value of the minimum interval in the given m/z values
	 *         ({@code -1} if the given m/z values are not listed sequentially)
	 */
	public static double getMinimumInterval(double[] lMzValues) {
		double dMinInterval = Double.MAX_VALUE;
		double dCurrent = lMzValues[0];
		for (int i = 1; i < lMzValues.length; i++) {
			double dNext = lMzValues[i];
			double dInterval = dNext - dCurrent;
			if (dInterval < 0) {
				System.err.println("The masses are not sorted.");
				return -1;
			}
			dMinInterval = Math.min(dMinInterval, dInterval);
			dCurrent = dNext;
		}
		return dMinInterval;
	}

}
