/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.xml;

import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import org.mule.util.BeanUtils;

import java.util.Map;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class XmlPrettyPrinterTransformer extends AbstractTransformer
{
    protected OutputFormat outputFormat = OutputFormat.createPrettyPrint();

    public XmlPrettyPrinterTransformer()
    {
        super();
        this.registerSourceType(String.class);
        this.registerSourceType(org.dom4j.Document.class);
        this.setReturnClass(String.class);
    }

    public synchronized OutputFormat getOutputFormat()
    {
        return outputFormat;
    }

    public synchronized void setOutputFormat(OutputFormat newFormat)
    {
        outputFormat = newFormat;
    }

    public synchronized void setOutputFormatProperties(Map properties)
    {
        BeanUtils.populateWithoutFail(outputFormat, properties, false);
    }

    // @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Document document = null;

            if (src instanceof String)
            {
                String text = (String) src;
                document = DocumentHelper.parseText(text);
            }
            else if (src instanceof org.dom4j.Document)
            {
                document = (Document) src;
            }

            XMLWriter writer = new XMLWriter(resultStream, this.getOutputFormat());
            writer.write(document);
            writer.close();
            return resultStream.toString(encoding);
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
