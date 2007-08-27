/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.issues;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Can we avoid the "address already in use" errors by using SO_REUSEADDR?
 *
 * Typical results are
<pre>
 [07-24 19:32:49] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats without reuse and a pause of 100 ms
 [07-24 19:33:49] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 57.3 +/- 33.15131973240282
 [07-24 19:33:49] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats with reuse and a pause of 100 ms
 [07-24 19:35:32] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 100.0 +/- 0.0
 [07-24 19:35:32] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats without reuse and a pause of 10 ms
 [07-24 19:35:48] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 96.8 +/- 7.332121111929359
 [07-24 19:35:48] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats with reuse and a pause of 10 ms
 [07-24 19:36:04] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 100.0 +/- 0.0
 [07-24 19:36:04] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats without reuse and a pause of 1 ms
 [07-24 19:36:10] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 75.8 +/- 37.690317058894586
 [07-24 19:36:10] INFO  ReuseExperimentMule2067TestCase [main]: Measuring average run length for 100 repeats with reuse and a pause of 1 ms
 [07-24 19:36:18] INFO  ReuseExperimentMule2067TestCase [main]: Average run length: 100.0 +/- 0.0
</pre>
 * which suggest that enabling address re-use could help with the issue.
 *
 * Note that if a single socket (ie a single port number) is reused for all tests we often
 * zeroes eveywhere (even with waits of 2sec and similar between iterations/tests).  This
 * suggests that once the error occurs, the socket enters a long-lived "broken" state.
 *
 * All this is by AC on linux, dual CPU, Java 1.4 - I suspect results will vary like crazy
 * in different contexts.
 */
public class ReuseExperimentMule2067TestCase extends TestCase
{

    private static final int NO_WAIT = -1;
    private static final int PORT = 65432;
    private static boolean NO_REUSE = false;
    private static boolean REUSE = true;

    private Log logger = LogFactory.getLog(getClass());

    public void testReuse() throws IOException
    {
        repeatOpenCloseClientServer(1000, 10, PORT, 1, REUSE, false); // fails, but less often?
        repeatOpenCloseClientServer(100, 10, PORT, 1, NO_REUSE, false); // intermittent
    }

    public void testMeasureImprovement() throws IOException
    {
        measureMeanRunLength(10, 100, 10, PORT, 100, NO_REUSE);
        measureMeanRunLength(10, 100, 10, PORT+10, 100, REUSE);
        measureMeanRunLength(10, 100, 10, PORT+20, 10, NO_REUSE);
        measureMeanRunLength(10, 100, 10, PORT+30, 10, REUSE);
        measureMeanRunLength(10, 100, 10, PORT+40, 1, NO_REUSE);
        measureMeanRunLength(10, 100, 10, PORT+50, 1, REUSE);
    }

    protected void measureMeanRunLength(int sampleSize, int numberOfRepeats, int numberOfConnections,
                                        int port, long pause,  boolean reuse)
            throws IOException
    {
        logger.info("Measuring average run length for " + numberOfRepeats + " repeats " +
                (reuse ? "with" : "without") + " reuse and a pause of " + pause + " ms");
        int totalLength = 0;
        long totalLengthSquared = 0;
        for (int i = 0; i < sampleSize; ++i)
        {
            int length = repeatOpenCloseClientServer(numberOfRepeats, numberOfConnections, port+i, pause, reuse, true);
            totalLength += length;
            totalLengthSquared += length * length;
        }
        double mean = totalLength / (double) sampleSize;
        double sd = Math.sqrt(totalLengthSquared / (double) sampleSize - mean * mean);
        logger.info("Average run length: " + mean + " +/- " + sd);
    }

    protected int repeatOpenCloseClientServer(int numberOfRepeats, int numberOfConnections, int port,
                                              long pause, boolean reuse, boolean noFail)
            throws IOException
    {
        String message = "Repeating openCloseClientServer with pauses of " + pause + " ms "
                    + (reuse ? "with" : "without") + " reuse";
        if (noFail)
        {
            logger.debug(message);
        }
        else
        {
            logger.info(message);
        }
        for (int i = 0; i < numberOfRepeats; i++)
        {
            if (0 != i)
            {
                pause(pause);
            }
            try
            {
                openCloseClientServer(numberOfConnections, port, reuse);
            }
            catch (BindException e)
            {
                if (noFail && e.getMessage().indexOf("Address already in use") > -1)
                {
                    return i;
                }
                throw e;
            }
        }
        return numberOfRepeats;
    }

    protected void openCloseClientServer(int numberOfConnections, int port, boolean reuse)
            throws IOException
    {
        Server server = new Server(port, reuse);
        try {
            new Thread(server).start();
            for (int i = 0; i < numberOfConnections; i++)
            {
                logger.debug("opening socket " + i);
                Socket client = new Socket("localhost", port);
                client.close();
            }
        }
        finally
        {
            server.close();
        }
    }

    protected void pause(long pause)
    {
        if (pause != NO_WAIT)
        {
            try
            {
                synchronized(this)
                {
                    if (pause > 0)
                    {
                        this.wait(pause);
                    }
                }
            }
            catch (InterruptedException e)
            {
                // ignore
            }
        }
    }

    protected static class Server implements Runnable
    {

        private Log logger = LogFactory.getLog(getClass());
        private ServerSocket server;

        public Server(int port, boolean reuse) throws IOException
        {
            server = new ServerSocket();
            server.setReuseAddress(reuse);
            server.bind(new InetSocketAddress("localhost", port));
        }

        public void run()
        {
            try
            {
                while (true)
                {
                    Socket socket = server.accept();
                    socket.close();
                }
            }
            catch (Exception e)
            {
                logger.debug("Expected - dirty closedown: " + e);
            }
        }

        public void close() throws IOException
        {
            server.close();
            server = null;
        }
    }

}