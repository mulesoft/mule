/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management.mbeans;

import com.yourkit.api.Controller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class YourKitProfilerService implements YourKitProfilerServiceMBean
{
    /**
     * logger used by this class
     */
    protected transient Log logger = LogFactory.getLog(getClass());

    private final Controller controller;
    private boolean capturing = false;

    public YourKitProfilerService() throws Exception
    {
        controller = new Controller();
    }

    /** {@inheritDoc} */
    public String getHost()
    {
        return controller.getHost();
    }

    /** {@inheritDoc} */
    public int getPort()
    {
        return controller.getPort();
    }

    /** {@inheritDoc} */
    public String captureMemorySnapshot() throws Exception
    {
        return controller.captureMemorySnapshot();
    }

    /** {@inheritDoc} */
    public String captureSnapshot(long snapshotFlags) throws Exception
    {
        return controller.captureSnapshot(snapshotFlags);
    }

    /** {@inheritDoc} */
    public void startAllocationRecording(long mode) throws Exception
    {
        controller.startAllocationRecording(mode);
    }

    /** {@inheritDoc} */
    public void stopAllocationRecording() throws Exception
    {
        controller.stopAllocationRecording();
    }

    /** {@inheritDoc} */
    public void startCPUProfiling(long mode, String filters) throws Exception
    {
        controller.startCPUProfiling(mode, filters);
    }

    /** {@inheritDoc} */
    public void stopCPUProfiling() throws Exception
    {
        controller.stopCPUProfiling();
    }

    /** {@inheritDoc} */
    public void startMonitorProfiling() throws Exception
    {
        controller.startMonitorProfiling();
    }

    /** {@inheritDoc} */
    public void stopMonitorProfiling() throws Exception
    {
        controller.stopMonitorProfiling();
    }

    /** {@inheritDoc} */
    public void advanceGeneration(String description)
    {
        controller.advanceGeneration(description);
    }

    /** {@inheritDoc} */
    public long[] forceGC() throws Exception
    {
        return controller.forceGC();
    }

    /** {@inheritDoc} */
    public void startCapturingMemorySnapshotEverySeconds(final int seconds)
    {
        if (this.capturing)
        {
            return;
        }

        this.capturing = true;
        final Thread thread = new Thread(
                new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            while (capturing)
                            {
                                controller.captureMemorySnapshot();
                                Thread.sleep(seconds * 1000 /* millis in second */);
                            }
                        }
                        catch (Exception e)
                        {
                            logger.error("Failed to capture memory snapshot", e);
                        }
                    }
                }
        );
        thread.setDaemon(true); // let the application normally terminate
        thread.start();
    }

    /** {@inheritDoc} */
    public void stopCapturingMemorySnapshot()
    {
        this.capturing = false;
    }

    /** {@inheritDoc} */
    public long getStatus() throws java.lang.Exception
    {
        return controller.getStatus();
    }

}
