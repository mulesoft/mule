/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.stockquote;

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

    @Override
    public String toString()
    {
        return LocaleMessage.getStockQuoteMessage(symbol, name, date, last, change, open, high,
            low, volume, previousClose);
    }
}
