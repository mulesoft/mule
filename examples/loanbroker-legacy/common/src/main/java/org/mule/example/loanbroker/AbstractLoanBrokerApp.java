/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.example.loanbroker;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.config.spring.SpringXmlConfigurationBuilder;
import org.mule.example.loanbroker.messages.Customer;
import org.mule.example.loanbroker.messages.CustomerQuoteRequest;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Runs the LoanBroker example application.
 */
public abstract class AbstractLoanBrokerApp
{
    private List<Customer> customers = new ArrayList<Customer>();
    private RemoteDispatcher remoteClient = null;
    private String config;

    public AbstractLoanBrokerApp() throws Exception
    {
        this.config = null;
        init();
    }

    public AbstractLoanBrokerApp(String config) throws Exception
    {
        this.config = config;
        init();
    }

    protected void init() throws Exception
    {
        MuleClient muleClient = new MuleClient(true);
        remoteClient = muleClient.getRemoteDispatcher("tcp://localhost:5555");
        customers.add(new Customer("Jenson Button", 123));
        customers.add(new Customer("Michael Schumacker", 456));
        customers.add(new Customer("Juan Pablo Montoya", 789));
        customers.add(new Customer("David Colthard", 101));
        customers.add(new Customer("Rubens Barrichello", 112));
        customers.add(new Customer("Mark Webber", 131));
        customers.add(new Customer("Takuma Sato", 415));
        customers.add(new Customer("Kimi Raikkonen", 161));
        customers.add(new Customer("Ralf Schumacher", 718));
        customers.add(new Customer("Jarno Trulli", 192));
    }

    protected ConfigurationBuilder getConfigBuilder() throws MuleException
    {
        return new SpringXmlConfigurationBuilder(config);
    }

    protected void dispose() throws Exception
    {
        remoteClient.dispose();
    }

    protected void run(boolean synchronous) throws Exception
    {
        int response = 0;
        while (response != 'q')
        {
            System.out.println("\n" + LocaleMessage.menu());

            response = readCharacter();

            switch (response)
            {
                case '1' :
                {
                    CustomerQuoteRequest request = getRequestFromUser();
                    request(request, synchronous);
                    break;
                }

                case '2' :
                {
                    sendRandomRequests(100, synchronous);
                    break;
                }

                case '3' :
                {
                    System.out.println(LocaleMessage.menuOptionNumberOfRequests());
                    int number = readInt();
                    if (number < 1)
                    {
                        System.out.println(LocaleMessage.menuErrorNumberOfRequests());
                    }
                    else
                    {
                        sendRandomRequests(number, synchronous);
                    }
                    break;
                }

                case 'q' :
                {
                    System.out.println(LocaleMessage.exiting());
                    dispose();
                    System.exit(0);
                    break; // no, we never reach this statement. But it shuts off the compiler warning
                }

                default :
                {
                    System.out.println(LocaleMessage.menuError());
                }
            }
        }
    }

    public CustomerQuoteRequest createRequest()
    {
        int index = new Double(Math.random() * 10).intValue();
        Customer c = customers.get(index);

        return new CustomerQuoteRequest(c, getRandomAmount(), getRandomDuration());
    }

    protected static CustomerQuoteRequest getRequestFromUser() throws IOException
    {
        byte[] buf = new byte[128];
        System.out.print(LocaleMessage.enterName());
        System.in.read(buf);
        String name = new String(buf).trim();
        System.out.print(LocaleMessage.enterLoanAmount());
        buf = new byte[16];
        System.in.read(buf);
        String amount = new String(buf).trim();
        System.out.print(LocaleMessage.enterLoanDuration());
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
            System.out.println(LocaleMessage.loanDurationError(duration));
            d = getRandomDuration();
        }

        double a = 0;
        try
        {
            a = Double.valueOf(amount).doubleValue();
        }
        catch (NumberFormatException e)
        {
            System.out.println(LocaleMessage.loanAmountError(amount));
            a = getRandomAmount();
        }

        Customer c = new Customer(name, getRandomSsn());
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, a, d);
        return request;
    }

    public void request(CustomerQuoteRequest request, boolean sync) throws Exception
    {
        if (!sync)
        {
            remoteClient.dispatchRemote("CustomerRequests", request, null);
            System.out.println(LocaleMessage.sentAsync());
            // let the request catch up
            Thread.sleep(3000);

        }
        else
        {
            MuleMessage result = remoteClient.sendRemote("CustomerRequests", request, null);
            if (result == null)
            {
                System.out.println(LocaleMessage.requestError());
            }
            else
            {
                System.out.println(LocaleMessage.requestResponse(result.getPayload()));
            }
        }
    }

    public void requestDispatch(int number, String endpoint) throws Exception
    {
        for (int i = 0; i < number; i++)
        {
            remoteClient.dispatchRemote(endpoint, createRequest(), null);
        }
    }

    public List<Object> requestSend(int number, String endpoint) throws Exception
    {
        List<Object> results = new ArrayList<Object>(number);
        for (int i = 0; i < number; i++)
        {
            MuleMessage result = remoteClient.sendRemote(endpoint, createRequest(), null);

            if (result != null)
            {
                results.add(result.getPayload());
            }
        }
        return results;
    }

    protected void sendRandomRequests(int number, boolean synchronous) throws Exception
    {
        if (synchronous)
        {
            List<Object> list = requestSend(number, "CustomerRequests");
            int i = 1;
            System.out.println("sendRandomRequests");
            for (Iterator<Object> iterator = list.iterator(); iterator.hasNext(); i++)
            {
                System.out.println("sendRandomRequests results :" +
                    LocaleMessage.request(i, iterator.next().toString()));
            }
        }
        else
        {
            this.requestDispatch(number, "CustomerRequests");
            System.out.println(LocaleMessage.sentAsync());
        }
    }

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

    protected static int getRandomSsn()
    {
        return new Double(Math.random() * 6000).intValue();
    }
}
