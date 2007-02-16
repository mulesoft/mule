/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transformers.csv;

import org.mule.umo.transformer.TransformerException;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

/**
 * Transform a Map to a single CSV row.
 */
public class MapToCSV extends AbstractCSVTransformer
{
    private static final long serialVersionUID = 7706571607820420420L;

    public MapToCSV()
    {
        super.registerSourceType(Map.class);
        super.setReturnClass(String.class);
    }

    /**
     * Do the transformation
     * 
     * @param src source data
     * @param encoding data encoding
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        try
        {
            Writer stringWriter = new StringWriter();
            CSVOutputParser writer = new CSVOutputParser(stringWriter, separator, quoteCharacter);
            writer.setLabels(fieldNames);
            writer.setPrintLabels(printLabels);
            writer.write(src);
            return stringWriter.toString();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
    }

}
