/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl;

import org.mule.MuleServer;

/**
 * TODO
 */

/**
 * The shutdown thread used by the server when its main thread is terminated
 */
public class MuleShutdownHook extends Thread
{
    private MuleServer server;
    private Throwable exception = null;

    public MuleShutdownHook(MuleServer server)
    {
        super();
        this.server = server;
    }

    public MuleShutdownHook(MuleServer server, Throwable exception)
    {
        this(server);
        this.exception = exception;
    }

    /*
    * (non-Javadoc)
    *
    * @see java.lang.Runnable#run()
    */
    public void run()
    {

        if (server != null)
        {
            if (exception != null)
            {
                server.shutdown(exception);
            }
            else
            {
                server.shutdown();
            }
        }
    }


    public Throwable getException()
    {
        return exception;
    }

    public void setException(Throwable exception)
    {
        this.exception = exception;
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o != null && getClass().getName().equals(o.getClass().getName()))
        {
            return true;
        }

        return false;
    }

    public int hashCode()
    {
        return getClass().getName().hashCode();
    }
}

