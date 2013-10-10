/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.file.reliability;

import static org.mule.transport.file.FileTestUtils.createDataFile;
import static org.mule.transport.file.FileTestUtils.createFolder;
import org.mule.api.MuleEventContext;
import org.mule.construct.Flow;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.probe.Probe;

import java.io.File;

import org.junit.Test;

import org.mule.transport.file.FileTestUtils;
import org.mule.util.concurrent.Latch;


public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    public InboundMessageLossFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss-flow.xml";
    }

    @Test
    public void testTransformerException() throws Exception
    {
        tmpDir = createFolder(".mule/transformerException");
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
    public void testRouterException() throws Exception
    {
        tmpDir = createFolder(".mule/routerException");
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
        tmpDir = createFolder(".mule/flowRefException");
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
