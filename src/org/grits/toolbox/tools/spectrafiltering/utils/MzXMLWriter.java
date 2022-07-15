package org.grits.toolbox.tools.spectrafiltering.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.systemsbiology.jrap.grits.stax.DataProcessingInfo;
import org.systemsbiology.jrap.grits.stax.MSInstrumentInfo;
import org.systemsbiology.jrap.grits.stax.ParentFile;
import org.systemsbiology.jrap.grits.stax.Scan;
import org.systemsbiology.jrap.grits.stax.ScanHeader;
import org.systemsbiology.jrap.grits.stax.SoftwareInfo;

/**
 * 
 * @author Masaaki Matsubara
 * @see http://sashimi.sourceforge.net/schema_revision/mzXML_2.1/Doc/mzXML_2.1_tutorial.pdf
 * @see http://sashimi.sourceforge.net/schema_revision/mzXML_3.2/mzXML_3.2.xsd
 */
public class MzXMLWriter {
	
	private int m_nScanCount;
	private String m_strStartTime;
	private String m_strEndTime;

	private int m_iCurrentScanNum;
	private Map<Integer, Integer> m_mapOldToNewScanNum;

	private int m_iCurrentOffset;
	private Map<Integer, Integer> m_mapScanNumToOffset;

	private BufferedWriter	bw	= null;

	private class Element {
		private String strTitle;
		private List<String> lAttributeNames;
		private List<String> lAttributeValues;
		private List<Boolean> lNeedsAttributeLineBreak;
		private String strText;
		private List<Element> lContents;

		public Element(String strTitle) {
			this.strTitle = strTitle;
			this.lAttributeNames = new ArrayList<>();
			this.lAttributeValues = new ArrayList<>();
			this.lNeedsAttributeLineBreak = new ArrayList<>();
			this.strText = null;
			this.lContents = new ArrayList<>();
		}

		public void setAttribute(String name, String value) {
			this.setAttribute(name, value, false);
		}

		public void setAttribute(String name, String value, boolean needsLineBreak) {
			this.lAttributeNames.add(name);
			this.lAttributeValues.add(value);
			this.lNeedsAttributeLineBreak.add(needsLineBreak);
		}

		public void setText(String text) {
			this.strText = text;
		}

		public void addContent(Element content) {
			this.lContents.add(content);
		}

		@Override
		public String toString() {
			return this.toStringWithIndent(0);
		}

		public String toStringWithIndent(int indent) {
			String strIndent = "";
			for ( int i=0; i<indent; i++ )
				strIndent += ' ';
			String content = strIndent+"<"+this.strTitle;
			String strIndentForAttributes = "";
			for ( int i=0; i<content.length(); i++ )
				strIndentForAttributes += ' ';

			// Add attributes
			int nAttributes = this.lAttributeNames.size();
			for ( int i=0; i<nAttributes; i++ ) {
				String strAttribute = this.lAttributeNames.get(i)+"=\""+this.lAttributeValues.get(i)+"\"";
				if ( this.lNeedsAttributeLineBreak.get(i) )
					strAttribute += "\n"+strIndentForAttributes;
				content += " "+strAttribute;
			}

			content += ">";

			if ( this.strText != null ) {
				content += this.strText+"</"+this.strTitle+">\n";
				return content;
			}

			content += "\n";
			// Add child contents
			for ( Element child : this.lContents ) {
				content += child.toStringWithIndent(indent+2);
			}
			// Close tag
			content += strIndent+"</"+this.strTitle+">\n";

			return content;
		}
	}

	public void createMZXML(String fileName) throws IOException {
		bw = new BufferedWriter(new FileWriter(fileName));

		this.m_iCurrentScanNum = 0;
		this.m_mapOldToNewScanNum = new TreeMap<>();
		this.m_iCurrentOffset = 0;
		this.m_mapScanNumToOffset = new TreeMap<>();
	}

	public void setMsRun(int nScanCount, String strStartTime, String strEndTime) {
		this.m_nScanCount = nScanCount;
		this.m_strStartTime = strStartTime;
		this.m_strEndTime = strEndTime;
	}

	public void write(String content) throws IOException {
		bw.write(content);
	}

	public void closeFile() throws IOException {
		bw.flush();
		bw.close();
	}

	/**
	 * Returns headers of mzXML and msRun
	 * @return String of headers of mzXML and msRun
	 */
	public String getHeader() {
		String content
		= "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"
		+ "<mzXML xmlns=\"http://sashimi.sourceforge.net/schema_revision/mzXML_3.2\"\n"
		+ "       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
		+ "       xsi:schemaLocation=\"http://sashimi.sourceforge.net/schema_revision/mzXML_3.2 http://sashimi.sourceforge.net/schema_revision/mzXML_3.2/mzXML_idx_3.2.xsd\">"
		+ "\n" + getMsRunTag() + "\n";
		this.m_iCurrentOffset += content.length() + 4;
		return content;
	}

	private String getMsRunTag() {
		return "  <msRun scanCount=\""+this.m_nScanCount+"\" startTime=\""+this.m_strStartTime+"\" endTime=\""+this.m_strEndTime+"\">";
	}

	public String getFooter() {
		String closeTag = "  </msRun>\n";
		this.m_iCurrentOffset += closeTag.length();
		// Create index part
		if ( !this.m_mapScanNumToOffset.isEmpty() ) {
			Element elem = new Element("index");
			elem.setAttribute("name", "scan");
			for ( int iScan : this.m_mapScanNumToOffset.keySet() ) {
				int index = this.m_mapScanNumToOffset.get(iScan);
				Element elem_i = new Element("offset");
				elem_i.setAttribute("id", Integer.toString(iScan));
				elem_i.setText(Integer.toString(index));
				elem.addContent(elem_i);
			}
			closeTag += elem.toStringWithIndent(2);
			closeTag += "  <indexOffset>"+this.m_iCurrentOffset+"</indexOffset>\n";
		}
		// TODO: add sha1 tag
		closeTag += "</mzXML>";

		return closeTag;
	}

	public String getParentFile(ParentFile info) {
		Element t_elem = new Element("parentFile");

		t_elem.setAttribute("fileName", info.getURI());
		t_elem.setAttribute("fileType", info.getType());
		t_elem.setAttribute("fileSha1", info.getSha1());

		String t_xml = t_elem.toStringWithIndent(4);
		this.m_iCurrentOffset += t_xml.length();
		return t_xml;
	}

	public String getMSInstrument(MSInstrumentInfo info, int id) throws IOException {
		Element t_elemInst = new Element("msInstrument");

		t_elemInst.setAttribute("msInstrumentID", Integer.toString(id));

		Element t_elem;

		t_elem = new Element("msManufacturer");
		t_elem.setAttribute("category", "msManufacturer");
		t_elem.setAttribute("value", info.getManufacturer());
		t_elemInst.addContent(t_elem);

		t_elem = new Element("msModel");
		t_elem.setAttribute("category", "msModel");
		t_elem.setAttribute("value", info.getModel());
		t_elemInst.addContent(t_elem);

		t_elem = new Element("msIonisation");
		t_elem.setAttribute("category", "msIonisation");
		t_elem.setAttribute("value", info.getIonization());
		t_elemInst.addContent(t_elem);

		t_elem = new Element("msMassAnalyzer");
		t_elem.setAttribute("category", "msMassAnalyzer");
		t_elem.setAttribute("value", info.getMassAnalyzer());
		t_elemInst.addContent(t_elem);

		t_elem = new Element("msDetector");
		t_elem.setAttribute("category", "msDetector");
		t_elem.setAttribute("value", info.getDetector());
		t_elemInst.addContent(t_elem);

		t_elem = new Element("software");
		t_elem.setAttribute("type", info.getSoftwareInfo().type);
		t_elem.setAttribute("name", info.getSoftwareInfo().name);
		t_elem.setAttribute("version", info.getSoftwareInfo().version);
		t_elemInst.addContent(t_elem);

		String content = t_elemInst.toStringWithIndent(4);
		this.m_iCurrentOffset += content.length();
		return content;
	}

	public String getDataProcessing(DataProcessingInfo info) throws IOException {
		Element t_elemParent = new Element("dataProcessing");

		Element t_elem;
		for ( SoftwareInfo soft : info.getSoftwareUsed() ) {
			t_elem = new Element("software");
			t_elem.setAttribute("type", soft.type);
			t_elem.setAttribute("name", soft.name);
			t_elem.setAttribute("version", soft.version);
			t_elemParent.addContent(t_elem);
		}

		if ( info.getIntensityCutoff() != -1 )
			t_elemParent.setAttribute("intensityCutoff", Double.toString(info.getIntensityCutoff()));

		if ( info.getCentroided() != DataProcessingInfo.UNKNOWN )
			t_elemParent.setAttribute("centroided", Integer.toString(info.getCentroided()));

		if ( info.getDeisotoped() != DataProcessingInfo.UNKNOWN )
			t_elemParent.setAttribute("deisotoped", Integer.toString(info.getDeisotoped()));

		if ( info.getChargeDeconvoluted() != DataProcessingInfo.UNKNOWN )
			t_elemParent.setAttribute("chargeDeconvoluted", Integer.toString(info.getChargeDeconvoluted()));

		if ( info.getSpotIntegration() != DataProcessingInfo.UNKNOWN )
			t_elemParent.setAttribute("spotIntegration", Integer.toString(info.getSpotIntegration()));

		String content = t_elemParent.toStringWithIndent(4);
		this.m_iCurrentOffset += content.length();
		return content;
	}

	public String getScan(Scan scan) {
		// Scan number must start from 1 and increase sequentially!
		this.m_iCurrentScanNum++;

		ScanHeader scanHeader = scan.getHeader();
		// Map old scan number to new one
		this.m_mapOldToNewScanNum.put(scanHeader.getNum(), this.m_iCurrentScanNum);

		// Map scan number to the offset value of this scan tag which will be used as an index
		this.m_mapScanNumToOffset.put(this.m_iCurrentScanNum, this.m_iCurrentOffset);

		Element t_elemScan = new Element("scan");

		t_elemScan.setAttribute("num", Integer.toString(this.m_iCurrentScanNum), true);
		t_elemScan.setAttribute("msLevel", Integer.toString(scanHeader.getMsLevel()), true);
		t_elemScan.setAttribute("peaksCount", Integer.toString(scan.getMassIntensityList()[0].length), true);
		if ( scanHeader.getPolarity() != null || !scanHeader.getPolarity().isEmpty() )
			t_elemScan.setAttribute("polarity", scanHeader.getPolarity(), true);
		if ( scanHeader.getScanType() != null || !scanHeader.getScanType().isEmpty() )
			t_elemScan.setAttribute("scanType", scanHeader.getScanType(), true);
		if ( scanHeader.getCentroided() != -1 )
			t_elemScan.setAttribute("centroided", Integer.toString(scanHeader.getCentroided()), true);
		if ( scanHeader.getDeisotoped() != -1 )
			t_elemScan.setAttribute("deisotoped", Integer.toString(scanHeader.getDeisotoped()), true);
		if ( scanHeader.getChargeDeconvoluted() != -1 )
			t_elemScan.setAttribute("chargeDeconvoluted", Integer.toString(scanHeader.getChargeDeconvoluted()), true);
		if ( scanHeader.getRetentionTime() != null || !scanHeader.getRetentionTime().isEmpty() )
			t_elemScan.setAttribute("retentionTime", scanHeader.getRetentionTime(), true);
		if ( scanHeader.getIonisationEnergy() != -1f )
			t_elemScan.setAttribute("ionizationEnergy", Float.toString(scanHeader.getIonisationEnergy()), true);
		if ( scanHeader.getCollisionEnergy() != -1f )
			t_elemScan.setAttribute("collisionEnergy", Float.toString(scanHeader.getCollisionEnergy()), true);

		if ( scanHeader.getStartMz() != -1f )
			t_elemScan.setAttribute("startMz", Float.toString(scanHeader.getStartMz()), true);
		if ( scanHeader.getEndMz() != -1f )
			t_elemScan.setAttribute("endMz", Float.toString(scanHeader.getEndMz()), true);
		if ( scanHeader.getLowMz() != -1f )
			t_elemScan.setAttribute("lowMz", Float.toString(scanHeader.getLowMz()), true);
		if ( scanHeader.getHighMz() != -1f )
			t_elemScan.setAttribute("highMz", Float.toString(scanHeader.getHighMz()), true);
		if ( scanHeader.getBasePeakMz() != -1f )
			t_elemScan.setAttribute("basePeakMz", Float.toString(scanHeader.getBasePeakMz()), true);
		if ( scanHeader.getBasePeakIntensity() != -1f )
			t_elemScan.setAttribute("basePeakIntensity", Float.toString(scanHeader.getBasePeakIntensity()), true);
		if ( scanHeader.getTotIonCurrent() != -1f )
			t_elemScan.setAttribute("totIonCurrent", Float.toString(scanHeader.getTotIonCurrent()));

		// precursorMz
		if ( scanHeader.getPrecursorIntensity() != -1f ) {
			Element t_elemPrecuror = new Element("precursorMz");

			// Do not set if precursor scan is not exist
			int iScanNum = scanHeader.getPrecursorScanNum();
			if ( iScanNum != -1 && this.m_mapOldToNewScanNum.containsKey(iScanNum) ) {
				// Replace old scan number to new one
				iScanNum = this.m_mapOldToNewScanNum.get(iScanNum);
				t_elemPrecuror.setAttribute("precursorScanNum", Integer.toString(iScanNum));
			}

			t_elemPrecuror.setAttribute("precursorIntensity", Float.toString(scanHeader.getPrecursorIntensity()));

			if ( scanHeader.getPrecursorCharge() != -1 )
				t_elemPrecuror.setAttribute("precursorCharge", Integer.toString(scanHeader.getPrecursorCharge()));
			// TODO: possible charges
			if ( scanHeader.getActivationMethod() != null || !scanHeader.getActivationMethod().isEmpty())
				t_elemPrecuror.setAttribute("activationMethod", scanHeader.getActivationMethod());
			// TODO: window wideness
			
			t_elemPrecuror.setText(Double.toString(scanHeader.getPrecursorMz()));

			t_elemScan.addContent(t_elemPrecuror);
		}
		
		Element t_elementPeaks = new Element("peaks");

		// Do not compress
//		t_elementPeaks.setAttribute("compressionType", scanHeader.getCompressionType());
//		t_elementPeaks.setAttribute("compressedLen", Integer.toString(scanHeader.getCompressedLen()));
		t_elementPeaks.setAttribute("compressionType", "none", true);
		t_elementPeaks.setAttribute("compressedLen", "0", true);
		if ( scanHeader.getPrecision() != -1 )
			t_elementPeaks.setAttribute("precision", Integer.toString(scanHeader.getPrecision()), true);
		t_elementPeaks.setAttribute("byteOrder", scanHeader.getByteOrder(), true);
		t_elementPeaks.setAttribute("contentType", scanHeader.getContentType());

		// Create Base64 string
		t_elementPeaks.setText(createBase64String(scan.getMassIntensityList()));
		
		// add the precursor and peak tag to the scan tag
		t_elemScan.addContent(t_elementPeaks);

		String content = t_elemScan.toStringWithIndent(4);
//		content = addIndentToAttributes(content, new String[] {"precursorMz"});
		this.m_iCurrentOffset += content.length();
		return content;
	}
	
	private String createBase64String(double[][] a_peaklist) {
		int size = a_peaklist[0].length;
		float[][] t_spectra = new float[2][size];
		for (int i = 0; i < size; i++) {
			Double mzDouble = a_peaklist[0][i];
			Double intensityDouble = a_peaklist[1][i];
			
			t_spectra[0][i] = mzDouble.floatValue();
			t_spectra[1][i] = intensityDouble.floatValue();
		}

		String encode64 = Base64SpectraUtil.encodeBase64(t_spectra);
		return encode64;
	}
}
