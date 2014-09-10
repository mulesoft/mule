/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.devkit.processor;

import static org.junit.Assert.assertSame;

import org.mule.api.MuleEvent;

import org.junit.Before;

public class ListeningMessageProcessorWithoutListener extends ListeningMessageProcessorWithListenerTest
{

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        this.processor.setListener(null);
    }

    @Override
    public void processEvent() throws Exception
    {
        MuleEvent event = this.getMuleEvent();
        assertSame(event, this.processor.processEvent(event));
    }
}
