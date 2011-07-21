/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp.reliability;

import org.mule.tck.probe.Probe;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;


/**
 * Verify that no inbound messages are lost when exceptions occur.
 * The message must either make it all the way to the SEDA queue (in the case of
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 *
 * In the case of FTP, this will cause the postProcess() method to not be executed
 * and therefore the source file will not be deleted.
 */
public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{            
            {ConfigVariant.FLOW, "reliability/inbound-message-loss-flow.xml"}
        });
    }      
    
    public InboundMessageLossFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);     
    }
    
    @Override
    @Test
    public void testTransformerException() throws Exception
    {
        createFileOnFtpServer("transformerException/test1");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                // Note that this behavior is different from services because the exception occurs before
                // the SEDA queue for services.
                return !fileExists("transformerException/test1");
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
        createFileOnFtpServer("routerException/test1");
        prober.check(new Probe()
        {
            @Override
            public boolean isSatisfied()
            {
                // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
                // perspective, the message has been delivered successfully.
                // Note that this behavior is different from services because the exception occurs before
                // the SEDA queue for services.
                return !fileExists("routerException/test1");
            }

            @Override
            public String describeFailure()
            {
                return "File should be gone";
            }
        });
    }
}
