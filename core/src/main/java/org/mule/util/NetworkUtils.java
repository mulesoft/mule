/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.util;

import java.net.Socket;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class NetworkUtils
{
    private static final Log logger = LogFactory.getLog(NetworkUtils.class);
    
    private NetworkUtils()
    {
        // utility class only
    }
    
    public static boolean isServerReachable(URL url, int timeout)
    {
        int port = url.getPort() != -1 ? url.getPort() : url.getDefaultPort();
        return isServerReachable(url.getHost(), port, timeout);
    }
    
    public static boolean isServerReachable(String host, int port, int timeout)
    {
        boolean isServerReachable = false;
        Socket socket = null;
        
        try
        {
            socket = TimedSocket.createSocket(host, port, timeout);
            isServerReachable = true;
        }
        catch (Exception e)
        {
            logger.debug("Server at " + host + ":" + port + " not reachable. " + e.getMessage());
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            }
            catch (Exception socketNotClosed)
            {
                logger.debug(socketNotClosed);
            }
        }

        return isServerReachable;
    }
}
