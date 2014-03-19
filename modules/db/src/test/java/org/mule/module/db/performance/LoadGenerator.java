/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.performance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadGenerator
{

    public static final int DEFAULT_THREADS = 20;
    public static final int DEFAULT_MESSAGE_PER_THREAD = 200;
    public static final int DEFAULT_MESSAGE_DELAY = 50;

    protected static final Log logger = LogFactory.getLog(LoadGenerator.class);
    private final int threadCount;
    private final int messagesPerThread;
    private final int messageDelay;

    public LoadGenerator()
    {
        this(DEFAULT_THREADS, DEFAULT_MESSAGE_PER_THREAD, DEFAULT_MESSAGE_DELAY);
    }

    public LoadGenerator(int threadCount, int messagesPerThread, int messageDelay)
    {
        this.threadCount = threadCount;
        this.messagesPerThread = messagesPerThread;
        this.messageDelay = messageDelay;
    }

    public void generateLoad(final LoadTask loadTask) throws InterruptedException, ExecutionException
    {
        Collection<Callable<Integer>> solvers = new ArrayList<Callable<Integer>>(getThreadCount());
        for (int i = 1; i <= getThreadCount(); i++)
        {
            solvers.add(new Callable<Integer>()
            {
                public Integer call() throws Exception
                {

                    for (int message = 1; message <= getMessagesPerThread(); message++)
                    {
                        try
                        {

                            loadTask.execute(message);
                        }
                        catch (Exception e)
                        {
                            // Ignore and continue
                            logger.error("Error sending message: " + e.getMessage());
                        }
                        Thread.sleep(getMessageDelay());
                    }

                    return getMessagesPerThread();
                }
            });
        }
        ExecutorService exec = Executors.newFixedThreadPool(getThreadCount());

        CompletionService<Integer> executorCompletionService = new ExecutorCompletionService<Integer>(exec);
        for (Callable<Integer> s : solvers)
        {
            executorCompletionService.submit(s);
        }

        Integer count = 0;

        for (int i = 0; i < getThreadCount(); ++i)
        {
            count = count + executorCompletionService.take().get();
            logger.info("Current row processed count: " + count);
        }

        logger.info("Load generation completed");
    }

    public int getThreadCount()
    {
        return threadCount;
    }

    public int getMessagesPerThread()
    {
        return messagesPerThread;
    }

    public int getMessageDelay()
    {
        return messageDelay;
    }
}
