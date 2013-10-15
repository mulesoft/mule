/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.module.xml.transformer.DelayedResult;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.sax.SAXResult;

import javanet.staxutils.ContentHandlerToXMLStreamWriter;
import org.apache.cxf.databinding.stax.XMLStreamWriterCallback;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
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
                    final Object payload = cleanUpPayload(muleMsg.getPayload());
                    
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
                        o = muleMsg.getPayload(DataTypeFactory.create(XMLStreamReader.class));
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

        // For the server side response, ensure that the body object is in the correct
        // location when running in proxy mode
        if (!isRequestor(message))
        {
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            if (bop != null)
            {
                ensurePartIndexMatchListIndex(objs, bop.getOutput().getMessageParts());
            }
        }
    }

    /**
     * Ensures that each part's content is in the right place in the content list.
     * <p/>
     * This is required because in some scenarios there are parts that were removed from
     * the part list. In that cases, the content list contains only the values for the
     * remaining parts, but the part's indexes could be wrong. This method fixes that
     * adding null values into the content list so the part's index matches the contentList
     * index. (Related to: MULE-5113.)
     */
    protected void ensurePartIndexMatchListIndex(MessageContentsList contentList, List<MessagePartInfo> parts)
    {

        // In some circumstances, parts is a {@link UnmodifiableList} instance, so a new copy
        // is required in order to sort its content.
        List<MessagePartInfo> sortedParts = new LinkedList<MessagePartInfo>();
        sortedParts.addAll(parts);
        sortPartsByIndex(sortedParts);

        int currentIndex = 0;

        for (MessagePartInfo part : sortedParts)
        {
            while (part.getIndex() > currentIndex)
            {
                contentList.add(currentIndex++, null);
            }

            // Skips the index for the current part because now is in the right place
            currentIndex = part.getIndex() + 1;
        }
    }

    private void sortPartsByIndex(List<MessagePartInfo> parts)
    {
        Collections.sort(parts, new Comparator<MessagePartInfo>()
        {

            public int compare(MessagePartInfo o1, MessagePartInfo o2)
            {
                if (o1.getIndex() < o2.getIndex())
                {
                    return -1;
                }
                else if (o1.getIndex() == o2.getIndex())
                {
                    return 0;
                }
                else
                {
                    return 1;
                }
            }
        });
    }

    protected Object cleanUpPayload(final Object payload)
    {
        final Object cleanedUpPayload;
        if (payload instanceof Object[])
        {
            final Object[] payloadArray = (Object[]) payload;
            final List<Object> payloadList = new ArrayList<Object>(payloadArray.length);
            for (Object object : payloadArray)
            {
                if (object != null && object != MessageContentsList.REMOVED_MARKER)
                {
                    payloadList.add(object);
                }
            }
            if (payloadList.size() == payloadArray.length)
            {
                cleanedUpPayload = payload; // no cleanup was done
            }
            else
            {
                cleanedUpPayload = payloadList.size() == 1 ? payloadList.get(0) : payloadList.toArray();
            }
        }
        else
        {
            cleanedUpPayload = payload;
        }
        return cleanedUpPayload;
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
