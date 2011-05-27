/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.file.reliability;

import org.mule.exception.DefaultSystemExceptionStrategy;
import org.mule.routing.filters.WildcardFilter;
import org.mule.transport.file.AbstractFileMoveDeleteTestCase;

import java.io.File;

/**
 * Verify that no inbound messages are lost when exceptions occur.  
 * The message must either make it all the way to the SEDA queue (in the case of 
 * an asynchronous inbound endpoint), or be restored/rolled back at the source.
 * 
 * In the case of the File transport, this will cause the file to be restored to 
 * its original location from the working directory.  Note that a 
 * workDirectory must be specified on the connector in order for this to succeed.  
 */
public class InboundMessageLossTestCase extends AbstractFileMoveDeleteTestCase
{
    /** Delay (in ms) to wait for file to be processed */
    public static final int DELAY = 3000;
    
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
    }

    public void testNoException() throws Exception
    {
        tmpDir = createFolder(".mule/noException");
        File file = createDataFile(tmpDir, "test1.txt");
        Thread.sleep(DELAY);
        // Delivery was successful so message should be gone
        assertFalse(file.exists());
    }
    
    public void testTransformerException() throws Exception
    {
        tmpDir = createFolder(".mule/transformerException");
        File file = createDataFile(tmpDir, "test1.txt");
        Thread.sleep(DELAY);
        // Delivery failed so message should have been restored at the source
        assertTrue(file.exists());
    }
    
    public void testRouterException() throws Exception
    {
        tmpDir = createFolder(".mule/routerException");
        File file = createDataFile(tmpDir, "test1.txt");
        Thread.sleep(DELAY);
        // Delivery failed so message should have been restored at the source
        assertTrue(file.exists());
    }
    
    public void testComponentException() throws Exception
    {
        tmpDir = createFolder(".mule/componentException");
        File file = createDataFile(tmpDir, "test1.txt");
        Thread.sleep(DELAY);
        // Component exception occurs after the SEDA queue for an asynchronous request, so from the client's
        // perspective, the message has been delivered successfully.
        assertFalse(file.exists());
    }        
}
