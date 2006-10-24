/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.axis;

/**
 * TODO document
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
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
