/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener.grizzly;

import org.mule.module.http.internal.listener.ServerAddress;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.Grizzly;
import org.glassfish.grizzly.IOEvent;
import org.glassfish.grizzly.IOEventLifeCycleListener;
import org.glassfish.grizzly.strategies.AbstractIOStrategy;

/**
 * Grizzly IO Strategy that will handle each work to an specific {@link java.util.concurrent.Executor}
 * based on the {@link org.mule.module.http.internal.listener.ServerAddress} of a {@link org.glassfish.grizzly.Connection}.
 *
 * There's logic from {@link org.glassfish.grizzly.strategies.WorkerThreadIOStrategy} that need to be reused but unfortunately
 * that class cannot be override.
 */
public class ExecutorPerServerAddressIOStrategy extends AbstractIOStrategy
{

    private static final Logger logger = Grizzly.logger(ExecutorPerServerAddressIOStrategy.class);
    private final ExecutorProvider executorProvider;

    public ExecutorPerServerAddressIOStrategy(final ExecutorProvider executorProvider)
    {
        this.executorProvider = executorProvider;
    }

    /**
     * Method copied form {@link org.glassfish.grizzly.strategies.WorkerThreadIOStrategy}
     */
    @Override
    public boolean executeIoEvent(final Connection connection,
                                  final IOEvent ioEvent, final boolean isIoEventEnabled)
            throws IOException
    {

        final boolean isReadOrWriteEvent = isReadWrite(ioEvent);

        final IOEventLifeCycleListener listener;
        if (isReadOrWriteEvent) {
            if (isIoEventEnabled) {
                connection.disableIOEvent(ioEvent);
            }

            listener = ENABLE_INTEREST_LIFECYCLE_LISTENER;
        } else {
            listener = null;
        }

        final Executor threadPool = getThreadPoolFor(connection, ioEvent);
        if (threadPool != null) {
            threadPool.execute(
                    new WorkerThreadRunnable(connection, ioEvent, listener));
        } else {
            run0(connection, ioEvent, listener);
        }

        return true;
    }

    @Override
    public Executor getThreadPoolFor(Connection connection, IOEvent ioEvent)
    {
        final String hostName = ((InetSocketAddress) connection.getLocalAddress()).getHostName();
        final int port = ((InetSocketAddress) connection.getLocalAddress()).getPort();
        return executorProvider.getExecutorFor(new ServerAddress(hostName, port));
    }

    /**
     * Method copied form {@link org.glassfish.grizzly.strategies.WorkerThreadIOStrategy}
     */
    private static void run0(final Connection connection,
                             final IOEvent ioEvent,
                             final IOEventLifeCycleListener lifeCycleListener) {

        fireIOEvent(connection, ioEvent, lifeCycleListener, logger);

    }

    /**
     * Class copied form {@link org.glassfish.grizzly.strategies.WorkerThreadIOStrategy}
     */
    private static final class WorkerThreadRunnable implements Runnable {
        final Connection connection;
        final IOEvent ioEvent;
        final IOEventLifeCycleListener lifeCycleListener;

        private WorkerThreadRunnable(final Connection connection,
                                     final IOEvent ioEvent,
                                     final IOEventLifeCycleListener lifeCycleListener) {
            this.connection = connection;
            this.ioEvent = ioEvent;
            this.lifeCycleListener = lifeCycleListener;
        }

        @Override
        public void run() {
            run0(connection, ioEvent, lifeCycleListener);
        }
    }

}