/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
