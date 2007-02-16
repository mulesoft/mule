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
import org.mule.util.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Transform a CSV string to a List of Maps. Each Map represents the fields of a row.
 * The key of the map is either the zero-based sequence number or the actual field
 * names. Field names can be passed in via the configuration or can be parsed out of
 * the first line of the CSV data.
 */
public class CSVToMaps extends AbstractCSVTransformer
{
    private static final long serialVersionUID = 4050673946506172890L;

    public CSVToMaps()
    {
        registerSourceType(String.class);
        registerSourceType(byte[].class);
        registerSourceType(File.class);
        setReturnClass(List.class);
    }

    /**
     * Do the transformation
     * 
     * @param src source data
     * @param encoding data encoding
     */
    public Object doTransform(Object src, String encoding) throws TransformerException
    {
        Reader reader = null;

        try
        {
            reader = this.getReader(src, encoding);
            CSVInputParser parser = new CSVInputParser(reader, separator, quoteCharacter, startLine);
            parser.setFirstLineLabels(firstLineLabels);
            parser.setLabels(fieldNames);
            return parser.parse();
        }
        catch (Exception e)
        {
            throw new TransformerException(this, e);
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }
    }

    protected Reader getReader(Object src, String encoding)
        throws UnsupportedEncodingException, FileNotFoundException
    {
        if (src instanceof byte[])
        {
            return new StringReader(new String((byte[])src, encoding));
        }
        else if (src instanceof String)
        {
            return new StringReader((String)src);
        }
        else if (src instanceof File)
        {
            return new BufferedReader(new FileReader((File)src));
        }
        else
        {
            throw new IllegalArgumentException(src.getClass().getName());
        }
    }

}
