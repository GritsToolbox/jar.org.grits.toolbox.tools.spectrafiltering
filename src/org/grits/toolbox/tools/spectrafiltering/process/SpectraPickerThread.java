package org.grits.toolbox.tools.spectrafiltering.process;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.grits.toolbox.tools.spectrafiltering.dialog.process.ProgressDialogThread;
import org.grits.toolbox.tools.spectrafiltering.om.SpectraPickerSettings;
import org.grits.toolbox.tools.spectrafiltering.utils.MzXMLFormatException;
import org.grits.toolbox.tools.spectrafiltering.utils.MzXMLWriter;
import org.systemsbiology.jrap.grits.stax.MSXMLParser;
import org.systemsbiology.jrap.grits.stax.Scan;
import org.systemsbiology.jrap.grits.stax.ScanHeader;

public class SpectraPickerThread extends ProgressDialogThread {

	private SpectraPickerSettings m_filter;
	private MSXMLParser m_parser;

	private boolean m_writeParentScan = false;

	public SpectraPickerThread(SpectraPickerSettings a_filter) {
		this.m_filter = a_filter;
		this.m_parser = new MSXMLParser(this.m_filter.getOpenFrom());
	}

	@Override
	public boolean threadStart() throws Exception {

		LinkedList<Integer> lFilteredScans;
		this.m_progressReporter.setProcessMessageLabel("Task 1 of 2: Filter scans");
		try {
			lFilteredScans = this.filterScans();
		} catch (CancelProcessException e) {
			return false;
		} catch (Exception e) {
			this.m_progressReporter.setDescriptionText(
					"Error reading scans from " + this.m_filter.getOpenFrom() + "\n" + e.getMessage());
			return false;
		}

				this.m_progressReporter.setProcessMessageLabel("Task 2 of 2: Create filtered mzXML file");
		try {
			this.writeMzXML(lFilteredScans);
		} catch (IOException e) {
			this.m_progressReporter.setDescriptionText("Error when writting the new mzXML file: " + e.getMessage());
			this.deleteMzXMLFile();
			return false;
		} catch (CancelProcessException e) {
			this.deleteMzXMLFile();
			return false;
		}

		return true;
	}

	private LinkedList<Integer> filterScans() throws CancelProcessException {
		LinkedList<Integer> lFilteredScans = new LinkedList<>();

		// Filter scans
		int nMax = m_parser.getMaxScanNumber();
		this.m_progressReporter.setMax(nMax);
		int nCheckPoint = 1;
		if ( nMax > 1000 ) {
			this.m_progressReporter.setMax(1000);
			nCheckPoint = nMax / 1000;
		}
		for( int i = 1; i < nMax + 1; i++ ) {
			ScanHeader header = m_parser.rapHeader(i);
			if ( header == null || header.getMsLevel() != 1 )
				continue;

			// MS1 scan
			if ( i % nCheckPoint == 0 )
				this.m_progressReporter.updateProgresBar("Reading Scan #" + header.getNum());
			int iMS1 = i;

			if (this.m_canceled)
				throw new CancelProcessException();

			List<Integer> lScans = new ArrayList<>();
			lScans.add(iMS1);

			// Reads through next MS1 scan to seek subscans
			while( i < nMax + 1 ) {
				i++;

				if (this.m_canceled)
					throw new CancelProcessException();

				ScanHeader headerSub = m_parser.rapHeader(i);
				if ( headerSub == null )
					continue;
				// Break at next MS1 scan
				if( headerSub.getMsLevel() == header.getMsLevel() ) {
					i--;
					break;
				}
				if ( i % nCheckPoint == 0 )
					this.m_progressReporter.updateProgresBar("Reading Scan #" + header.getNum());

				// Skips if precursor scan is not parent MS1 scan
				if ( headerSub.getPrecursorScanNum() != iMS1 )
					continue;

				// Skips if no peaks in this scan
				if ( headerSub.getPeaksCount() == 0 )
					continue;

				// Filters scan
				if ( this.filterScan(m_parser.rap(i)) )
					lScans.add(i);
			}
			// Removes MS1 scan if the flag is true
			if ( !this.m_writeParentScan )
				lScans.remove(0);
			if ( lScans.isEmpty() )
				continue;
			lFilteredScans.addAll(lScans);
		}

		return lFilteredScans;
	}

	private boolean filterScan(Scan a_scan) throws CancelProcessException {
		if ( a_scan == null )
			return false;

		if (this.m_canceled)
			throw new CancelProcessException();

		// Calculate interval
		Double mzValue = m_filter.getMzValue();
		Double accuracyValue = m_filter.getAccuracy();
		if ( m_filter.getPPM() )
			accuracyValue = (mzValue / 1000000) * accuracyValue;
		Double minInterval = mzValue - accuracyValue;
		Double maxInterval = mzValue + accuracyValue;

		double[][] massIntensityList = a_scan.getMassIntensityList();

		// Double highestPeakIntensityInSpectra =
		// a_scan.getPeaklist().get(0).getIntensity();
		Double highestPeakIntensity = massIntensityList[1][0];
		/* first vector is masses and second vector is the intensity */

		// Find highest peak
		for (int i = 1; i < massIntensityList[1].length; i++) {
			if (this.m_canceled)
				throw new CancelProcessException();

			highestPeakIntensity = Math.max(highestPeakIntensity, massIntensityList[1][i]);
		}

		// Set cut off value
		Double cutOffValue = m_filter.getCutOffValue();
		if ( m_filter.getPercentage() )
			cutOffValue = highestPeakIntensity * cutOffValue / 100;

		// Find matched peak
		for (int i = 0; i < massIntensityList[0].length; i++) {
			if (this.m_canceled)
				throw new CancelProcessException();

			// Skip if the intensity is smaller than cut off value
			if ( massIntensityList[1][i] < cutOffValue )
				continue;

			// Skip if the m/z value is out of the range
			if (massIntensityList[0][i] > maxInterval)
				continue;
			if ( massIntensityList[0][i] < minInterval)
				continue;

			// Returns true when any peak is matched
			return true;
		}

		return false;
	}

	private void writeMzXML(LinkedList<Integer> a_lScanIndexes) throws IOException, CancelProcessException, MzXMLFormatException {

		this.m_progressReporter.setMax(a_lScanIndexes.size() + 3);

		// Create the MzXML writer
		MzXMLWriter writer = new MzXMLWriter();

		// Create mzXML file
		writer.createMZXML(this.m_filter.getSaveLocation());

		if (this.m_canceled)
			throw new CancelProcessException();

		String strStartTime = m_parser.rapHeader(a_lScanIndexes.getFirst()).getRetentionTime();
		String strEndTime = m_parser.rapHeader(a_lScanIndexes.getLast()).getRetentionTime();
		writer.setMsRun(a_lScanIndexes.size(), strStartTime, strEndTime);

		// Write Header
		this.m_progressReporter.updateProgresBar("Writing header");
		writer.write(writer.getHeader());
//		writer.writeHeader();

		for (int iScan : a_lScanIndexes) {
			if (this.m_canceled) {
				writer.closeFile();
				throw new CancelProcessException();
			}
			Scan scan = this.m_parser.rap(iScan);
			this.m_progressReporter.updateProgresBar("Writing scan #" + scan.getHeader().getNum());
			writer.write(writer.getScan(scan));
		}

		this.m_progressReporter.updateProgresBar("Writing footer");
		writer.write(writer.getFooter());
//		writer.writeFooter();

		// close the file
		writer.closeFile();
		this.m_progressReporter.updateProgresBar("Done!");
	}

	private void deleteMzXMLFile() throws IOException {
		if ( !Files.exists(Paths.get(this.m_filter.getSaveLocation())) )
				return;
		Files.delete(Paths.get(this.m_filter.getSaveLocation()));
	}
}
