/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.samples.loanbroker;

import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageHelper;
import org.mule.util.Utility;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>LoanConsumer</code> TODO
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class LoanConsumer
{
    private List customers = new ArrayList();
    private MuleClient client = null;

    public LoanConsumer() throws UMOException
    {
        init();
    }

    public LoanConsumer(String config) throws UMOException
    {
        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure(config);
        init();
    }

    private void init() throws UMOException
    {
        client = new MuleClient();

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

    public void close()
    {
        MuleManager.getInstance().dispose();
    }

    public LoanRequest createRequest()
    {
        int index = new Double(Math.random() * 10).intValue();
        Customer c = (Customer) customers.get(index);

        return new LoanRequest(c, getRandomAmount(), getRandomDuration());
    }

    private static double getRandomAmount()
    {
        return Math.round(Math.random() * 18000);
    }

    private static int getRandomDuration()
    {
        return new Double(Math.random() * 60).intValue();
    }

    public void request(LoanRequest request, boolean sync) throws Exception
    {
        if(!sync) {
            client.dispatch("vm://LoanBrokerRequests", request, null);
            System.out.println("Sent Async request");
            //let the request catch up
            Thread.sleep(1500);
        } else {
            UMOMessage result = client.send("vm://LoanBrokerRequests", request, null);
            System.out.println("Loan Consumer received a Quote: " + result.getPayload());
        }
    }

    public void requestDispatch(int number, String endpoint) throws Exception
    {
        for (int i = 0; i < number; i++)
        {
            client.dispatch(endpoint,createRequest(), null);
        }
    }

    public List requestSend(int number, String endpoint) throws Exception
    {
        List results = new ArrayList(number);
        UMOMessage result;
        for (int i = 0; i < number; i++)
        {
            result = client.send(endpoint,createRequest(), null);
            if(result!=null) {
                results.add(result.getPayload());
            }
        }
        return results;
    }

    public void requestRandom(int number, boolean sync) throws Exception
    {

        for (int i = 0; i < number; i++)
        {
            request(createRequest(), sync);
        }
    }

    public static void main(String[] args)
    {
        LoanConsumer loanConsumer = null;
        boolean synchronous = false;
        try
        {
            if (args.length > 0)
            {
                loanConsumer = new LoanConsumer(args[0]);
                int i = 100;
                if(args.length > 1) {
                    i = Integer.parseInt(args[1]);
                }
                boolean sync = false;
                if(args.length > 2) {
                    sync = Boolean.valueOf(args[2]).booleanValue();
                }
                if(sync) {
                    long start = System.currentTimeMillis();
                    List results = loanConsumer.requestSend(i, "vm://LoanBrokerRequests");
                    System.out.println("Number or quotes received: " + results.size());
                    List output = new ArrayList();
                    int x=1;
                    for (Iterator iterator = results.iterator(); iterator.hasNext();x++)
                    {
                        org.mule.samples.loanbroker.LoanQuote quote = (org.mule.samples.loanbroker.LoanQuote) iterator.next();
                        output.add(x + ". " + quote.toString());
                    }
                    System.out.println(StringMessageHelper.getBoilerPlate(output, '*', 80));
                    long cur = System.currentTimeMillis();
                    System.out.println(Utility.getFormattedDuration(cur - start));
                    System.out.println("Avg request: " + ((cur-start)/x));
                } else {
                    loanConsumer.requestDispatch(i, "vm://LoanBrokerRequests");
                }
            } else
            {
                System.out.println(StringMessageHelper.getBoilerPlate("Welcome to the Mule Loan broker example"));
                System.out.println("\nWould you like to run the [s]ynchronous or [a]synchronous version?");
                int response = getSelection();
                if (response == 's')
                {
                    System.out.println("Loading Synchronous Loan Broker");
                    synchronous = true;
                    loanConsumer = new LoanConsumer("loan-broker-sync-config.xml");
                } else
                {
                    System.out.println("Loading Asynchronous Loan Broker");
                    synchronous = false;
                    loanConsumer = new LoanConsumer("loan-broker-async-config.xml");
                }

                while (response != 'q')
                {
                    System.out.println("\n[1] make a loan request");
                    System.out.println("[2] send 100 random requests");
                    System.out.println("[q] quit");
                    System.out.println("\nPlease make your selection: ");

                    response = getSelection();
                    if (response == '2')
                    {
                        if(synchronous) {
                            List list = loanConsumer.requestSend(100, "vm://LoanBrokerRequests");
                            int i=1;
                            for (Iterator iterator = list.iterator(); iterator.hasNext();i++)
                            {
                                System.out.println("Request " + i + ": " + iterator.next().toString());
                            }
                        } else {
                            loanConsumer.requestDispatch(100, "vm://LoanBrokerRequests");
                        }
                    } else if (response == '1')
                    {
                        LoanRequest request = getRequestFromUser();
                        loanConsumer.request(request, synchronous);
                    } else if (response == 'q')
                    {
                        System.out.println("Exiting now");
                        loanConsumer.close();
                        System.exit(0);
                    } else
                    {
                        System.out.println("That response is not recognised, try again:");
                    }
                }
            }
        } catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int getSelection() throws IOException
    {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }

    private static LoanRequest getRequestFromUser() throws IOException
    {
        byte[] buf = new byte[128];
        System.out.println("Enter your name:");
        int len = System.in.read(buf);
        String name = new String(buf, 0, len - 1);
        System.out.println("Enter loan Amount:");
        buf = new byte[16];
        len = System.in.read(buf);
        String amount = new String(buf, 0, len - 1);
        System.out.println("Enter loan Duration in months:");
        buf = new byte[16];
        len = System.in.read(buf);
        String duration = new String(buf, 0, len - 1);

        int d = 0;
        try
        {
            d = Integer.parseInt(duration);
        } catch (NumberFormatException e)
        {
            System.out.println("Failed to parse duration: " + duration + ". Using random default");
            d = getRandomDuration();
        }

        double a = 0;
        try
        {
            a = Double.valueOf(amount).doubleValue();
        } catch (NumberFormatException e)
        {
            System.out.println("Failed to parse amount: " + amount + ". Using random default");
            a = getRandomAmount();
        }

        Customer c = new Customer(name, getRandomSsn());
        LoanRequest request = new LoanRequest(c, a, d);
        return request;
    }

    private static int getRandomSsn()
    {
        return new Double(Math.random() * 6000).intValue();
    }
}
