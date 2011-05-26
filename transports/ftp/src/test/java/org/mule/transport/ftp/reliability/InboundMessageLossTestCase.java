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

import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.ftp.AbstractFtpServerTestCase;

public class InboundMessageLossTestCase extends AbstractFtpServerTestCase
{
    /** Delay (in ms) to wait for file to be processed */
    public static final int DELAY = 1000;
    
    @Override
    protected String getConfigResources()
    {
        return "reliability/inbound-message-loss.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        // Set SystemExceptionStrategy to redeliver messages (this can only be configured programatically for now)
        ((DefaultSystemExceptionStrategy) muleContext.getExceptionListener()).setRollbackTxFilter(new WildcardFilter("*"));

        // Create a separate source directory for each test case
        createFtpServerDir("noException");
        createFtpServerDir("transformerException");
        createFtpServerDir("routerException");
        createFtpServerDir("componentException");
    }

    public void testNoException() throws Exception
    {
        createFileOnFtpServer("noException/test1");
        Thread.sleep(DELAY);
        // Delivery was successful so message should be gone
        assertFalse(fileExists("noException/test1"));
    }
    
    public void testTransformerException() throws Exception
    {
        createFileOnFtpServer("transformerException/test1");
        Thread.sleep(DELAY);
        // Delivery failed so message should have been restored at the source
        assertTrue(fileExists("transformerException/test1"));
    }
    
    public void testRouterException() throws Exception
    {
        createFileOnFtpServer("routerException/test1");
        Thread.sleep(DELAY);
        // Delivery failed so message should have been restored at the source
        assertTrue(fileExists("routerException/test1"));
    }
    
    public void testComponentException() throws Exception
    {
        createFileOnFtpServer("componentException/test1");
        Thread.sleep(DELAY);
        // Component exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        assertFalse(fileExists("componentException/test1"));
    }    
}
