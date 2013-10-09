/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.axis;

/**
 * TODO document
 */
public class Trade implements java.io.Serializable
{
    private static final long serialVersionUID = -1225935545079750532L;

    private int accountID;
    private java.lang.String cusip;
    private int currency;
    private int tradeID;
    private int transaction;

    public Trade()
    {
        super();
    }

    public int getAccountID()
    {
        return accountID;
    }

    public void setAccountID(int accountID)
    {
        this.accountID = accountID;
    }

    public java.lang.String getCusip()
    {
        return cusip;
    }

    public void setCusip(java.lang.String cusip)
    {
        this.cusip = cusip;
    }

    public int getCurrency()
    {
        return currency;
    }

    public void setCurrency(int currency)
    {
        this.currency = currency;
    }

    // public float getPrice() {
    // return price;
    // }

    // public void setPrice(float price) {
    // this.price = price;
    // }

    public int getTradeID()
    {
        return tradeID;
    }

    public void setTradeID(int tradeID)
    {
        this.tradeID = tradeID;
    }

    public int getTransaction()
    {
        return transaction;
    }

    public void setTransaction(int transaction)
    {
        this.transaction = transaction;
    }

}
