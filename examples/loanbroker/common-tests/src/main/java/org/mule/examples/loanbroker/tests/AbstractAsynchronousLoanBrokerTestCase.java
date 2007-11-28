/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.examples.loanbroker.tests;

import org.mule.examples.loanbroker.messages.Customer;
import org.mule.examples.loanbroker.messages.CustomerQuoteRequest;
import org.mule.examples.loanbroker.messages.LoanQuote;
import org.mule.extras.client.MuleClient;
import org.mule.providers.NullPayload;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.util.ExceptionHolder;
import org.mule.util.StringMessageUtils;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;

import java.beans.ExceptionListener;

import org.apache.commons.lang.time.StopWatch;

/**
 * Tests the Loan Broker application asynchronously.  Note that a simple thread delay is used to wait for the
 * incoming responses to arrive.  This may or may not be sufficient depending on external factors (processor
 * speed, logging detail, etc.).  To make the tests reliable, a more accurate mechanism should be employed
 * (notifications, thread-safe counter, etc.)
 */
public abstract class AbstractAsynchronousLoanBrokerTestCase extends AbstractLoanBrokerTestCase
{
    // @Override
    protected int getNumberOfRequests()
    {
        return 100;
    }

    /** Milliseconds to wait after sending each message in order for the thread to "catch up" with the test. */
    protected int getDelay()
    {
        return 10000;
    }

    protected int getWarmUpMessages()
    {
        return 50;
    }

    public void testSingleLoanRequest() throws Exception
    {
        MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest request = new CustomerQuoteRequest(c, 100000, 48);
        // Send asynchronous request
        client.dispatch("CustomerRequests", request, null);

        // Wait for asynchronous response
        UMOMessage result = client.request("CustomerResponses", getDelay());
        assertNotNull("Result is null", result);
        assertFalse("Result is null", result.getPayload() instanceof NullPayload);
        assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                result.getPayload() instanceof LoanQuote);
        LoanQuote quote = (LoanQuote) result.getPayload();
        assertTrue(quote.getInterestRate() > 0);
    }

    public void testLotsOfLoanRequests() throws Exception
    {
        final MuleClient client = new MuleClient();
        Customer c = new Customer("Ross Mason", 1234);
        CustomerQuoteRequest[] requests = new CustomerQuoteRequest[3];
        requests[0] = new CustomerQuoteRequest(c, 100000, 48);
        requests[1] = new CustomerQuoteRequest(c, 1000, 12);
        requests[2] = new CustomerQuoteRequest(c, 10, 24);

        final StopWatch stopWatch = new StopWatch();

        final int numRequests = getNumberOfRequests() + getWarmUpMessages();
        int i = 0;

        int numberOfThreads = 1;

        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        ExceptionHolder exceptionHolder = new ExceptionHolder();
        try
        {
            for (int x = 0; x < numberOfThreads; x++)
            {
                Thread thread = new Thread(new ClientReceiver(latch, numRequests / numberOfThreads, exceptionHolder));
                thread.start();
            }


            for (i = 0; i < numRequests; i++)
            {
                if (i == getWarmUpMessages())
                {
                    stopWatch.start();
                }
                client.dispatch("CustomerRequests", requests[i % 3], null);
            }
        }
        finally
        {
            latch.await();
            stopWatch.stop();
            System.out.println("Total running time was: " + stopWatch.getTime() + "ms");
            System.out.println("Requests processed was: " + i);
            int mps = (int) (numRequests / ((double) stopWatch.getTime() / (double) 1000));
            System.out.println("Msg/sec: " + mps + " (warm up msgs = " + getWarmUpMessages() + ")");
            if(exceptionHolder.isExceptionThrown())
            {
                exceptionHolder.print();
                fail("Exceptions thrown during async processing");
            }
        }
    }

    private class ClientReceiver implements Runnable
    {

        private CountDownLatch latch;
        private int numberOfRequests;
        private ExceptionListener exListener;

        public ClientReceiver(CountDownLatch latch, int numberOfRequests, ExceptionListener exListener)
        {
            this.latch = latch;
            this.numberOfRequests = numberOfRequests;
            this.exListener = exListener;

        }

        public void run()
        {
            int i = 0;
            try
            {
                Thread.sleep(2000);
                MuleClient client = new MuleClient();
                UMOMessage result = null;
                for ( i = 0; i < numberOfRequests; i++)
                {
                    try
                    {
                        result = client.request("CustomerResponses", 600);
                    }
                    catch (UMOException e)
                    {
                        exListener.exceptionThrown(e);
                        break;
                    }
                    //System.out.println("Received: " + i);
                    assertNotNull("Result is null", result);
                    assertFalse("Result is null", result.getPayload() instanceof NullPayload);
                    assertTrue("Result should be LoanQuote but is " + result.getPayload().getClass().getName(),
                            result.getPayload() instanceof LoanQuote);
                    LoanQuote quote = (LoanQuote) result.getPayload();
                    assertTrue(quote.getInterestRate() > 0);
                }
            }
            catch (Throwable e)
            {
                //e.printStackTrace();
                System.out.println(StringMessageUtils.getBoilerPlate("Processed Messages=" + i));
                if(e instanceof Error)
                {
                    //throw (Error)e;
                    exListener.exceptionThrown(new Exception(e));
                }
                else
                {
                    exListener.exceptionThrown((Exception)e);
                }
            }
            finally
            {
                latch.countDown();
            }
        }
    }
}
