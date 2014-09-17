/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.file.reliability;

import org.mule.tck.probe.Probe;

import java.io.File;

import org.junit.Test;


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
    @Override
    public void testTransformerException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "transformerException");
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
    @Override
    public void testRouterException() throws Exception
    {
        tmpDir = createFolder(tempRootFolder.getRoot(), "routerException");
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
}
