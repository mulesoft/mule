/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esn;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.AbstractMain;
import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;
import org.mule.util.StringUtils;
import org.mule.util.SystemUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <code>Main</code> is a loan broker client app that uses command line
 * prompts to obtain loan requests
 */

public class Main extends AbstractMain
{
    public static final String CLI_OPTIONS[][] = {
            { "config", "true", "Configuration File" },
            { "main", "true", "Main Class"},
            { "req", "true", "Number of loan requests to use"},
            { "sync", "true", "Whether to run in synchronous mode or not"}
        };

    private static boolean synchronous = false;

    private List customers = new ArrayList();
    private MuleClient client = null;

    public Main() throws UMOException
    {
        init();
    }

    public Main(String config) throws UMOException
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
        client.dispose();
    }

    public CustomerQuoteRequest createRequest()
    {
        int index = new Double(Math.random() * 10).intValue();
        Customer c = (Customer)customers.get(index);

        return new CustomerQuoteRequest(c, getRandomAmount(), getRandomDuration());
    }

    public void request(CustomerQuoteRequest request, boolean sync) throws Exception
    {
        if (!sync)
        {
            client.dispatch("vm://customer.requests", request, null);
            System.out.println(new Message("loanbroker-example", 42).getMessage());
            // let the request catch up
            Thread.sleep(1500);
        }
        else
        {
            UMOMessage result = client.send("vm://customer.requests", request, null);
            if (result == null)
            {
                System.out.println(new Message("loanbroker-example", 12).getMessage());
            }
            else
            {
                System.out.println(new Message("loanbroker-example", 13, result.getPayload()).getMessage());
            }
        }
    }

    public void requestDispatch(int number, String endpoint) throws Exception
    {
        for (int i = 0; i < number; i++)
        {
            client.dispatch(endpoint, createRequest(), null);
        }
    }

    public List requestSend(int number, String endpoint) throws Exception
    {
        List results = new ArrayList(number);
        UMOMessage result;
        for (int i = 0; i < number; i++)
        {
            result = client.send(endpoint, createRequest(), null);
            if (result != null)
            {
                results.add(result.getPayload());
            }
        }
        return results;
    }

    public static void main(String[] args)
    {
        Main loanConsumer = null;
        try
        {
            Map options = SystemUtils.getCommandLineOptions(args, CLI_OPTIONS);
            String config = (String)options.get("config");

            if (StringUtils.isNotBlank(config))
            {
                loanConsumer = new Main(config);

                int i = 100;
                String requests = (String)options.get("req");
                if (requests != null)
                {
                    i = Integer.parseInt(requests);
                }

                String sync = (String)options.get("sync");
                if (sync != null)
                {
                    synchronous = Boolean.valueOf(sync).booleanValue();
                }

                if (synchronous)
                {
                    long start = System.currentTimeMillis();
                    List results = loanConsumer.requestSend(i, "vm://customer.requests");
                    System.out.println(new Message("loanbroker-example", 10, String.valueOf(results.size())).getMessage());
                    List output = new ArrayList(results.size());
                    int x = 1;
                    for (Iterator iterator = results.iterator(); iterator.hasNext(); x++)
                    {
                        LoanQuote quote = (LoanQuote)iterator.next();
                        output.add(x + ". " + quote.toString());
                    }
                    System.out.println(StringMessageUtils.getBoilerPlate(output, '*', 80));
                    long cur = System.currentTimeMillis();
                    System.out.println(DateUtils.getFormattedDuration(cur - start));
                    System.out.println(new Message("loanbroker-example", 11, String.valueOf( ((cur - start) / x) )).getMessage());
                }
                else
                {
                    loanConsumer.requestDispatch(i, "vm://customer.requests");
                }
            }
            else
            {
                loanConsumer = new Main(getInteractiveConfig());
                loanConsumer.run(synchronous);
            }

        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected static String getInteractiveConfig() throws IOException
    {
        System.out.println(StringMessageUtils.getBoilerPlate(new Message("loanbroker-example", 40).getMessage()));
                    
        int response = 0;
        String provider = "axis";

        while (response != 'a' && response != 'x')
        {
            System.out.println("\n" + new Message("loanbroker-example", 43).getMessage());
            response = readCharacter();
            switch (response)
            {
                case 'a' :
                {
                    provider = "axis";
                    break;
                }
                case 'x' :
                {
                    provider = "xfire";
                    break;
                }
            }
        }

        response = 0;
        while (response != 'a' && response != 's')
        {
            System.out.println("\n" + new Message("loanbroker-example", 44).getMessage());
            response = readCharacter();
            switch (response)
            {
                case 'a' :
                {
                    System.out.println(new Message("loanbroker-example", 45).getMessage());
                    synchronous = false;
                    break;
                }

                case 's' :
                {
                    System.out.println(new Message("loanbroker-example", 46).getMessage());
                    synchronous = true;
                    break;
                }
            }
        }

        String config = "loan-broker-" + provider + "-" + (synchronous ? "sync" : "async") + "-config.xml";
        return config;
    }

    protected void run(boolean synchronous) throws Exception
    {

        int response = 0;
        while (response != 'q')
        {
            System.out.println("\n" + new Message("loanbroker-example", 41).getMessage());

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
                    System.out.println(new Message("loanbroker-example", 22).getMessage());
                    int number = readInt();
                    if (number < 1)
                    {
                        System.out.println(new Message("loanbroker-example", 23).getMessage());
                    }
                    else
                    {
                        sendRandomRequests(number, synchronous);
                    }
                    break;
                }

                case 'q' :
                {
                    System.out.println(new Message("loanbroker-example", 14).getMessage());
                    close();
                    System.exit(0);
                }

                default :
                {
                    System.out.println(new Message("loanbroker-example", 15).getMessage());
                }
            }
        }
    }

    protected void sendRandomRequests(int number, boolean synchronous) throws Exception
    {
        if (synchronous)
        {
            List list = this.requestSend(number, "vm://customer.requests");
            int i = 1;
            for (Iterator iterator = list.iterator(); iterator.hasNext(); i++)
            {
                System.out.println(new Message("loanbroker-example", 24, String.valueOf(i), iterator.next().toString()).getMessage());
            }
        }
        else
        {
            this.requestDispatch(number, "vm://customer.requests");
        }
    }

}
