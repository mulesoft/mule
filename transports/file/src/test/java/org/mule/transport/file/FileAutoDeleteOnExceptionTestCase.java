/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file;

import static org.junit.Assert.assertFalse;

import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.tck.probe.file.FileExists;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class FileAutoDeleteOnExceptionTestCase extends FunctionalTestCase
{
    public String testFolder1;
    public String testFolder2;

    private Prober prober;

    public FileAutoDeleteOnExceptionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "file-auto-delete-on-exception-config.xml";
    }

    @Before
    public void setUp() throws IOException
    {
        prober = new PollingProber(10000, 100);
        testFolder1 = getFileInsideWorkingDirectory("testData1").getAbsolutePath();
        testFolder2 = getFileInsideWorkingDirectory("testData2").getAbsolutePath();
    }

    private File createTestFile(String folder) throws IOException
    {
        File testFolder = new File(folder);
        testFolder.mkdirs();
        prober.check(new FileExists(testFolder));

        File target = File.createTempFile("mule-file-test-", ".txt", testFolder);
        target.deleteOnExit();
        prober.check(new FileExists(target));
        return target;
    }

    @Test
    public void testDoesNotAutoDeleteFileOnException() throws Exception
    {
        File target = createTestFile(testFolder1);
        // Starts file endpoint polling
        muleContext.start();

        // Exception strategy should be stopped after processing the file
        final FlowConstruct fileFlow = muleContext.getRegistry().lookupFlowConstruct("fileTest");
        prober.check(new FlowStopped(fileFlow));

        // Checks that the source file was not deleted after the exception processing
        prober.check(new FileExists(target));
    }

    @Test
    public void testAutoDeletesFileOnExceptionIfFileWasTransformed() throws Exception
    {
        File target = createTestFile(testFolder2);

        // Starts file endpoint polling
        muleContext.start();

        // Exception strategy should be stopped after processing the file
        final FlowConstruct fileFlow = muleContext.getRegistry().lookupFlowConstruct("fileTestWithTransformation");
        prober.check(new FlowStopped(fileFlow));

        // Checks that the source file was deleted after the exception processing
        assertFalse(target.exists());
    }

    private static class FlowStopped implements Probe
    {

        private final FlowConstruct fileFlow;

        public FlowStopped(FlowConstruct fileFlow)
        {
            this.fileFlow = fileFlow;
        }

        public boolean isSatisfied()
        {
            return fileFlow.getLifecycleState().isStopped();
        }

        public String describeFailure()
        {
            return "Flow was not stopped after processing the exception";
        }
    }
}
