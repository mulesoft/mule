/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.cep;

import org.mule.api.MuleEventContext;
import org.mule.api.lifecycle.Callable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TickFeed implements Initialisable, Callable
{
    public static final String DATA_FILE = "stocktickstream.dat";
	private static final MessageFormat lineFormat = new MessageFormat("{0,number,0};{1};{2,number,currency}", Locale.US);
    
    private Iterator<String> lines;
    
    protected transient Log logger = LogFactory.getLog(getClass());
    
    public void initialise() throws InitialisationException
    {
        try
        {
            lines = readStockTickDataFile();
        }
        catch (IOException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    private Iterator<String> readStockTickDataFile() throws IOException
    {
        InputStream is = IOUtils.getResourceAsStream(DATA_FILE, TickFeed.class, false, false);
        try
        {
            List<String> linesList = IOUtils.readLines(is);
            logger.debug("Read data file, " + linesList.size() + " lines");
            return linesList.iterator();
        }
        finally
        {
            is.close();
        }
    }
    
    public Object onCall(MuleEventContext eventContext) throws Exception
    {
        // If we've gone through the entire datafile, start over again from the beginning.
        if (!lines.hasNext())
        {
            lines = readStockTickDataFile();
        }

        Object[] results = lineFormat.parse(lines.next());
        StockTick tick = new StockTick((String)results[1],
                                       ((Number)results[2]).doubleValue(),
                                       ((Number)results[0]).longValue());
        logger.info("New Stock Tick: " + tick);
        return tick;
    }
}


