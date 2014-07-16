/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

    @Override
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

    @SuppressWarnings("unchecked")
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

    @Override
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


