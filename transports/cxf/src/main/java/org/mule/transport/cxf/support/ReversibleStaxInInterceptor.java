/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf.support;

import org.mule.module.xml.stax.ReversibleXMLStreamReader;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Resets the ReversibleXMLStreamReader so the person receiving it can start back
 * at the beginning of the stream.
 */
public class ReversibleStaxInInterceptor extends AbstractPhaseInterceptor<Message>
{

    public ReversibleStaxInInterceptor()
    {
        super(Phase.POST_STREAM);
        getAfter().add(StaxInInterceptor.class.getName());
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


