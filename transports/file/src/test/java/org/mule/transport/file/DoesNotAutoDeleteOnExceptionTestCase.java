/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file;

import org.mule.api.construct.FlowConstruct;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class DoesNotAutoDeleteOnExceptionTestCase extends FunctionalTestCase
{

    public static final String TEST_FOLDER = ".mule/testData";

    private Prober prober;
    private File target;

    public DoesNotAutoDeleteOnExceptionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigResources()
    {
        return "does-not-auto-delete-on-exception-config.xml";
    }

    @Before
    public void setUp() throws IOException
    {
        prober = new PollingProber(10000, 100);

        File testFolder = new File(TEST_FOLDER);
        testFolder.mkdirs();
        prober.check(new FileExists(testFolder));

        target = File.createTempFile("mule-file-test-", ".txt", testFolder);
        target.deleteOnExit();
        prober.check(new FileExists(target));
    }

    @Test
    public void testDoesNotAutoDeleteFileOnException() throws Exception
    {
        // Starts file endpoint polling
        muleContext.start();

        // Exception strategy should be stopped after processing the file
        final FlowConstruct fileFlow = muleContext.getRegistry().lookupFlowConstruct("fileTest");
        prober.check(new FlowStopped(fileFlow));

        // Checks that the source file was not deleted after the exception processing
        prober.check(new FileExists(target));
    }

    private static class FileExists implements Probe
    {

        private final File target;

        public FileExists(File target)
        {
            this.target = target;
        }

        public boolean isSatisfied()
        {
            return target.exists();
        }

        public String describeFailure()
        {
            return "File does not exists";
        }
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
