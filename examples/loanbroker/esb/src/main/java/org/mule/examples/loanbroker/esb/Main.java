/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.esb;

import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.config.i18n.Message;
import org.mule.examples.loanbroker.AbstractMain;
import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOMessage;

import java.io.IOException;

import org.apache.activemq.broker.BrokerService;

/**
 * <code>Main</code> Executes the LoanBroker ESB application
 */

public class Main extends AbstractMain
{
    private MuleClient client = null;
    private BrokerService msgBroker = null;

    public Main(String config) throws Exception
    {
        // Start up the ActiveMQ message broker.
        msgBroker = new BrokerService();
        msgBroker.addConnector("tcp://localhost:61616");
        msgBroker.start();

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure(config, null);
        client = new MuleClient();
    }

    public void close() throws Exception
    {
        client.dispose();
        if (msgBroker != null)
        {
            msgBroker.stop();
        }
    }

    public UMOMessage request(CustomerQuoteRequest request) throws Exception
    {
        return client.send("vm://customer.requests", request, null);
    }

    public static void main(String[] args)
    {
        Main loanConsumer = null;
        int response = 0;
        try
        {
            System.out.println("******************\n"
                + new Message("loanbroker-example", 30).getMessage()
                + "\n******************");
            response = readCharacter();
            if (response == '1')
            {
                System.out.println(new Message("loanbroker-example", 31).getMessage());
                loanConsumer = new Main("loan-broker-esb-mule-config.xml");
            }
            else if (response == 'q')
            {
                System.out.println(new Message("loanbroker-example", 32).getMessage());
                System.exit(0);
            }
            else
            {
                System.out.println(new Message("loanbroker-example", 33).getMessage());
                loanConsumer = new Main("loan-broker-esb-mule-config-with-ejb-container.xml");
            }

            while (response != 'q')
            {
                System.out.println("\n" + new Message("loanbroker-example", 34).getMessage());

                response = readCharacter();
                if (response == '1')
                {
                    CustomerQuoteRequest request = getRequestFromUser();
                    UMOMessage result = loanConsumer.request(request);
                    if (result == null)
                    {
                        System.out.println(new Message("loanbroker-example", 12).getMessage());
                    }
                    else
                    {
                        System.out.println(new Message("loanbroker-example", 13, result.getPayload()).getMessage());
                    }
                }
                else if (response == 'q')
                {
                    System.out.println(new Message("loanbroker-example", 14).getMessage());
                    loanConsumer.close();
                    System.exit(0);
                }
                else
                {
                    System.out.println(new Message("loanbroker-example", 15).getMessage());
                }
            }

        }
        catch (Exception e)
        {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

}
