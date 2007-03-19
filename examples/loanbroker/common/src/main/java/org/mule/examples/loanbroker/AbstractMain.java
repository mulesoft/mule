/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker;

import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;

import java.io.IOException;

/**
 * <code>Main</code> Executes the LoanBroker ESB application
 */

public abstract class AbstractMain
{
    protected static int readCharacter() throws IOException
    {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }

    protected static String readString() throws IOException
    {
        byte[] buf = new byte[80];
        System.in.read(buf);
        return new String(buf).trim();
    }

    protected static int readInt() throws IOException
    {
        try
        {
            return Integer.parseInt(readString());
        }
        catch (NumberFormatException nfex)
        {
            return 0;
        }
    }

    protected static double getRandomAmount()
    {
        return Math.round(Math.random() * 18000);
    }

    protected static int getRandomDuration()
    {
        return new Double(Math.random() * 60).intValue();
    }

    protected static CustomerQuoteRequest getRequestFromUser() throws IOException
    {
        byte[] buf = new byte[128];
        System.out.print(new Message("loanbroker-example", 16).getMessage());
        System.in.read(buf);
        String name = new String(buf).trim();
        System.out.print(new Message("loanbroker-example", 17).getMessage());
        buf = new byte[16];
        System.in.read(buf);
        String amount = new String(buf).trim();
        System.out.print(new Message("loanbroker-example", 18).getMessage());
        buf = new byte[16];
        System.in.read(buf);
        String duration = new String(buf).trim();

        int d = 0;
        try
        {
            d = Integer.parseInt(duration);
        }
        catch (NumberFormatException e)
        {
            System.out.println(new Message("loanbroker-example", 19, duration).getMessage());
            d = getRandomDuration();
        }

        double a = 0;
        try
        {
            a = Double.valueOf(amount).doubleValue();
        }
        catch (NumberFormatException e)
        {
            System.out.println(new Message("loanbroker-example", 20, amount).getMessage());
            a = getRandomAmount();
        }

        Customer c = new Customer(name, getRandomSsn());
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, a, d);
        return request;
    }

    protected static int getRandomSsn()
    {
        return new Double(Math.random() * 6000).intValue();
    }
}

