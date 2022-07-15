package org.grits.toolbox.tools.spectrafiltering;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.grits.toolbox.tools.spectrafiltering.dialog.AveragingSpectraDialog;
import org.grits.toolbox.tools.spectrafiltering.dialog.process.ProgressReporterDialog;
import org.grits.toolbox.tools.spectrafiltering.process.SpectraAverageThread;

public class TestAveragingSpectra {

	public static void main (String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);

		// Open UI to set conditions for filtering scans
		AveragingSpectraDialog dlgFilter = new AveragingSpectraDialog(shell);
		
		if (dlgFilter.open() == Window.OK) {
			ProgressReporterDialog dlgProgress = new ProgressReporterDialog(shell);
			SpectraAverageThread thread = new SpectraAverageThread(dlgFilter.getFilter());
			dlgProgress.setWorker(thread);
			dlgProgress.open();
		}
		shell.dispose();
		display.dispose();
	}

}
