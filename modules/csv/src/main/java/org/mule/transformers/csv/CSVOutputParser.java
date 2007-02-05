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

import au.com.bytecode.opencsv.CSVWriter;

import java.io.Writer;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Parser to write CSV data using the OpenCSV writer
 */
public class CSVOutputParser implements CSVParser
{
    /**
     * The OpenCVS writer
     */
    private CSVWriter writer = null;

    /**
     * Constructor
     * 
     * @param out output writer
     * @param separator character to use as the field delimiter
     */
    public CSVOutputParser(Writer out, char separator, char quoteChar)
    {
        if (separator == '\u0000')
        {
            separator = CSVParser.DEFAULT_SEPARATOR;
        }

        writer = new CSVWriter(out, separator, quoteChar);
    }

    /**
     * Convert the object to CSV. We accept a List of Maps that represents multiple
     * rows or just a Map that represents one row.
     * 
     * @param o source object
     */
    public void write(Object o) throws Exception
    {
        if (o instanceof List)
        {
            this.write((List)o);
        }
        else if (o instanceof Map)
        {
            this.writeRow((Map)o);
        }
    }

    /**
     * Write the List as a CSV string. The List will contain Maps Each string in this
     * array represents a field in the CSV file.
     * 
     * @param l the List of Maps
     * @throws Exception
     */
    public void write(List l) throws Exception
    {
        try
        {
            for (Iterator i = l.iterator(); i.hasNext();)
            {
                writeRow((Map)i.next());
            }
        }
        finally
        {
            writer.close();
        }
    }

    /**
     * Write the row Map as a CSV string.
     * 
     * @param row the Map containing row data
     */
    public void writeRow(Map row) throws Exception
    {
        Collection values = row.values();
        String[] stringValues = new String[values.size()];

        int i = 0;
        for (Iterator v = values.iterator(); v.hasNext(); i++)
        {
            Object value = v.next();

            if (value != null)
            {
                stringValues[i] = value.toString();
            }
            else
            {
                stringValues[i] = "";
            }
        }

        writer.writeNext(stringValues);
    }

}
