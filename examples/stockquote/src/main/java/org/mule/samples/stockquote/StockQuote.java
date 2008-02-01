/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.samples.stockquote;

import java.io.Serializable;

/**
 * A stock Quote object that is crated from the xml returned from the
 * http://www.webservicex.net/stockquote.asmx service
 */
public class StockQuote implements Serializable
{
    private static final long serialVersionUID = -3579080716991795218L;

    private String symbol;
    private String last;
    private String change;
    private String open;
    private String high;
    private String low;
    private String volume;
    private String previousClose;
    private String name;
    private String date;

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public String getLast()
    {
        return last;
    }

    public void setLast(String last)
    {
        this.last = last;
    }

    public String getChange()
    {
        return change;
    }

    public void setChange(String change)
    {
        this.change = change;
    }

    public String getOpen()
    {
        return open;
    }

    public void setOpen(String open)
    {
        this.open = open;
    }

    public String getHigh()
    {
        return high;
    }

    public void setHigh(String high)
    {
        this.high = high;
    }

    public String getLow()
    {
        return low;
    }

    public void setLow(String low)
    {
        this.low = low;
    }

    public String getVolume()
    {
        return volume;
    }

    public void setVolume(String volume)
    {
        this.volume = volume;
    }

    public String getPreviousClose()
    {
        return previousClose;
    }

    public void setPreviousClose(String previousClose)
    {
        this.previousClose = previousClose;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public String toString()
    {
        return LocaleMessage.getStockQuoteMessage(symbol, name, date, last, change, open, high,
            low, volume, previousClose);
    }
}
