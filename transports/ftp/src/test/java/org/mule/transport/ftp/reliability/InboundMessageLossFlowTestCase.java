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


public class InboundMessageLossFlowTestCase extends InboundMessageLossTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss-flow.xml";
    }
    
    @Override
    public void testTransformerException() throws Exception
    {
        createFileOnFtpServer("transformerException/test1");
        Thread.sleep(DELAY);
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse(fileExists("transformerException/test1"));
    }
    
    @Override
    public void testRouterException() throws Exception
    {
        createFileOnFtpServer("routerException/test1");
        Thread.sleep(DELAY);
        // Exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        // Note that this behavior is different from services because the exception occurs before
        // the SEDA queue for services.
        assertFalse(fileExists("routerException/test1"));
    }
}
