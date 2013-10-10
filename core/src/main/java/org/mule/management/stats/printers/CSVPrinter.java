/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management.stats.printers;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;

/**
 * <code>CSVPrinter</code> prints service stats in CSV format
 */
public class CSVPrinter extends AbstractTablePrinter
{
    private String delim = ",";
    private boolean printHeaders = true;

    public CSVPrinter(Writer out)
    {
        super(out);
    }

    public CSVPrinter(OutputStream out)
    {
        super(out);
    }

    public void print(Collection stats)
    {
        try
        {
            String[][] table = getTable(stats);
            int i = (printHeaders ? 0 : 1);
            for (; i < table.length; i++)
            {
                for (int j = 0; j < table[0].length; j++)
                {
                    print(table[i][j]);
                    if (j + 1 != table[i].length)
                    {
                        print(delim);
                    }
                }
                println();
            }
        }
        catch (Throwable e)
        {
            // TODO MULE-863: Unlikely to be sufficient
            // (and nothing explicitly thrown above)
            e.printStackTrace();
        }
    }

    public boolean isPrintHeaders()
    {
        return printHeaders;
    }

    public void setPrintHeaders(boolean printHeaders)
    {
        this.printHeaders = printHeaders;
    }
}
