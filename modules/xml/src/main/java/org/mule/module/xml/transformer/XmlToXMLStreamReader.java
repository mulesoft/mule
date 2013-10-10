/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transformer.TransformerException;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.xml.stax.ReversibleXMLStreamReader;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.w3c.dom.Document;

public class XmlToXMLStreamReader extends AbstractXmlTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;
    private boolean reversible;
    
    public XmlToXMLStreamReader()
    {
        super();
        registerSourceType(DataTypeFactory.create(Source.class));
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(Document.class));
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.STRING);

        setReturnDataType(DataTypeFactory.create(XMLStreamReader.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, String encoding) throws TransformerException
    {
        Object src = message.getPayload();
        try
        {
            XMLStreamReader xsr = XMLUtils.toXMLStreamReader(getXMLInputFactory(), src);
            if (xsr == null)
            {
                throw new TransformerException(MessageFactory
                    .createStaticMessage("Unable to convert " + src.getClass() + " to XMLStreamReader."), this);
            }
        
            if (reversible && !(xsr instanceof ReversibleXMLStreamReader))
            {
                return new ReversibleXMLStreamReader(xsr);
            }
            else
            {
                return xsr;
            }
        }
        catch (XMLStreamException e)
        {
            throw new TransformerException(this, e);
        }
    }

    public boolean isReversible()
    {
        return reversible;
    }

    public void setReversible(boolean reversible)
    {
        this.reversible = reversible;
    }

    public int getPriorityWeighting()
    {
        return priorityWeighting;
    }

    public void setPriorityWeighting(int priorityWeighting)
    {
        this.priorityWeighting = priorityWeighting;
    }
}
