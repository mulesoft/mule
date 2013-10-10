/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
