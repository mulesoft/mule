/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.source;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.source.MessageSource;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.SensingNullMessageProcessor;

public class StartablePatternAwareCompositeMessageSourceTestCase extends AbstractMuleTestCase
{
    SensingNullMessageProcessor listener;
    SensingNullMessageProcessor listener2;
    StartablePatternAwareCompositeMessageSource sourceAgregator;
    MuleEvent testEvent;
    NullMessageSource source;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        listener = getSensingNullMessageProcessor();
        listener2 = getSensingNullMessageProcessor();
        sourceAgregator = new StartablePatternAwareCompositeMessageSource();
        testEvent = getTestEvent(TEST_MESSAGE);
        source = new NullMessageSource(testEvent);
    }

    public void testAddSourceStopped() throws MuleException
    {
        sourceAgregator.setListener(listener);
        sourceAgregator.addSource(source);

        source.triggerSource();
        assertNull(listener.event);

        source.start();
        source.triggerSource();
        assertNull(listener.event);

        sourceAgregator.start();
        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testAddSourceStarted() throws MuleException
    {
        sourceAgregator.setListener(listener);
        sourceAgregator.start();

        sourceAgregator.addSource(source);

        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testRemoveSource() throws MuleException
    {
        sourceAgregator.setListener(listener);
        sourceAgregator.addSource(source);
        sourceAgregator.start();

        source.triggerSource();
        assertEquals(testEvent, listener.event);
        listener.clear();

        sourceAgregator.removeSource(source);
        source.triggerSource();
        assertNull(listener.event);
    }

    public void testSetListenerStarted() throws MuleException
    {
        sourceAgregator.addSource(source);
        sourceAgregator.setListener(listener);
        sourceAgregator.start();

        source.triggerSource();
        assertEquals(testEvent, listener.event);

        listener.clear();
        sourceAgregator.setListener(listener2);

        source.triggerSource();
        assertNull(listener.event);
        assertEquals(testEvent, listener2.event);
    }

    public void testStart() throws MuleException
    {
        sourceAgregator.setListener(listener);
        sourceAgregator.addSource(source);

        source.triggerSource();
        assertNull(listener.event);

        sourceAgregator.start();
        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testStartNoListener() throws MuleException
    {
        sourceAgregator.addSource(source);
        try
        {
            sourceAgregator.start();
            fail("Exception excepted");
        }
        catch (Exception e)
        {
        }

    }

    public void testStop() throws MuleException
    {
        sourceAgregator.setListener(listener);
        sourceAgregator.addSource(source);
        sourceAgregator.start();

        sourceAgregator.stop();
        source.triggerSource();
        assertNull(listener.event);
    }

    class NullMessageSource implements MessageSource, Startable, Stoppable
    {
        MuleEvent event;
        MessageProcessor listener;
        boolean started = false;

        public NullMessageSource(MuleEvent event)
        {
            this.event = event;
        }

        public void setListener(MessageProcessor listener)
        {
            this.listener = listener;
        }

        public void triggerSource() throws MuleException
        {
            if (started && listener != null)
            {
                listener.process(event);
            }
        }

        public void start() throws MuleException
        {
            started = true;
        }

        public void stop() throws MuleException
        {
            started = false;
        }

    }

}
