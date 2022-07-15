package org.grits.toolbox.tools.spectrafiltering.dialog.process;

public abstract class ProgressDialogThread extends Thread
{
    protected IProgressReporter m_progressReporter = null;
    protected Boolean m_canceled = Boolean.FALSE;

    public void setDialog(IProgressReporter a_reporter)
    {
        this.m_progressReporter = a_reporter;
    }

    /**
     * When start() is called in Thread class, this class will be invoked!
     */
    @Override
    public void run()
    {
        try
        {
            boolean successful = this.threadStart();
            this.m_progressReporter.threadFinished(successful);
        }
        catch (Exception e)
        {
            this.m_progressReporter.endWithException(e);
        }
    }

    public abstract boolean threadStart() throws Exception;

    public void cancelWork()
    {
        this.m_canceled = Boolean.TRUE;
    }
}
