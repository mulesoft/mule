/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.FileUtils;
import org.mule.util.queue.DualRandomAccessFileQueueStoreDelegate;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class VmPerformanceTest extends AbstractMuleContextTestCase
{

    @Before
    public void clearTemp()
    {
        FileUtils.deleteTree(new File("temp"));
    }
    //1 - queueFile.write(..) 15908
    //2 - ByteArrayOutputStream 7664
    //3 - ByteBuffer 6154
    @Test
    public void test() throws Exception
    {
        DualRandomAccessFileQueueStoreDelegate pepe = new DualRandomAccessFileQueueStoreDelegate("pepe", "temp", muleContext, 0);
        long initialTime = System.currentTimeMillis();
        for (int i = 0; i < 1000000; i++)
        {
            //System.out.println(i);
            pepe.offer("data".getBytes(), 1, 10);
        }
        System.out.println("total time: " + (System.currentTimeMillis() - initialTime));
    }

    //3 - 6714
    @Test
    public void test1() throws Exception
    {
        final DualRandomAccessFileQueueStoreDelegate pepe = new DualRandomAccessFileQueueStoreDelegate("pepe", "temp", muleContext, 0);
        processConcurrentTest("data".getBytes(), new Callback()
        {
            @Override
            public void execute(Object data)
            {
                try
                {
                    pepe.offer((Serializable) data, 0, 10);
                }
                catch (InterruptedException e)
                {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    //9558
    //9801
    @Test
    public void test2() throws Exception
    {
        OutboundEndpoint outboundEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint("vm://out");
        outboundEndpoint.getConnector().start();
        MuleEvent data = getTestEvent("data");
        long initialTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++)
        {
            //System.out.println(i);
            outboundEndpoint.process(data);
        }
        System.out.println("total time: " + (System.currentTimeMillis() - initialTime));
    }


    //1 - 41482
    //2 - 35031
    //3 - 32530
    //best number 33 seconds
    @Test
    public void test3() throws Exception
    {
        muleContext.getEndpointFactory().getOutboundEndpoint("vm://out").getConnector().start();
        final OutboundEndpoint outboundEndpoint = muleContext.getEndpointFactory().getOutboundEndpoint("vm://out");
        Callback callback = new Callback()
        {
            @Override
            public void execute(Object data)
            {
                try
                {
                    outboundEndpoint.process((MuleEvent) data);
                }
                catch (MuleException e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
        MuleEvent data = getTestEvent("data");
        processConcurrentTest(data,callback);
    }

    private void processConcurrentTest(Object data, Callback callback) throws MuleException, InterruptedException
    {
        List<Thread> threads = new ArrayList<Thread>();
        long initialTime = System.currentTimeMillis();
        for (int i = 0; i < 100; i++)
        {
            Thread thread = createThread(callback, 10000, data);
            threads.add(thread);
            thread.start();
        }
        for (Thread thread : threads)
        {
            thread.join();
        }
        System.out.println("total time: " + (System.currentTimeMillis() - initialTime));
    }


    public interface Callback {

        void execute(Object data);
    }

    public Thread createThread(final Callback callback, final int  times, final Object data)
    {
        return new Thread()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < times; i++)
                {
                    try
                    {
                        callback.execute(data);
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
    }

    @Override
    public int getTestTimeoutSecs()
    {
        return 900000;
    }

}
