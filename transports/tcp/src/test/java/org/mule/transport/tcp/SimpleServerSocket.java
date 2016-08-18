/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SimpleServerSocket implements Runnable
{

    private ServerSocket server;
    private AtomicBoolean running = new AtomicBoolean(true);
    private AtomicInteger count = new AtomicInteger(0);
    private String response;

    protected final transient Log logger = LogFactory.getLog(this.getClass());

    public SimpleServerSocket(int port, String response) throws Exception
    {
        server = new ServerSocket();
        logger.debug("starting server");
        server.bind(new InetSocketAddress("localhost", port), 3);
        this.response = response;
    }

    public int getCount()
    {
        return count.get();
    }

    @Override
    public void run()
    {
        try
        {
            running.set(true);
            while (running.get())
            {
                Socket socket = server.accept();
                logger.error("have connection " + count);
                count.incrementAndGet();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Writer out = new OutputStreamWriter(socket.getOutputStream());

                String s;
                while ((s = in.readLine()) != null) {
                    System.out.println(s);
                    if (s.isEmpty()) {
                        break;
                    }
                }

                logger.error("writing reply ");
                out.write(response);
                out.close();
                in.close();
                socket.close();
            }
        }
        catch (Exception e)
        {
            // an exception is expected during shutdown
            if (running.get())
            {
                throw new RuntimeException(e);
            }
        }
    }

    public void close()
    {
        try
        {
            running.set(false);
            server.close();
        }
        catch (Exception e)
        {
            // no-op
        }
    }
}
