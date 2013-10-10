/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.example.cep;

import java.text.DecimalFormat;

/**
 * A stock tick event informing of a state change due to some operation;
 * 
 * @author etirelli
 */
public class StockTick
{
    private final String symbol;
    private final double price;
    private final long timestamp;
    private double delta;
    private String str;

    public StockTick(String symbol, double price, long timestamp)
    {
        super();
        this.symbol = symbol;
        this.price = price;
        this.timestamp = timestamp;
        this.str = createString();
    }

    public String getSymbol()
    {
        return symbol;
    }

    public double getPrice()
    {
        return price;
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public String toString()
    {
        return str;
    }

    private String createString()
    {
        return symbol + " $" + price;
    }

    public double getDelta()
    {
        return delta;
    }

    public void setDelta(double delta)
    {
        this.delta = delta;
        this.str = createString();
    }

    public static String percent(double number)
    {
        return new DecimalFormat("0.0%").format(number);
    }
}
