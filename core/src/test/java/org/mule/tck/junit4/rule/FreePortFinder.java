/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.junit4.rule;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Finds available port numbers in a specified range.
 */
public class FreePortFinder
{

    protected final Log logger = LogFactory.getLog(getClass());

    private final int minPortNumber;
    private final int portRange;
    private final Set<Integer> selectedPorts = new HashSet<Integer>();
    private final Random random  = new Random();

    public FreePortFinder(int minPortNumber, int maxPortNumber)
    {
        this.minPortNumber = minPortNumber;
        this.portRange = maxPortNumber - minPortNumber;
    }

    public synchronized Integer find()
    {
        for (int i = 0; i < portRange; i++)
        {
            int port = minPortNumber + random.nextInt(portRange);

            if (selectedPorts.contains(port))
            {
                continue;
            }

            if (isPortFree(port))
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Found free port: " + port);
                }

                selectedPorts.add(port);

                return port;
            }
        }

        throw new IllegalStateException("Unable to find an available port");
    }

    /**
     * Indicates that the port is free from the point of view of the caller.
     * <p/>
     * Checks that the port was released, if it was not, then it would be
     * marked as in use, so no other client receives the same port again.
     *
     * @param port the port number to release.
     */
    public synchronized void releasePort(int port)
    {
        if (isPortFree(port))
        {
            selectedPorts.remove(port);
        }
        else
        {
            if (logger.isInfoEnabled())
            {
                logger.info(String.format("Port %d was is not correctly released", port));
            }
        }
    }

    /**
     * Check and log is a given port is available
     *
     * @param port the port number to check
     * @return true if the port is available, false otherwise
     */
    public boolean isPortFree(int port)
    {
        boolean portIsFree = true;

        ServerSocket server = null;
        try
        {
            server = new ServerSocket(port);
        }
        catch (IOException e)
        {
            portIsFree = false;
        }
        finally
        {
            if (server != null)
            {
                try
                {
                    server.close();
                }
                catch (IOException e)
                {
                    // ignore
                }
            }
        }

        return portIsFree;
    }
}
