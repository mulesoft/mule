/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.soap;

import org.dom4j.Namespace;
import org.dom4j.QName;
import org.dom4j.dom.DOMElement;
import org.mule.config.MuleProperties;
import org.mule.umo.UMOEvent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
        SOAPHeaderElement headerElement;
        while (iter.hasNext()) {
            headerElement = (SOAPHeaderElement) iter.next();
            
            //checking that the elements are part of the mule namespace
            if (org.mule.util.StringUtils.equals(MULE_10_ACTOR, headerElement.getNamespaceURI()))
            {
                Iterator iter2 = headerElement.getChildElements();
                readElements(iter2);
            }
        }
    }

    public MuleSoapHeaders(Iterator elements)
    {
        readElements(elements);
    }

    protected void readElements(Iterator elements) {

        SOAPElement element;
        
        while (elements.hasNext()) {
        	
    		Object elementObject = elements.next();

            //Fixed MULE-770 (http://jira.symphonysoft.com/browse/MULE-770)
    		if (elementObject instanceof SOAPElement) 
    		// if not, means that it is a value not an element, therefore we cannot look for correlation_id ...
    		{
                element = (SOAPElement)elementObject;
                String localName = element.getLocalName();
                String elementValue = getStringValue(element);
                
                if (MuleProperties.MULE_CORRELATION_ID_PROPERTY.equals(localName)) {
                    correlationId = elementValue;
                } else if (MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY.equals(localName)) {
                    correlationGroup = elementValue;
                } else if (MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY.equals(localName)) {
                    correlationSequence = elementValue;
                } else if (MuleProperties.MULE_REPLY_TO_PROPERTY.equals(localName)) {
                    replyTo = elementValue;
                }
                
    		}
        }
    }
    private String getStringValue(Element e)
    {
        String value = e.getNodeValue();
        if (value == null && e.hasChildNodes()) {
            // see if the value is base64 ecoded
            value = e.getFirstChild().getNodeValue();
            if (value != null) {
                //value = new String(org.apache.axis.encoding.Base64.decode(value));
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
            SOAPElement e = muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_ID_PROPERTY,
                                                                           MULE_NAMESPACE);
            e.addTextNode(correlationId);
            e = muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY,
                                                            MULE_NAMESPACE);
            e.addTextNode(correlationGroup);
            e = muleHeader.addChildElement(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY,
                                                            MULE_NAMESPACE);
            e.addTextNode(correlationSequence);
        }
        if (replyTo != null) {
           SOAPElement e = muleHeader.addChildElement(MuleProperties.MULE_REPLY_TO_PROPERTY,
                                                                           MULE_NAMESPACE);
            //String enc = (String)encoder.transform(replyTo);
            //e.addTextNode(enc);
            e.addTextNode(replyTo);
        }
    }

    public Element createHeaders() throws Exception
    {
        Element muleHeader = null;

        if (correlationId != null || replyTo != null) {
            muleHeader = new DOMElement(new QName(MULE_HEADER, new Namespace(MULE_NAMESPACE, MULE_10_ACTOR)));
        } else {
            return null;
        }

        if (correlationId != null) {
            Node e = muleHeader.appendChild(new DOMElement(new QName(MuleProperties.MULE_CORRELATION_ID_PROPERTY, new Namespace(MULE_NAMESPACE, MULE_10_ACTOR))));
            e.setNodeValue(correlationId);

            e = muleHeader.appendChild(new DOMElement(new QName(MuleProperties.MULE_CORRELATION_GROUP_SIZE_PROPERTY, new Namespace(MULE_NAMESPACE, MULE_10_ACTOR))));
            e.setNodeValue(correlationGroup);

            e = muleHeader.appendChild(new DOMElement(new QName(MuleProperties.MULE_CORRELATION_SEQUENCE_PROPERTY, new Namespace(MULE_NAMESPACE, MULE_10_ACTOR))));
            e.setNodeValue(correlationSequence);
        }
        if (replyTo != null) {

            Node e = muleHeader.appendChild(new DOMElement(new QName(MuleProperties.MULE_REPLY_TO_PROPERTY, new Namespace(MULE_NAMESPACE, MULE_10_ACTOR))));
            e.setNodeValue(replyTo);
        }
        return muleHeader;
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
