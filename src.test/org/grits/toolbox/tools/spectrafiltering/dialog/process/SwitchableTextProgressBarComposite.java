package org.grits.toolbox.tools.spectrafiltering.dialog.process;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;

public class SwitchableTextProgressBarComposite extends Composite {

	private TextProgressBar m_progressBar;
	private boolean m_bIsIndeterminate;
	private int m_iBarStyle;

	public SwitchableTextProgressBarComposite(Composite parent, int style) {
		super(parent, SWT.NONE);

		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		this.setLayout(layout);

		this.m_bIsIndeterminate = ((style & SWT.INDETERMINATE) != 0);
		createProgressBar();
	}

	/**
	 * Sets the minimum value of the progress bar.
	 * @param value
	 * @see TextProgressBar#setMinimum(int)
	 */
	public void setMinimum(int value) {
		if ( !this.m_bIsIndeterminate )
			this.m_progressBar.setMinimum(value);
	}

	/**
	 * Sets the maximum value of the progress bar. If negative value is set,
	 * the progress bar will be switched to indeterminate mode automatically,
	 * and vice versa.
	 * @param value the maximum value of the progress bar
	 * @see TextProgressBar#setMaximum(int)
	 */
	public void setMaximum(int value) {
		if ( this.m_bIsIndeterminate != (value < 0) ) {
			this.m_bIsIndeterminate = (value < 0);
			createProgressBar();
		}
		if ( !this.m_bIsIndeterminate )
			this.m_progressBar.setMaximum(value);
	}

	/**
	 * 
	 * @param text
	 * @see TextProgressBar#setText(String)
	 */
	public void setText(String text) {
		this.m_progressBar.setText(text);
	}

	/**
	 * 
	 * @param value
	 * @see TextProgressBar#setSelection(int)
	 */
	public void setSelection(int value) {
		if ( !this.m_bIsIndeterminate )
			this.m_progressBar.setSelection(value);
	}

	private void createProgressBar() {
		if ( this.m_progressBar != null && !this.m_progressBar.isDisposed() ) {
			this.m_progressBar.dispose();
			this.m_iBarStyle &= ~SWT.INDETERMINATE;
			this.m_iBarStyle &= ~SWT.SMOOTH;
		}
		this.m_progressBar = new TextProgressBar(this,
				this.m_iBarStyle |
				(this.m_bIsIndeterminate? SWT.INDETERMINATE : SWT.SMOOTH)
			);
		this.m_progressBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		this.layout();
	}

	
}
