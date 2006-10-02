/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.samples.loanbroker.esb;

import java.io.IOException;

import org.activemq.broker.BrokerContainer;
import org.activemq.broker.impl.BrokerContainerImpl;
import org.mule.MuleManager;
import org.mule.config.builders.MuleXmlConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.samples.loanbroker.esb.message.Customer;
import org.mule.samples.loanbroker.esb.message.CustomerQuoteRequest;
import org.mule.umo.UMOMessage;
import org.mule.util.StringMessageUtils;

/**
 * <code>Main</code> Executes the LoanBroker ESB application
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class Main
 {
    private MuleClient client = null;
    private BrokerContainer msgBroker = null;

    public Main(String config) throws Exception {
        // Start up the ActiveMQ message broker.
        msgBroker = new BrokerContainerImpl("ActiveMQ");
        msgBroker.addConnector("tcp://localhost:61616");
        msgBroker.start();

        MuleXmlConfigurationBuilder builder = new MuleXmlConfigurationBuilder();
        builder.configure(config, null);
        client = new MuleClient();
    }

    public void close() throws Exception {
        MuleManager.getInstance().dispose();
        if (msgBroker != null) {
            msgBroker.stop();
        }
    }

    private static double getRandomAmount() {
        return Math.round(Math.random() * 18000);
    }

    private static int getRandomDuration() {
        return new Double(Math.random() * 60).intValue();
    }

    public UMOMessage request(CustomerQuoteRequest request) throws Exception {
         return client.send("vm://loan.broker.requests", request, null);
    }

    public static void main(String[] args) {
        Main loanConsumer = null;
        int response = 0;
        try {
                 System.out.println("******************"+ 
			"\nWelcome to the Mule Loan Broker ESB example. This example demonstrates using JMS, Web Services,\n Http/Rest and EJBs using an ESB architecture." +
                        "\nFor more information see http://mule.mulesource.org/LoanBroker." +
                        "\n\nThe example demonstrates integrating EJB applications in 2 ways -" +
                        "\n  1. Calling out to a remote EJB using a Mule Endpoint." +
                        "\n  2. Managing an EJB as a Mule component." +
                        "\n\nBoth have the same behavior but the second method allows a remote EJB to be used as if it were\n a local Mule component, thereby enabling tighter integration." +
                        "\n\nPlease select [1], [2] or [q]uit"+
			"\n******************");



                response = getSelection();
                if (response == '1')
                {
                    System.out.println("Loading 'Ejb via an Endpoint' version");
                    loanConsumer = new Main("loan-broker-esb-mule-config.xml");
                } else if(response == 'q')
                {
                    System.out.println("Bye");
                    System.exit(0);
                } else
                {
                    System.out.println("Loading 'Managed Ejb Component' version");
                    loanConsumer = new Main("loan-broker-esb-mule-config-with-ejb-container.xml");
                }

            while (response != 'q') {
                System.out.println("\n[1] make a loan request");
                System.out.println("[q] quit");
                System.out.println("\nPlease make your selection: ");

                response = getSelection();
                if (response == '1') {
                    CustomerQuoteRequest request = getRequestFromUser();
                    UMOMessage result = loanConsumer.request(request);
                    if (result == null) {
                        System.out.println("A result was not received, an error must have occurred. Check the logs.");
                    } else {
                        System.out.println("Loan Consumer received a Quote: " + result.getPayload());
                    }
                } else if (response == 'q') {
                    System.out.println("Exiting now");
                    loanConsumer.close();
                    System.exit(0);
                } else {
                    System.out.println("That response is not recognised, try again:");
                }
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static int getSelection() throws IOException {
        byte[] buf = new byte[16];
        System.in.read(buf);
        return buf[0];
    }

    private static CustomerQuoteRequest getRequestFromUser() throws IOException {
        byte[] buf = new byte[128];
        System.out.println("Enter your name:");
        System.in.read(buf);
        String name = new String(buf).trim();
        System.out.println("Enter loan Amount:");
        buf = new byte[16];
        System.in.read(buf);
        String amount = new String(buf).trim();
        System.out.println("Enter loan Duration in months:");
        buf = new byte[16];
        System.in.read(buf);
        String duration = new String(buf).trim();

        int d = 0;
        try {
            d = Integer.parseInt(duration);
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse duration: " + duration + ". Using random default");
            d = getRandomDuration();
        }

        double a = 0;
        try {
            a = Double.valueOf(amount).doubleValue();
        } catch (NumberFormatException e) {
            System.out.println("Failed to parse amount: " + amount + ". Using random default");
            a = getRandomAmount();
        }

        Customer c = new Customer(name, getRandomSsn());
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, a, d);
        return request;
    }

    private static int getRandomSsn() {
        return new Double(Math.random() * 6000).intValue();
    }
}
