/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.reliability;

import static org.mule.transport.file.FileTestUtils.createDataFile;
import static org.mule.transport.file.FileTestUtils.createFolder;

import org.mule.api.MuleEventContext;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.probe.Probe;
import org.mule.util.concurrent.Latch;

import java.io.File;

import org.junit.Test;


public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    public InboundMessageLossFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected String getConfigFile()
    {
        return "reliability/inbound-message-loss-flow.xml";
    }

    @Override
    @Test
    public void testTransformerException() throws Exception
    {
        tmpDir = createFolder(getFileInsideWorkingDirectory("transformerException").getAbsolutePath());
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                // Note that this behavior is different from services because the exception occurs before
                // the SEDA queue for services.
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Override
    @Test
    public void testRouterException() throws Exception
    {
        tmpDir = createFolder(getFileInsideWorkingDirectory("routerException").getAbsolutePath());
        final File file = createDataFile(tmpDir, "test1.txt");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                // Note that this behavior is different from services because the exception occurs before
                // the SEDA queue for services.
                return !file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }

    @Test
    public void testFlowRefException() throws Exception
    {
        if (variant.equals(ConfigVariant.SERVICE))
        {
            return;
        }
        final Latch exceptionThrownLatch = new Latch();
        tmpDir = createFolder(getFileInsideWorkingDirectory("flowRefException").getAbsolutePath());
        final File file = createDataFile(tmpDir, "test1.txt");
        FunctionalTestComponent ftc = getFunctionalTestComponent("failingFlow");
        ftc.setEventCallback(new EventCallback()
        {
            @Override
            public void eventReceived(MuleEventContext context, Object component) throws Exception
            {
                exceptionThrownLatch.release();
                throw new RuntimeException();
            }
        });
        Flow flow = (Flow)getFlowConstruct("FlowRefException");
        flow.stop();
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Delivery failed so message should have been restored at the source
                return file.exists();
            }

            @Override
            public String describeFailure()
            {
                return "File should have been restored";
            }
        });
    }

}
