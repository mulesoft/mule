/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.request.grizzly;

import com.ning.http.client.providers.grizzly.TransportCustomizer;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.strategies.WorkerThreadIOStrategy;

/**
 * Transport customizer that sets the IO strategy to {@code SameThreadIOStrategy} and sets appropriate names
 * for the threads that are used.
 */
public class IOStrategyTransportCustomizer implements TransportCustomizer
{
    private static final String REQUESTER_WORKER_THREAD_NAME_SUFFIX = ".worker";

    private final String threadNamePrefix;

    public IOStrategyTransportCustomizer(String threadNamePrefix)
    {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public void customize(TCPNIOTransport transport, FilterChainBuilder filterChainBuilder)
    {
        transport.setIOStrategy(FlowWorkManagerIOStrategy.getInstance());
        transport.setWorkerThreadPoolConfig(WorkerThreadIOStrategy.getInstance().createDefaultWorkerPoolConfig(transport));

        transport.getKernelThreadPoolConfig().setPoolName(threadNamePrefix);
        transport.getWorkerThreadPoolConfig().setPoolName(threadNamePrefix + REQUESTER_WORKER_THREAD_NAME_SUFFIX);
    }
}
