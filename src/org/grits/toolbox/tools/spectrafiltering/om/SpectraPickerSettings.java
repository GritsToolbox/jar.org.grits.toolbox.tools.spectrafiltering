package org.grits.toolbox.tools.spectrafiltering.om;

public class SpectraPickerSettings extends SpectraFilterSettings {
	
	private Double	m_dMzValue	= null;

	public Double getMzValue() {
		return m_dMzValue;
	}
	
	public void setMzValue(Double a_mzValue) {
		this.m_dMzValue = a_mzValue;
	}
}

