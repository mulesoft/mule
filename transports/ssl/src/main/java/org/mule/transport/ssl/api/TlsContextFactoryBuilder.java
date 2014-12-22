/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.ssl.api;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.registry.RegistrationException;
import org.mule.transport.ssl.DefaultTlsContextFactory;

/**
 * Builder for TlsContextFactory.
 */
public class TlsContextFactoryBuilder
{

    private static final String DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY = "_muleDefaultTlsContextFactory";
    private final MuleContext muleContext;

    public TlsContextFactoryBuilder(final MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    /**
     * @return a {@link org.mule.transport.ssl.api.TlsContextFactory} with the jvm default certificates.
     */
    public TlsContextFactory buildDefault()
    {
        synchronized (muleContext)
        {
            TlsContextFactory tlsContextFactory = muleContext.getRegistry().get(DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY);
            if (tlsContextFactory == null)
            {
                tlsContextFactory = new DefaultTlsContextFactory();
                try
                {
                    muleContext.getRegistry().registerObject(DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY, tlsContextFactory);
                }
                catch (RegistrationException e)
                {
                    throw new MuleRuntimeException(e);
                }
            }
            return tlsContextFactory;
        }
    }
}
