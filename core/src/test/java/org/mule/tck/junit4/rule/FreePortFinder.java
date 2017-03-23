/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.junit4.rule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * Finds available port numbers in a specified range.
 */
public class FreePortFinder
{

    protected final Log logger = LogFactory.getLog(getClass());

    private final int minPortNumber;
    private final int portRange;
    private final Random random  = new Random();
    private final String LOCK_FILE_EXTENSION = ".lock";
    private final Map<Integer, FileLock> locks = new HashMap<>();

    public FreePortFinder(int minPortNumber, int maxPortNumber)
    {
        this.minPortNumber = minPortNumber;
        this.portRange = maxPortNumber - minPortNumber;
    }

    public synchronized Integer find()
    {
        for (int i = 0; i < portRange; i++)
        {
            try
            {
                int port = minPortNumber + random.nextInt(portRange);
                String portFile = String.valueOf(port) + LOCK_FILE_EXTENSION;
                FileChannel channel = FileChannel.open(Paths.get(portFile), CREATE, WRITE);
                FileLock lock = channel.tryLock();

                if (isPortFree(port))
                {
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("Found free port: " + port);
                    }

                    locks.put(port, lock);
                    return port;
                } else {
                    lock.release();
                }
            }
            catch(OverlappingFileLockException e)
            {
                // The file is locked,
                if (logger.isDebugEnabled())
                {
                    logger.debug("Port selected already locked");
                }
            }
            catch(IOException e)
            {
                if (logger.isDebugEnabled())
                {
                    logger.debug("Error when trying to open port lock file, trying another port");
                }
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
        if (isPortFree(port) && locks.containsKey(port))
        {
            FileLock lock = locks.get(port);
            try {
                lock.release();
            } catch (IOException e) {
                // Ignore
            }
        }
        else
        {
            if (logger.isInfoEnabled())
            {
                logger.info(String.format("Port %d was not correctly released", port));
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
            server.setReuseAddress(true);
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
                    // Ignore
                }
            }
        }

        return portIsFree;
    }
}
