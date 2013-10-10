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
 * <code>SimplePrinter</code> Default stats printer
 */
public class SimplePrinter extends AbstractTablePrinter
{
    public SimplePrinter(Writer out)
    {
        super(out);
    }

    public SimplePrinter(OutputStream out)
    {
        super(out);
    }

    public void print(Collection stats)
    {
        String[][] table = getTable(stats);
        for (int i = 1; i < table.length; i++)
        {
            println();
            println("---- Service Statistics ----");
            for (int j = 0; j < table[0].length; j++)
            {
                println(table[0][j] + ": " + table[i][j]);
            }
            println("---- End Service Statistics ----");
        }
    }
}
