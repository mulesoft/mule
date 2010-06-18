/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformer;

import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;
import org.mule.endpoint.inbound.AbstractInboundMessageProcessorTestCase;
import org.mule.transformer.TransformerMessageProcessor;
import org.mule.transformer.simple.InboundAppendTransformer;

import org.junit.Test;

public class TransformerMessageProcessorTestCase extends AbstractInboundMessageProcessorTestCase
{

    @Test
    public void testProcess() throws Exception
    {
        MessageProcessor mp = new TransformerMessageProcessor(new InboundAppendTransformer());

        MuleEvent event = getTestInboundEvent(TEST_MESSAGE);

        assertEquals(TEST_MESSAGE + InboundAppendTransformer.APPEND_STRING, mp.process(event)
            .getMessageAsString());

    }

}
