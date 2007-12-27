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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

public class XmlPrettyPrinterTransformer extends AbstractTransformer
{
    protected final OutputFormat format = OutputFormat.createPrettyPrint();

    public XmlPrettyPrinterTransformer()
    {
        super();
        this.registerSourceType(java.lang.String.class);
        this.registerSourceType(org.dom4j.Document.class);
        this.setReturnClass(String.class);
    }

    // @Override
    protected Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
            Document document = null;

            if (src instanceof java.lang.String)
            {
                String text = (String) src;
                document = DocumentHelper.parseText(text);
            }
            else if (src instanceof org.dom4j.Document)
            {
                document = (Document) src;
            }

            XMLWriter writer = new XMLWriter(resultStream, format);
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
