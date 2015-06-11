/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import org.mule.api.MuleException;
import org.mule.api.context.WorkManager;
import org.mule.api.context.WorkManagerSource;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.providers.grizzly.HttpTransactionContext;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.strategies.AbstractIOStrategy;

/**
 * Grizzly IO Strategy that will handle work using a Mule {@link org.mule.api.context.WorkManager}.
 */
public class FlowWorkManagerIOStrategy extends AbstractIOStrategy
{

    private static final FlowWorkManagerIOStrategy INSTANCE = new FlowWorkManagerIOStrategy();

    private final static EnumSet<IOEvent> WORKER_THREAD_EVENT_SET =
            EnumSet.of(IOEvent.READ, IOEvent.CLOSED);

    private static final Logger logger = Grizzly.logger(FlowWorkManagerIOStrategy.class);

    protected FlowWorkManagerIOStrategy()
    {
        // Use getInstance() to obtain singleton instance.
    }

    @Override
    public boolean executeIoEvent(final Connection connection,
                                  final IOEvent ioEvent, final boolean isIoEventEnabled)
            throws IOException

    {
        final boolean isReadOrWriteEvent = isReadWrite(ioEvent);

        final IOEventLifeCycleListener listener;
        if (isReadOrWriteEvent)
        {
            if (isIoEventEnabled)
            {
                connection.disableIOEvent(ioEvent);
            }

            listener = ENABLE_INTEREST_LIFECYCLE_LISTENER;
        }
        else
        {
            listener = null;
        }

        final Executor threadPool = getThreadPoolFor(connection, ioEvent);
        if (threadPool != null)
        {
            threadPool.execute(
                    new WorkerThreadRunnable(connection, ioEvent, listener));
        }
        else
        {
            run0(connection, ioEvent, listener);
        }

        return true;
    }

    @Override
    public Executor getThreadPoolFor(Connection connection, IOEvent ioEvent)
    {
        if (WORKER_THREAD_EVENT_SET.contains(ioEvent))
        {
            try
            {
                WorkManager workManager = getWorkManager(connection);
                if (workManager != null)
                {
                    return workManager;
                }
            }
            catch (MuleException e)
            {
                // ignore exception, log warning and fallback to using WorkerIOStrategy
            }
            if(logger.isLoggable(Level.FINE))
            {
                logger.fine("Unable to obtain Mule WorkManager instance for worker thread IO. Grizzly " +
                            "WorkerIOStrategy will be used instead.");
            }
            return connection.getTransport().getWorkerThreadPool();
        }
        else
        {
            // Run other types of IOEvent in selector thread.
            return null;
        }
    }

    protected WorkManager getWorkManager(Connection connection) throws MuleException
    {
        HttpTransactionContext httpTransactionContext = (HttpTransactionContext) connection.getAttributes().getAttribute
                (HttpTransactionContext.class.getName());

        if (httpTransactionContext != null && httpTransactionContext.getAsyncHandler() instanceof WorkManagerSource)
        {
            return ((WorkManagerSource) httpTransactionContext.getAsyncHandler()).getWorkManager();
        }
        else
        {
            return null;
        }
    }

    private static void run0(final Connection connection,
                             final IOEvent ioEvent,
                             final IOEventLifeCycleListener lifeCycleListener)
    {

        fireIOEvent(connection, ioEvent, lifeCycleListener, logger);

    }

    private static final class WorkerThreadRunnable implements Runnable
    {

        final Connection connection;
        final IOEvent ioEvent;
        final IOEventLifeCycleListener lifeCycleListener;

        private WorkerThreadRunnable(final Connection connection,
                                     final IOEvent ioEvent,
                                     final IOEventLifeCycleListener lifeCycleListener)
        {
            this.connection = connection;
            this.ioEvent = ioEvent;
            this.lifeCycleListener = lifeCycleListener;
        }

        @Override
        public void run()
        {
            run0(connection, ioEvent, lifeCycleListener);
        }
    }

    public static FlowWorkManagerIOStrategy getInstance()
    {
        return INSTANCE;
    }

}