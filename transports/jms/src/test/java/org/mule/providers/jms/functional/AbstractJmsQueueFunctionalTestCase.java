/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.functional;

import org.mule.tck.functional.CountdownCallback;
import org.mule.tck.functional.FunctionalTestComponent;

public abstract class AbstractJmsQueueFunctionalTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "jms-functional-test.xml";
    }

    public void testSend() throws Exception
    {
        FunctionalTestComponent ftc = lookupTestComponent();

        CountdownCallback callback = new CountdownCallback(1);
        ftc.setEventCallback(callback);

        send(TEST_MESSAGE, "in");

        assertTrue(callback.await(LOCK_TIMEOUT));
        assertEquals(TEST_MESSAGE, ftc.getLastReceivedMessage());
        
        assertEquals(TEST_MESSAGE_RESPONSE, receiveTextMessage("out"));
    }

    // TODO This test fails due to concurrency issues.
//    public void testMultipleSend() throws Exception
//    {
//        RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);
//      
//        FunctionalTestComponent ftc = lookupTestComponent();
//
//        CountdownCallback callback = new CountdownCallback(3);
//        ftc.setEventCallback(callback);
//
//        send(TEST_MESSAGE, "in");
//        send(TEST_MESSAGE, "in");
//        send(TEST_MESSAGE, "in");
//
//        assertTrue(callback.await(LOCK_TIMEOUT));
//        //assertEquals(3, ftc.getReceivedMessages());
//        assertEquals(TEST_MESSAGE, ftc.getReceivedMessage(1));
//        assertEquals(TEST_MESSAGE, ftc.getReceivedMessage(2));
//        assertEquals(TEST_MESSAGE, ftc.getReceivedMessage(3));
//        assertNull(ftc.getReceivedMessage(4));
//
//        assertEquals(TEST_MESSAGE_RESPONSE, receiveTextMessage("out"));
//        assertEquals(TEST_MESSAGE_RESPONSE, receiveTextMessage("out"));
//        assertEquals(TEST_MESSAGE_RESPONSE, receiveTextMessage("out"));
//    }

    public void testSendWithReplyTo() throws Exception
    {
        FunctionalTestComponent ftc = lookupTestComponent();

        CountdownCallback callback = new CountdownCallback(1);
        ftc.setEventCallback(callback);

        send(TEST_MESSAGE, "in", false, getAcknowledgementMode(), "replyto");

        assertTrue(callback.await(LOCK_TIMEOUT));
        assertEquals(TEST_MESSAGE, ftc.getLastReceivedMessage());

        assertEquals(TEST_MESSAGE_RESPONSE, receiveTextMessage("out"));
    }

    public boolean useTopics()
    {
        return false;
    }

    protected FunctionalTestComponent lookupTestComponent()
    {
        try
        {
            Object ftc;
            if (useTopics())
            {
                ftc = getPojoServiceForComponent("topicComponent");
            }
            else
            {
                ftc = getPojoServiceForComponent("queueComponent");
            }
            assertTrue("FunctionalTestComponent expected", ftc instanceof FunctionalTestComponent);
            return (FunctionalTestComponent) ftc;
        } 
        catch (Exception e)
        {
            logger.error(e);
            return null;
        }
    }
}

