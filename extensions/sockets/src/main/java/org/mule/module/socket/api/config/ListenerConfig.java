/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.socket.api.config;

import org.mule.module.socket.api.provider.tcp.TcpListenerProvider;
import org.mule.module.socket.api.provider.udp.UdpListenerProvider;
import org.mule.module.socket.api.source.SocketListener;
import org.mule.module.socket.api.worker.SocketWorker;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.param.Optional;

/**
 * Config for listener sockets
 *
 * @since 4.0
 */
@Configuration(name = "listener-config")
@Providers({TcpListenerProvider.class, UdpListenerProvider.class})
@Sources({SocketListener.class})
public class ListenerConfig
{

    /**
     * Used by the {@link SocketListener} for the creation and execution of the {@link SocketWorker}
     */
    @Parameter
    @Optional
    private ThreadingProfile threadingProfile;

    public ThreadingProfile getThreadingProfile()
    {
        return threadingProfile;
    }
}
