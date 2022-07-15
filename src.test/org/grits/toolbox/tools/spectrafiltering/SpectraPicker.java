package org.grits.toolbox.tools.spectrafiltering;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.tools.spectrafiltering.dialog.SpectraPickerDialog;
import org.grits.toolbox.tools.spectrafiltering.dialog.process.ProgressReporterDialog;
import org.grits.toolbox.tools.spectrafiltering.process.SpectraPickerThread;

public class SpectraPicker {
	
	public static void main (String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);

		// Open UI to set conditions for filtering scans
		SpectraPickerDialog dlgFilter = new SpectraPickerDialog(shell);
		
		if (dlgFilter.open() == Window.OK) {
			// OK was pressed now start processing the settings
			// creating a progress dialog and let the spectra filter thread
			// do the work
			ProgressReporterDialog dlgProgress = new ProgressReporterDialog(shell);
			SpectraPickerThread filter = new SpectraPickerThread(dlgFilter.getFilter());
			dlgProgress.setWorker(filter);
			dlgProgress.open();
		}
		shell.dispose();
		display.dispose();
	}

}
