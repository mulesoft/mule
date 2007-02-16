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
import java.util.TreeSet;

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
     * Holds a list of field names
     */
    private List labels = null;

    /**
     * Whether to print labels or not
     */
    private boolean printLabels = false;

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
            this.writeRow((Map)o, 0);
        }
    }

    /**
     * Write the List as a CSV string. The List will contain Maps Each string in this
     * array represents a field in the CSV file.
     * 
     * @param l the List of Maps
     * @throws Exception
     */
    protected void write(List l) throws Exception
    {
        try
        {
            int rowPos = 0;
            for (Iterator i = l.iterator(); i.hasNext();)
            {
                writeRow((Map)i.next(), rowPos);
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
    protected void writeRow(Map row, int rowPos) throws Exception
    {
        Collection actualLabels;

        if (labels != null)
        {
            actualLabels = labels;
        }
        else
        {
            actualLabels = new TreeSet(row.keySet());
        }

        // If required, print the labels first
        if (rowPos == 0 && printLabels)
        {
            writeRowLabels(actualLabels);
        }

        // Now write the row, with columns ordered by the labels
        writeOrderedRow(row, actualLabels);
    }

    /**
     * Write the row Map as a CSV string as ordered by the column labels.
     *
     * Label ordering is provided either by the labels, as defined in the transformer 
     * properties, or by the keys of the data Map itself.
     *
     * If the keys are used for the ordering, then simple alphanumeric ordering is 
     * used.
     *
     * Note that if a label is defined, but there is no corresponding value, then an 
     * empty column will be printed.
     *
     * Note that if a field is defined in the Map AND the labels were defined in the
     * transformer properties AND the labels do not contain the field name, it will be
     * skipped.
     * 
     * @param row the Map containing row data
     * @param labels the Collection of the labels
     */
    private void writeOrderedRow(Map row, Collection labels)
    {
        String[] stringValues = new String[labels.size()];

        int i = 0;

        for (Iterator iter = labels.iterator(); iter.hasNext(); i++)
        {
            Object label = iter.next();
            Object value = row.get(label);
            
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

    /**
     * Write the row labels.
     * 
     * @param row the Collection containing the labels
     */
    public void writeRowLabels(Collection labels) throws Exception
    {
        String[] stringValues = new String[labels.size()];

        int i = 0;

        for (Iterator iter = labels.iterator(); iter.hasNext(); i++)
        {
            Object value = iter.next();
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

    /**
     * Gets whether or not to extract field names
     * 
     * @return true of extracting field names from the first line
     */
    public void setLabels(List labels)
    {
        this.labels = labels;
    }

    /**
     * Returns the List of field names
     * 
     * @return the List
     */
    public List getLabels()
    {
        return labels;
    }

    /**
     * Sets whether to print field names as the first line of output or not
     * 
     * @param printLabels boolean yes/no
     */
    public void setPrintLabels(boolean printLabels)
    {
        this.printLabels = printLabels;
    }

    /**
     * Gets whether to print field names as the first line of output or not
     */
    public boolean getPrintLabels()
    {
        return printLabels;
    }

}
