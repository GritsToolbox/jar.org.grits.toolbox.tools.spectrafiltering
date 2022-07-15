package org.grits.toolbox.tools.spectrafiltering.process;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.grits.toolbox.tools.spectrafiltering.dialog.process.ProgressDialogThread;
import org.grits.toolbox.tools.spectrafiltering.om.SpectraFilterSettings;
import org.grits.toolbox.tools.spectrafiltering.utils.MassIntensityListUtils;
import org.grits.toolbox.tools.spectrafiltering.utils.MzXMLWriter;
import org.systemsbiology.jrap.grits.stax.MSXMLParser;
import org.systemsbiology.jrap.grits.stax.Scan;
import org.systemsbiology.jrap.grits.stax.ScanHeader;

public class SpectraAverageThread extends ProgressDialogThread {

	private SpectraFilterSettings m_filter;
	private MSXMLParser m_parser;

	public SpectraAverageThread(SpectraFilterSettings a_settings) {
		this.m_filter = a_settings;
		this.m_parser = new MSXMLParser(this.m_filter.getOpenFrom());
	}

	@Override
	public boolean threadStart() throws Exception {
		Map<Integer, List<ScanHeader>> mapMS1ToMS2ScanHeaders;
		List<Integer> lMS1Scans = new ArrayList<>();
		try {
			this.m_progressReporter.setProcessMessageLabel("Task 1 of 4: collect scan information");
			mapMS1ToMS2ScanHeaders = collectScanInfo();
			lMS1Scans.addAll(mapMS1ToMS2ScanHeaders.keySet());
			Collections.sort(lMS1Scans);
		} catch (CancelProcessException e) {
			return false;
		}

		Map<Integer, List<Integer>> mapFirstScanToSameScans;
		try {
			this.m_progressReporter.setProcessMessageLabel("Task 2 of 4: find redundant scans");
			mapFirstScanToSameScans = findRedundantScans(mapMS1ToMS2ScanHeaders);
			mapMS1ToMS2ScanHeaders.clear();
		} catch (CancelProcessException e) {
			return false;
		}

		List<Scan> lNewScans = new ArrayList<>();
		try {
			this.m_progressReporter.setProcessMessageLabel("Task 3 of 4: average scans");
			this.m_progressReporter.setMax(mapFirstScanToSameScans.size()+1);
			// Average MS1 scans
			this.m_progressReporter.updateProgresBar("Averaging MS1 Scans ...");
			Integer iFirstMS1Scan = lMS1Scans.get(0);
			lMS1Scans.remove(iFirstMS1Scan);
			Scan scan = averageScans(iFirstMS1Scan, lMS1Scans);
			lNewScans.add(scan);
			// Average MS2 scans
			for (int iMS2 : mapFirstScanToSameScans.keySet()) {
				this.m_progressReporter.updateProgresBar("Averaging MS2 Scans #" + iMS2);
				List<Integer> lSameScans = mapFirstScanToSameScans.get(iMS2);
				scan = averageScans(iMS2, lSameScans);
				scan.header.setPrecursorScanNum(iFirstMS1Scan);
				lNewScans.add(scan);
			}
		} catch (CancelProcessException e) {
			return false;
		}

		try {
			this.m_progressReporter.setProcessMessageLabel("Task 4 of 4: create mzXML file");
			this.outputMzXML(lNewScans);
		} catch (CancelProcessException e) {
			return false;
		}

		this.m_progressReporter.setProcessMessageLabel("");
		return true;
	}

	private Map<Integer, List<ScanHeader>> collectScanInfo() throws CancelProcessException {
		int nMax = m_parser.getMaxScanNumber();

		this.m_progressReporter.setMax(-1);
		this.m_progressReporter.updateProgresBar("Collecting MS2 Scans ...");
		// Collect ScanHeaders of MS2 scans and the relationships to the parent MS1
		Map<Integer, List<ScanHeader>> mapMS1ToMS2 = new TreeMap<>();
		for (int i = 1; i <= nMax; i++) {
			if (this.m_canceled)
				throw new CancelProcessException();

			ScanHeader header1 = m_parser.rapHeader(i);
			if (header1 == null || header1.getMsLevel() != 2 ) {
				if (header1.getMsLevel() == 1 && !mapMS1ToMS2.containsKey(header1.getNum()))
					mapMS1ToMS2.put(header1.getNum(), new ArrayList<>());
				continue;
			}
			// Ignore if no peak in the scan
			if ( header1.getPeaksCount() == 0 )
				continue;

			mapMS1ToMS2.get(header1.getPrecursorScanNum()).add(header1);
		}

		this.m_progressReporter.updateProgresBar("Done!");

		return mapMS1ToMS2;
	}

	private Map<Integer, List<Integer>> findRedundantScans(Map<Integer, List<ScanHeader>> mapMS1ToMS2Header)
			throws CancelProcessException {

		Map<Integer, List<Integer>> mapFirstScanToSameScans = new TreeMap<>();

		// Filter scans
		List<Integer> lMS1ScanNums = new ArrayList<>();
		lMS1ScanNums.addAll(mapMS1ToMS2Header.keySet());

		int nMS1 = lMS1ScanNums.size();
		this.m_progressReporter.setMax(-1);

		this.m_progressReporter.updateProgresBar("Comparing the precursor m/z values of MS2 scans ...");

		// Seek the MS1 scans
		Set<ScanHeader> setCheckedMS2Scans = new HashSet<>();
		for (int i = 0; i < nMS1; i++) {
			if (this.m_canceled)
				throw new CancelProcessException();

			int iMS1 = lMS1ScanNums.get(i);

			// MS2 scans in a MS1 scan
			for (ScanHeader header1 : mapMS1ToMS2Header.get(iMS1)) {
				if (this.m_canceled)
					throw new CancelProcessException();

				if (setCheckedMS2Scans.contains(header1))
					continue;

				int iMS2 = header1.getNum();

				List<Integer> lSameScans = new ArrayList<>();

				// The other MS1 scan
				for (int j = i + 1; j < nMS1; j++) {
					if (this.m_canceled)
						throw new CancelProcessException();

					int jMS1 = lMS1ScanNums.get(j);

					// MS2 scans in the other MS1 scan
					for (ScanHeader header2 : mapMS1ToMS2Header.get(jMS1)) {
						if (this.m_canceled)
							throw new CancelProcessException();

						if (setCheckedMS2Scans.contains(header2))
							continue;

						// Compare the two headers
						if (!this.isSameScan(header1, header2))
							continue;

						setCheckedMS2Scans.add(header2);

						lSameScans.add(header2.getNum());
						break;
					}
				}

				mapFirstScanToSameScans.put(iMS2, lSameScans);
			}
		}
		this.m_progressReporter.updateProgresBar("Done!");

		return mapFirstScanToSameScans;
	}

	private boolean isSameScan(ScanHeader header1, ScanHeader header2) {

		// Compare activation method
		if (!header1.getActivationMethod().equals(header2.getActivationMethod()))
			return false;

		// Compare charge
		if (header1.getPrecursorCharge() != header2.getPrecursorCharge())
			return false;

		// Compare precursor m/z values with accuracy
		if (!MassIntensityListUtils.isSamePeaks(header1.getPrecursorMz(), header2.getPrecursorMz(),
				m_filter.getAccuracy(), m_filter.getPPM()))
			return false;

		return true;
	}

	private Scan averageScans(int iFirstScanId, List<Integer> lScanIds) throws CancelProcessException {
		if (lScanIds == null || lScanIds.isEmpty())
			return m_parser.rap(iFirstScanId);

		Scan scan0 = m_parser.rap(iFirstScanId);

		// Get header info
		float fTotIonCurrent = scan0.header.getTotIonCurrent();
		double[][] dPrecursorMassIntenList = new double[2][lScanIds.size() + 1];
		dPrecursorMassIntenList[0][0] = scan0.header.getPrecursorMz();
		dPrecursorMassIntenList[1][0] = scan0.header.getPrecursorIntensity();
		float fStartMz = scan0.header.getStartMz();
		float fEndMz = scan0.header.getEndMz();

		double[][] dMassIntenList = scan0.getMassIntensityList();

		int j = 0;
		for (int iScanId : lScanIds) {
			if (this.m_canceled)
				throw new CancelProcessException();

			Scan scan = m_parser.rap(iScanId);

			// Collect precursor info
			j++;
			dPrecursorMassIntenList[0][j] = scan.header.getPrecursorMz();
			dPrecursorMassIntenList[1][j] = scan.header.getPrecursorIntensity();
			// Update start and end m/z values which are the instrumental setting
			fStartMz = Math.min(fStartMz, scan.header.getStartMz());
			fEndMz = Math.max(fEndMz, scan.header.getEndMz());
			// Update total ion current
			fTotIonCurrent += scan0.header.getTotIonCurrent();

			if (scan0.header.getCentroided() != 1 && scan.header.getCentroided() != 1)
				dMassIntenList = MassIntensityListUtils.addMassIntensityListForProfile(dMassIntenList,
						scan.getMassIntensityList());
			else
				dMassIntenList = MassIntensityListUtils.addMassIntensityListForCentroid(dMassIntenList,
						scan.getMassIntensityList(), m_filter.getAccuracy(), m_filter.getPPM());
		}

		if (this.m_canceled)
			throw new CancelProcessException();

		int nMass = dMassIntenList[0].length;
		int nScans = j + 1;
		// Calculate new info for the header
		float dBaseInten = 0;
		float dBaseMz = 0;
		for (int i = 0; i < nMass; i++) {
			// Average intensities
			dMassIntenList[1][i] /= nScans;
			if (dBaseInten < dMassIntenList[1][i]) {
				dBaseInten = (float) dMassIntenList[1][i];
				dBaseMz = (float) dMassIntenList[0][i];
			}
		}
		fTotIonCurrent /= nScans;
		double dPrecursorMz = 0.0D;
		double dPrecursorIntensity = 0.0D;
		for (int i = 0; i < nScans; i++) {
			dPrecursorMz += dPrecursorMassIntenList[0][i] * dPrecursorMassIntenList[1][i];
			dPrecursorIntensity += dPrecursorMassIntenList[1][i];
		}
		if ( dPrecursorMz == 0.0D ) {
			for (int i = 0; i < nScans; i++)
				dPrecursorMz += dPrecursorMassIntenList[0][i];
			dPrecursorMz /= nScans;
		} else if ( dPrecursorIntensity != 0 )
			dPrecursorMz /= dPrecursorIntensity;
		dPrecursorIntensity /= nScans;

		// Update ScanHeader
		ScanHeader headerNew = scan0.header;
		headerNew.setPeaksCount(nMass);
		headerNew.setStartMz(fStartMz);
		headerNew.setEndMz(fEndMz);
		headerNew.setLowMz((float) dMassIntenList[0][0]);
		headerNew.setHighMz((float) dMassIntenList[0][nMass - 1]);
		headerNew.setBasePeakMz(dBaseMz);
		headerNew.setBasePeakIntensity(dBaseInten);
		headerNew.setTotIonCurrent(fTotIonCurrent);

		headerNew.setPrecursorMz((float) dPrecursorMz);
		headerNew.setPrecursorIntensity((float) dPrecursorIntensity);

		// Create a new Scan
		Scan scanNew = new Scan();
		scanNew.setHeader(headerNew);
		scanNew.setMassIntensityList(dMassIntenList);
		return scanNew;
	}

	private void outputMzXML(List<Scan> lScans) throws CancelProcessException, IOException {
		int nScans = lScans.size();
		this.m_progressReporter.setMax(nScans + 3);

		// Create the MzXML writer
		MzXMLWriter writer = new MzXMLWriter();

		// Create mzXML file
		writer.createMZXML(this.m_filter.getSaveLocation());

		if (this.m_canceled)
			throw new CancelProcessException();

		String strStartTime = lScans.get(0).header.getRetentionTime();
		String strEndTime = lScans.get(nScans - 1).header.getRetentionTime();
		writer.setMsRun(nScans, strStartTime, strEndTime);

		// Write Header
		this.m_progressReporter.updateProgresBar("Writing header");
		writer.write(writer.getHeader());
		// writer.writeHeader();

		for (Scan scan : lScans) {
			if (this.m_canceled) {
				writer.closeFile();
				throw new CancelProcessException();
			}
			this.m_progressReporter.updateProgresBar("Writing scan #" + scan.getHeader().getNum());
			writer.write(writer.getScan(scan));
		}

		this.m_progressReporter.updateProgresBar("Writing footer");
		writer.write(writer.getFooter());
		// writer.writeFooter();

		// close the file
		writer.closeFile();
		this.m_progressReporter.updateProgresBar("Done!");
	}
}
