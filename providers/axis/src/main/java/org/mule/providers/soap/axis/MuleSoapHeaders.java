/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.soap.axis;

import org.mule.config.MuleProperties;
import org.mule.transformers.codec.Base64Encoder;
import org.mule.umo.UMOEvent;
import org.w3c.dom.Element;

import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPHeaderElement;
import java.util.Iterator;

/**
 * <code>MuleSoapHeaders</code> is a helper class for extracting and writing
 * Mule header properties to s Soap message
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class MuleSoapHeaders
{
    private String replyTo;
    private String correlationId;
    private String correlationGroup;
    private String correlationSequence;

    public static final String MULE_10_ACTOR = "http://www.muleumo.org/providers/soap/1.0";
    public static final String MULE_NAMESPACE = "mule";
    public static final String MULE_HEADER = "header";
    public static final String ENV_REQUEST_HEADERS = "MULE_REQUEST_HEADERS";

    private Base64Encoder encoder = new Base64Encoder();

    /**
     * Extracts header properties from a Mule event
     * 
     * @param event
     */
    public MuleSoapHeaders(UMOEvent event)
    {
        setCorrelationId(event.getMessage().getCorrelationId());
        setCorrelationGroup(String.valueOf(event.getMessage().getCorrelationGroupSize()));
        setCorrelationSequence(String.valueOf(event.getMessage().getCorrelationSequence()));
        setReplyTo((String) event.getMessage().getReplyTo());
    }

    /**
     * Extracts Mule header properties from a Soap message
     * 
     * @param soapHeader
     */
    public MuleSoapHeaders(SOAPHeader soapHeader)
    {
        Iterator iter = soapHeader.examineHeaderElements(MULE_10_ACTOR);
        SOAPHeaderElement header;
        while (iter.hasNext()) {
            header = (SOAPHeaderElement) iter.next();
            Iterator iter2 = header.getChildElements();
            readElements(iter2);
        }
    }

    public MuleSoapHeaders(Iterator elements)
    {
        readElements(elements);
    }

    protected void readElements(Iterator elements) {
        Element element;
        while (elements.hasNext()) {
                element = (SOAPElement) elements.next();
                if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(element.getNodeName())) {
                    correlationId = getStringValue(element);
                } else if (MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY.equals(element.getNodeName())) {
                    correlationGroup = getStringValue(element);
                } else if (MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY.equals(element.getNodeName())) {
                    correlationSequence = getStringValue(element);
                } else if (MuleProperties.MULE_REPLY_TO_PROPERTY.equals(element.getNodeName())) {
                    replyTo = getStringValue(element);
                }
            }
    }
    private String getStringValue(Element e)
    {
        String value = e.getNodeValue();
        if (value == null && e.hasChildNodes()) {
            // see if the value is base64 ecoded
            value = e.getChildNodes().item(0).getChildNodes().item(0).getNodeValue();
            if (value != null) {
                value = new String(org.apache.axis.encoding.Base64.decode(value));
            }
        }
        return value;
    }

    /**
     * Writes the header properties to a Soap header
     * 
     * @param env
     * @throws SOAPException
     */
    public void addHeaders(SOAPEnvelope env) throws Exception
    {
        SOAPHeader header = env.getHeader();
        SOAPHeaderElement muleHeader;
        if (correlationId != null || replyTo != null) {
            if (header == null) {
                header = env.addHeader();
            }
            Name muleHeaderName = env.createName(MULE_HEADER, MULE_NAMESPACE, MULE_10_ACTOR);
            muleHeader = header.addHeaderElement(muleHeaderName);
            muleHeader.setActor(MULE_10_ACTOR);
        } else {
            return;
        }

        if (correlationId != null) {
            Element e = muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_ID_PROPERTY,
                                                                           MULE_NAMESPACE);
            e.setNodeValue(correlationId);
            e = (Element) muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                                                            MULE_NAMESPACE);
            e.setNodeValue(correlationGroup);
            e = muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                                                            MULE_NAMESPACE);
            e.setNodeValue(correlationSequence);
        }
        if (replyTo != null) {
            Element e = (Element) muleHeader.addChildElement(MuleProperties.MULE_REPLY_TO_PROPERTY,
                                                                           MULE_NAMESPACE);
            String enc = (String)encoder.transform(replyTo);
            e.setNodeValue(enc);
        }
    }

    public String getReplyTo()
    {
        return replyTo;
    }

    public void setReplyTo(String replyTo)
    {
        this.replyTo = replyTo;
    }

    public String getCorrelationId()
    {
        return correlationId;
    }

    public void setCorrelationId(String correlationId)
    {
        this.correlationId = correlationId;
    }

    public String getCorrelationGroup()
    {
        return correlationGroup;
    }

    public void setCorrelationGroup(String correlationGroup)
    {
        this.correlationGroup = correlationGroup;
    }

    public String getCorrelationSequence()
    {
        return correlationSequence;
    }

    public void setCorrelationSequence(String correlationSequence)
    {
        this.correlationSequence = correlationSequence;
    }
}
