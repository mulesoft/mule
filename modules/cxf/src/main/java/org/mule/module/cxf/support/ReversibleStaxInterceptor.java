/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;

/**
 * Creates a ReversibleXMLStreamReader to be able to track and replay events from the XMLStreamReader.
 */
public abstract class ReversibleStaxInterceptor extends AbstractPhaseInterceptor<Message>
{
    public ReversibleStaxInterceptor(String phase)
    {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault
    {
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);

        if (reader != null)
        {
            ReversibleXMLStreamReader reversible = new ReversibleXMLStreamReader(reader);
            reversible.setTracking(true);
            message.setContent(XMLStreamReader.class, reversible);
            message.setContent(ReversibleXMLStreamReader.class, reversible);
        }
    }

}
