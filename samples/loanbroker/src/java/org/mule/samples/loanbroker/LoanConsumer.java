/*
 * $Id$
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
import org.mule.util.DateUtils;
import org.mule.util.StringMessageUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <code>LoanConsumer</code> is a loacn broker client app that uses command line prompts
 * to obtain loan requests
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class LoanConsumer
{

    private static boolean synchronous = false;

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
        Customer c = (Customer)customers.get(index);

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
        if (!sync) {
            client.dispatch("vm://LoanBrokerRequests", request, null);
            System.out.println("Sent Async request");
            // let the request catch up
            Thread.sleep(1500);
        }
        else {
            UMOMessage result = client.send("vm://LoanBrokerRequests", request, null);
            if (result == null) {
                System.out
                        .println("A result was not received, an error must have occurred. Check the logs.");
            }
            else {
                System.out.println("Loan Consumer received a Quote: " + result.getPayload());
            }
        }
    }

    public void requestDispatch(int number, String endpoint) throws Exception
    {
        for (int i = 0; i < number; i++) {
            client.dispatch(endpoint, createRequest(), null);
        }
    }

    public List requestSend(int number, String endpoint) throws Exception
    {
        List results = new ArrayList(number);
        UMOMessage result;
        for (int i = 0; i < number; i++) {
            result = client.send(endpoint, createRequest(), null);
            if (result != null) {
                results.add(result.getPayload());
            }
        }
        return results;
    }

    public static void main(String[] args)
    {
        LoanConsumer loanConsumer = null;

        try {
            if (args.length > 0) {
                loanConsumer = new LoanConsumer(args[0]);

                int i = 100;
                if (args.length > 1) {
                    i = Integer.parseInt(args[1]);
                }

                if (args.length > 2) {
                    synchronous = Boolean.valueOf(args[2]).booleanValue();
                }

                if (synchronous) {
                    long start = System.currentTimeMillis();
                    List results = loanConsumer.requestSend(i, "vm://LoanBrokerRequests");
                    System.out.println("Number or quotes received: " + results.size());
                    List output = new ArrayList(results.size());
                    int x = 1;
                    for (Iterator iterator = results.iterator(); iterator.hasNext(); x++) {
                        LoanQuote quote = (LoanQuote)iterator.next();
                        output.add(x + ". " + quote.toString());
                    }
                    System.out.println(StringMessageUtils.getBoilerPlate(output, '*', 80));
                    long cur = System.currentTimeMillis();
                    System.out.println(DateUtils.getFormattedDuration(cur - start));
                    System.out.println("Avg request: " + ((cur - start) / x));
                }
                else {
                    loanConsumer.requestDispatch(i, "vm://LoanBrokerRequests");
                }
            }
            else {
                String config = getConfig();
                loanConsumer = new LoanConsumer(config);
                loanConsumer.run(synchronous);
            }

        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    protected static String getConfig() throws IOException
    {
        System.out.println(StringMessageUtils.getBoilerPlate("Welcome to the Mule Loan Broker example"));

        int response = 0;
        String provider = "axis";

        while (response != 'a' && /* response != 'g' && */response != 'x') {
            // System.out.println("\nWhich SOAP stack would you like to use: [a]xis,
            // [g]lue or [x]fire?");
            System.out.println("\nWhich SOAP stack would you like to use: [a]xis or [x]fire?");
            response = readCharacter();
            switch (response) {
                case 'a' : {
                    provider = "axis";
                    break;
                }

                // TODO re-enable glue when the locahost/IP issue is fixed
                // case 'g':
                // {
                // provider = "glue";
                // break;
                // }

                case 'x' : {
                    provider = "xfire";
                    break;
                }
            }
        }

        response = 0;
        while (response != 'a' && response != 's') {
            System.out.println("\nWould you like to run the [s]ynchronous or [a]synchronous version?");
            response = readCharacter();
            switch (response) {
                case 'a' : {
                    System.out.println("Loading Asynchronous Loan Broker");
                    synchronous = false;
                    break;
                }

                case 's' : {
                    System.out.println("Loading Synchronous Loan Broker");
                    synchronous = true;
                    break;
                }
            }
        }

        String config = "loan-broker-" + provider + "-" + (synchronous ? "sync" : "async")
                + "-config.xml";
        return config;
    }

    protected void run(boolean synchronous) throws Exception
    {

        int response = 0;
        while (response != 'q') {
            System.out.println("\n[1] make a loan request");
            System.out.println("[2] send 100 random requests");
            System.out.println("[3] send x requests");
            System.out.println("[q] quit");
            System.out.println("\nPlease make your selection: ");

            response = readCharacter();

            switch (response) {
                case '1' : {
                    LoanRequest request = getRequestFromUser();
                    request(request, synchronous);
                    break;
                }

                case '2' : {
                    sendRandomRequests(100, synchronous);
                    break;
                }

                case '3' : {
                    System.out.println("Enter number of requests: ");
                    int number = readInt();
                    if (number < 1) {
                        System.out.println("Number of requests must be at least 1");
                    }
                    else {
                        sendRandomRequests(number, synchronous);
                    }
                    break;
                }

                case 'q' : {
                    System.out.println("Exiting now.");
                    close();
                    System.exit(0);
                }

                default : {
                    System.out.println("That response is not recognised, try again.");
                }
            }
        }
    }

    protected void sendRandomRequests(int number, boolean synchronous) throws Exception
    {
        if (synchronous) {
            List list = this.requestSend(number, "vm://LoanBrokerRequests");
            int i = 1;
            for (Iterator iterator = list.iterator(); iterator.hasNext(); i++) {
                System.out.println("Request " + i + ": " + iterator.next().toString());
            }
        }
        else {
            this.requestDispatch(number, "vm://LoanBrokerRequests");
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
        try {
            return Integer.parseInt(readString());
        }
        catch (NumberFormatException nfex) {
            return 0;
        }
    }

    protected static LoanRequest getRequestFromUser() throws IOException
    {
        System.out.println("Enter your name:");
        String name = readString();

        System.out.println("Enter loan Amount:");
        String amount = readString();

        System.out.println("Enter loan Duration in months:");
        String duration = readString();

        int d = 0;
        try {
            d = Integer.parseInt(duration);
        }
        catch (NumberFormatException e) {
            System.out.println("Failed to parse duration: " + duration + ". Using random default");
            d = getRandomDuration();
        }

        double a = 0;
        try {
            a = Double.valueOf(amount).doubleValue();
        }
        catch (NumberFormatException e) {
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
