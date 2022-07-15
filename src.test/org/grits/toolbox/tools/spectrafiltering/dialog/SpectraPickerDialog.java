package org.grits.toolbox.tools.spectrafiltering.dialog;

import java.io.File;

import org.apache.log4j.Logger;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.grits.toolbox.tools.spectrafiltering.om.SpectraPickerSettings;

public class SpectraPickerDialog extends TitleAreaDialog {

	private Text m_txtLocation;
	private Text m_txtMzValue;
	private Text m_txtAccuracyValue;
	private Text m_txtCutOffValue;
	private Text m_txtSaveTo;

	private Boolean m_bIsPPM;
	private Boolean m_bPercentageCutOff;
	private String browseLocation;
	private Double mzValue;
	private Double accuracyValue;
	private Double cutOfValue;
	private String saveLocation;

	private SpectraPickerSettings spectrumFilter = new SpectraPickerSettings();

	public static final String EXTENSION = ".mzXML";
	private static final String DEFAULT_FILENAME = "filteredMass" + EXTENSION;
	private static final Logger logger = Logger.getLogger(SpectraPickerDialog.class);

	public SpectraPickerDialog(Shell a_parentShell) {
		super(a_parentShell);
	}

	@Override
	public void create() {
		super.create();
		setTitle("Spectra Filtering option");
		setMessage("This is a Dialog box to filter required spectra", IMessageProvider.INFORMATION);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
		Composite container = new Composite(area, SWT.NONE);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(5, false);
		layout.horizontalSpacing = 10;
		layout.verticalSpacing = 10;
		area.setSize(100, 100);
		container.setLayout(layout);
		createSpectraFilter(container);
		return area;
	}

	private Button addAButton(Composite container, String label, int horizontalAlignment) {
		Button btn = new Button(container, SWT.PUSH);
		btn.setText(label);
		GridData data = new GridData(horizontalAlignment);
		data.horizontalSpan = 1;
		btn.setLayoutData(data);
		return btn;
	}

	private void createSpectraFilter(Composite container) {
		Label lbl;

		// Browse
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Select the location to load the mzXML file from");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

		m_txtLocation = new Text(container, SWT.BORDER);
		m_txtLocation.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		m_txtLocation.setEnabled(false);

		Button btnBrowse = new Button(container, GridData.HORIZONTAL_ALIGN_END);
		btnBrowse.setText("Browse ...");
		btnBrowse.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent a_event) {
				try {
					FileDialog t_dialogSave = new FileDialog(container.getShell());
					t_dialogSave.setFilterNames(new String[] { "mzXML files (.mzXML)", "All files" });
					t_dialogSave.setFilterExtensions(new String[] { "*.mzXML", "*.*" });
					String t_file = t_dialogSave.open();
					if (t_file != null) {
						m_txtLocation.setText(t_file);
					}
				} catch (Exception e) {
					logger.fatal("Unable to select a file.", e);
				}
			}
		});
		btnBrowse.setFocus();
		btnBrowse.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		// m/z value
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Enter m/z value");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		m_txtMzValue = new Text(container, SWT.BORDER);
		m_txtMzValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		/// Empty label
		lbl = new Label(container, SWT.NONE);
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 2, 1));

		// Accuracy
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Enter accuracy");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		m_txtAccuracyValue = new Text(container, SWT.BORDER);
		m_txtAccuracyValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		final Combo c = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		String items[] = { "Dalton", "PPM" };
		c.setItems(items);
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));
		c.select(1);
		m_bIsPPM = true;
		c.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (c.getText().equals("Dalton")) {
					m_bIsPPM = false;
				} else if (c.getText().equals("PPM")) {
					m_bIsPPM = true;
				}
			}
		});

		/// Empty label
		new Label(container, SWT.NONE);

		// Cut off
		lbl = new Label(container, SWT.NONE);
		lbl.setText("Enter cut off value");
		m_txtCutOffValue = new Text(container, SWT.BORDER);
		m_txtCutOffValue.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));

		final Combo cutOff = new Combo(container, SWT.DROP_DOWN | SWT.READ_ONLY);
		String cutOffItems[] = { "Absolute", "Percentage" };
		cutOff.setItems(cutOffItems);
		cutOff.select(1);
		cutOff.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		m_bPercentageCutOff = true;
		cutOff.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (cutOff.getText().equals("Absolute")) {
					m_bPercentageCutOff = false;
				} else if (cutOff.getText().equals("Percentage")) {
					m_bPercentageCutOff = true;
				}
			}
		});

		/// Empty label
		new Label(container, SWT.NONE);

		// Save location
		lbl = new Label(container, SWT.DOWN);
		lbl.setText("Select the location to save the mzXML file to");
		lbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 5, 1));

		m_txtSaveTo = new Text(container, SWT.BORDER);
		m_txtSaveTo.setText(System.getProperty("user.home") + File.separator + DEFAULT_FILENAME);
		m_txtSaveTo.setEnabled(false);
		m_txtSaveTo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 4, 1));
		Button saveButton = addAButton(container, "Save to ...", GridData.HORIZONTAL_ALIGN_END);
		saveButton.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setErrorMessage(null);
				FileDialog fileDialog = new FileDialog(getShell(), SWT.SAVE);
				fileDialog.setText("Select File");
				fileDialog.setFilterExtensions(new String[] { "*"+EXTENSION });
				fileDialog.setFilterNames(new String[] { "Spectra Filtering (" + EXTENSION + ")" });
				fileDialog.setFileName(DEFAULT_FILENAME);
				fileDialog.setOverwrite(true);
				String selected = fileDialog.open();
				if (selected != null) {
					m_txtSaveTo.setText(selected);
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		saveButton.setFocus();
		saveButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		container.setLayoutData(new GridData(GridData.FILL_BOTH));
	}

	@Override
	protected void okPressed() {
		browseLocation = m_txtLocation.getText();
		if (browseLocation.equals("")) {
			setErrorMessage("Select the location to load mzXML file");
			return;
		}
		try {
			mzValue = Double.parseDouble(m_txtMzValue.getText());
		} catch (Exception e) {
			setErrorMessage("mzValue Value should be Double");
			return;
		}
		try {
			accuracyValue = Double.parseDouble(m_txtAccuracyValue.getText());
		} catch (Exception e) {
			setErrorMessage("accuracyValue Value should be Double");
			return;
		}
		try {
			cutOfValue = Double.parseDouble(m_txtCutOffValue.getText());
		} catch (Exception e) {
			setErrorMessage("cutOfValue Value should be Double");
			return;
		}

		saveLocation = m_txtSaveTo.getText();
		if (saveLocation.equals("")) {
			setErrorMessage("Select the location to save mzXML file");
			return;
		}

		spectrumFilter = new SpectraPickerSettings();
		spectrumFilter.setOpenFrom(browseLocation);
		spectrumFilter.setMzValue(mzValue);
		spectrumFilter.setAccuracy(accuracyValue);
		spectrumFilter.setCutOffValue(cutOfValue);
		spectrumFilter.setSaveLocation(saveLocation);
		if (m_bIsPPM == true) {
			spectrumFilter.setPPM(true);
		} else {
			spectrumFilter.setPPM(false);
		}
		if (m_bPercentageCutOff == true) {
			spectrumFilter.setPercentage(true);
		} else {
			spectrumFilter.setPercentage(false);
		}
		super.okPressed();
	}

	public SpectraPickerSettings getFilter() {
		return spectrumFilter;
	}
}
