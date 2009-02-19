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

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.transport.NullPayload;

import java.util.List;

import javanet.staxutils.ContentHandlerToXMLStreamWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import org.apache.cxf.databinding.stax.XMLStreamWriterCallback;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;
import org.xml.sax.SAXException;

public class OutputPayloadInterceptor extends AbstractOutDatabindingInterceptor
{

    public OutputPayloadInterceptor()
    {
        super(Phase.PRE_LOGICAL);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) throws Fault
    {
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.size() == 0)
        {
            return;
        }

        List<Object> originalParts = (List<Object>) objs.clone();

        objs.clear();

        for (Object o : originalParts)
        {
            if (o instanceof MuleMessage) 
            {
                try
                {
                    MuleMessage muleMsg = (MuleMessage) o;
                    final Object payload = muleMsg.getPayload();
                    
                    if (payload instanceof DelayedResult)
                    {
                        o = getDelayedResultCallback((DelayedResult)payload);
                    }
                    else if (payload instanceof XMLStreamReader)
                    {
                        o = new XMLStreamWriterCallback()
                        {
                            public void write(XMLStreamWriter writer) throws Fault, XMLStreamException
                            {
                                XMLStreamReader xsr = (XMLStreamReader)payload;
                                StaxUtils.copy(xsr, writer);      
                                writer.flush();
                                xsr.close();
                            }
                        };
                    } 
                    else if (payload instanceof NullPayload)
                    {
                        break;
                    }
                    else
                    {
                        o = muleMsg.getPayload(XMLStreamReader.class);
                    }
    
                    objs.add(o);
                }
                catch (TransformerException e)
                {
                    throw new Fault(e);
                } 
            }
            else
            {
                // it's probably a null object
                objs.add(o);
            }
            
        }
    }

    protected Object getDelayedResultCallback(final DelayedResult r)
    {
        return new XMLStreamWriterCallback()
        {
            public void write(XMLStreamWriter writer) throws Fault, XMLStreamException
            {
                ContentHandlerToXMLStreamWriter handler = new ContentHandlerToXMLStreamWriter(writer) {

                    @Override
                    public void endDocument() throws SAXException
                    {
                    }

                    @Override
                    public void processingInstruction(String target, String data) throws SAXException
                    {
                    }

                    @Override
                    public void startDocument() throws SAXException
                    {
                    }

                    @Override
                    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException
                    {
                    }
                    
                };
                
                try
                {
                    r.write(new SAXResult(handler));
                }
                catch (Exception e)
                {
                    throw new Fault(e);
                }
            }
        };
    }

}
