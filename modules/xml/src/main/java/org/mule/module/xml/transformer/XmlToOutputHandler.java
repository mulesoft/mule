/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.transformer;

import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.DiscoverableTransformer;
import org.mule.api.transport.OutputHandler;
import org.mule.module.xml.util.XMLUtils;
import org.mule.transformer.types.DataTypeFactory;

import java.io.IOException;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;

import org.dom4j.Document;

public class XmlToOutputHandler extends AbstractXmlTransformer implements DiscoverableTransformer
{

    private int priorityWeighting = DiscoverableTransformer.DEFAULT_PRIORITY_WEIGHTING;

    public XmlToOutputHandler()
    {
        registerSourceType(DataTypeFactory.STRING);
        registerSourceType(DataTypeFactory.BYTE_ARRAY);
        registerSourceType(DataTypeFactory.create(Source.class));
        registerSourceType(DataTypeFactory.create(Document.class));
        registerSourceType(DataTypeFactory.create(org.w3c.dom.Document.class));
        registerSourceType(DataTypeFactory.create(org.w3c.dom.Element.class));
        registerSourceType(DataTypeFactory.INPUT_STREAM);
        registerSourceType(DataTypeFactory.create(OutputHandler.class));
        registerSourceType(DataTypeFactory.create(XMLStreamReader.class));
        registerSourceType(DataTypeFactory.create(DelayedResult.class));
        setReturnDataType(DataTypeFactory.create(OutputHandler.class));
    }

    @Override
    public Object transformMessage(MuleMessage message, final String encoding)
    {
        final Object src = message.getPayload();
        return new OutputHandler()
        {
            public void write(MuleEvent event, OutputStream out) throws IOException
            {
                writeXml(src, encoding, out);
            }
        };
    }

    protected void writeXml(final Object src, final String encoding, OutputStream out)
        throws TransformerFactoryConfigurationError, IOException
    {
        try
        {
            if (src instanceof XMLStreamReader)
            {
                // Unfortunately, the StAX source doesn't copy/serialize correctly so
                // we have to do this little hack.
                XMLStreamReader reader = (XMLStreamReader)src;
                XMLStreamWriter writer = getXMLOutputFactory().createXMLStreamWriter(out);
                
                try {
                    writer.writeStartDocument();
                    XMLUtils.copy(reader, writer);
                    writer.writeEndDocument();
                } finally {
                    writer.close();
                    reader.close();
                }
            }
            else if (src instanceof DelayedResult)
            {
                DelayedResult result = (DelayedResult) src;
                
                StreamResult streamResult = new StreamResult(out);
                result.write(streamResult);
            }
            else
            {
                writeToStream(src, encoding, out);
            }
        }
        catch (Exception e)
        {
            IOException ioe = new IOException(e.toString());
            ioe.initCause(e);
            throw ioe;
        }
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
