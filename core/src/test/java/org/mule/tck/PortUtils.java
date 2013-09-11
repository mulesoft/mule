/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Utility class to find available ports.
 */
public class PortUtils
{
    final static private int MIN_PORT = 5000;
    final static private int MAX_PORT = 6000;
    final static Log logger = LogFactory.getLog(PortUtils.class);
    /**
     * Find a given number of available ports
     * 
     * @param numberOfPorts The number of free ports to find
     * @return an List with the number of requested ports
     */
    public static List<Integer> findFreePorts(int numberOfPorts)
    {
        List<Integer> freePorts = new ArrayList<Integer>();
        for (int port = MIN_PORT; freePorts.size() != numberOfPorts && port < MAX_PORT; ++port)
        {
            if (isPortFree(port))
            {
                freePorts.add(port);
            }
        }

        if (freePorts.size() != numberOfPorts)
        {
            logger.info("requested " + numberOfPorts + " open ports, but returning " + freePorts.size());
        }
        return freePorts;
    }
    
    /**
     * Iterate through the ports and log whether each is available
     * @param failIfTaken If true, fails the current test if the port is not available
     * @throws Exception 
     */
    public static void checkPorts(boolean failIfTaken, String prefix, List<Integer> ports) throws Exception
    {
        for (Integer port : ports)
        {
            if (isPortFree(port))
            {
                logger.info(prefix + " port is free : " + port);
            }
            else
            {
                logger.info(prefix + " port is not free : " + port);
                if (failIfTaken)
                {
                    throw new Exception("port is not free : " + port);
                }
            }
        }
    }

    /**
     * Check and log is a given port is available
     * 
     * @param port the port number to check
     * @return true if the port is available, false otherwise
     */
    public static boolean isPortFree(int port)
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
