package org.grits.toolbox.tools.spectrafiltering.dialog.process;

public interface IProgressReporter
{

    public void threadFinished(boolean a_successful);

    public void endWithException(Exception a_e);

    public void setMax(int a_i);

    public void setProcessMessageLabel(String a_string);

    public void setDescriptionText(String a_string);

    public void updateProgresBar(String a_string);

}
