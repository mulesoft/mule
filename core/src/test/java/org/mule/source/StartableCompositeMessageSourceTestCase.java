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
import org.mule.util.ObjectUtils;

public class StartableCompositeMessageSourceTestCase extends AbstractMuleTestCase
{
    protected SensingNullMessageProcessor listener;
    protected SensingNullMessageProcessor listener2;
    protected StartableCompositeMessageSource compositeSource;
    protected MuleEvent testEvent;
    protected NullMessageSource source;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        listener = getSensingNullMessageProcessor();
        listener2 = getSensingNullMessageProcessor();
        compositeSource = getCompositeSource();
        testEvent = getTestEvent(TEST_MESSAGE);
        source = new NullMessageSource(testEvent);
    }
    
    protected StartableCompositeMessageSource getCompositeSource()
    {
        return new StartableCompositeMessageSource();
    }

    public void testAddSourceStopped() throws MuleException
    {
        compositeSource.setListener(listener);
        compositeSource.addSource(source);

        source.triggerSource();
        assertNull(listener.event);

        source.start();
        source.triggerSource();
        assertNull(listener.event);

        compositeSource.start();
        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testAddSourceStarted() throws MuleException
    {
        compositeSource.setListener(listener);
        compositeSource.start();

        compositeSource.addSource(source);

        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testRemoveSource() throws MuleException
    {
        compositeSource.setListener(listener);
        compositeSource.addSource(source);
        compositeSource.start();

        source.triggerSource();
        assertEquals(testEvent, listener.event);
        listener.clear();

        compositeSource.removeSource(source);
        source.triggerSource();
        assertNull(listener.event);
    }

    public void testSetListenerStarted() throws MuleException
    {
        compositeSource.addSource(source);
        compositeSource.setListener(listener);
        compositeSource.start();

        source.triggerSource();
        assertEquals(testEvent, listener.event);

        listener.clear();
        compositeSource.setListener(listener2);

        source.triggerSource();
        assertNull(listener.event);
        assertEquals(testEvent, listener2.event);
    }

    public void testStart() throws MuleException
    {
        compositeSource.setListener(listener);
        compositeSource.addSource(source);

        source.triggerSource();
        assertNull(listener.event);

        compositeSource.start();
        source.triggerSource();
        assertEquals(testEvent, listener.event);
    }

    public void testStartNoListener() throws MuleException
    {
        compositeSource.addSource(source);
        try
        {
            compositeSource.start();
            fail("Exception excepted");
        }
        catch (Exception e)
        {
        }

    }

    public void testStop() throws MuleException
    {
        compositeSource.setListener(listener);
        compositeSource.addSource(source);
        compositeSource.start();

        compositeSource.stop();
        source.triggerSource();
        assertNull(listener.event);
    }

    protected class NullMessageSource implements MessageSource, Startable, Stoppable
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

        @Override
        public String toString()
        {
            return ObjectUtils.toString(this);
        }
    }
}
