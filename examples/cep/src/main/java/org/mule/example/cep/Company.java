/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.cep;

/**
 * A POJO for a company
 * 
 * @author etirelli
 */
public class Company
{
    private String name;
    private String symbol;
    private double currentPrice;
    private double previousPrice;

    public Company(String name, String symbol)
    {
        this(name, symbol, 0, 0);
    }

    public Company(String name, String symbol, double current, double previous)
    {
        this.name = name;
        this.symbol = symbol;
        this.currentPrice = current;
        this.previousPrice = previous;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public double getCurrentPrice()
    {
        return currentPrice;
    }

    public void setCurrentPrice(double current)
    {
        this.previousPrice = this.currentPrice;
        this.currentPrice = current;
    }

    public double getPreviousPrice()
    {
        return previousPrice;
    }

    public double getDelta()
    {
        return (previousPrice == 0) ? 0.0 : ((currentPrice / previousPrice) - 1.0);
    }

}
