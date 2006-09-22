/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats.printers;

import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;

/**
 * <code>CSVPrinter</code> prints component stats in CSV format
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
        try {
            String[][] table = getTable(stats);
            int i = (printHeaders ? 0 : 1);
            for (; i < table.length; i++) {
                for (int j = 0; j < table[0].length; j++) {
                    print(table[i][j]);
                    if (j + 1 != table[i].length) {
                        print(delim);
                    }
                }
                println();
            }
        } catch (Throwable e) {
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
