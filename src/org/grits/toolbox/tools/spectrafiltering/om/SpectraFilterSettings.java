package org.grits.toolbox.tools.spectrafiltering.om;

public class SpectraFilterSettings {
	
	private String	m_strOpenFrom	= null;
	private String	m_strSaveTo		= null;
	private Double	m_dAccuracy		= null;
	private Double	m_dCutOff		= null;
	private Boolean	m_bIsPPM;
	private Boolean	m_bIsPercentage;
	
	public String getOpenFrom() {
		return m_strOpenFrom;
	}
	
	public void setOpenFrom(String a_strOpen) {
		this.m_strOpenFrom = a_strOpen;
	}
	
	public Double getAccuracy() {
		return m_dAccuracy;
	}
	
	public void setAccuracy(Double a_accuracy) {
		this.m_dAccuracy = a_accuracy;
	}
	
	public Double getCutOffValue() {
		return m_dCutOff;
	}
	
	public void setCutOffValue(Double a_cutOfValue) {
		this.m_dCutOff = a_cutOfValue;
	}
	
	public String getSaveLocation() {
		return m_strSaveTo;
	}
	
	public void setSaveLocation(String a_saveLocation) {
		this.m_strSaveTo = a_saveLocation;
	}
	
	public Boolean getPPM() {
		return m_bIsPPM;
	}
	
	public void setPPM(Boolean ppm) {
		this.m_bIsPPM = ppm;
	}
	
	public Boolean getPercentage() {
		return m_bIsPercentage;
	}
	
	public void setPercentage(Boolean prcnt) {
		this.m_bIsPercentage = prcnt;
	}
	
}

