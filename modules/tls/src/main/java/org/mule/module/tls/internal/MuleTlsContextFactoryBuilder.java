/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.tls.internal;

import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.tls.TlsContextFactory;
import org.mule.api.tls.TlsContextFactoryBuilder;
import org.mule.module.tls.api.DefaultTlsContextFactoryBuilder;

@DefaultTlsContextFactoryBuilder
public class MuleTlsContextFactoryBuilder implements TlsContextFactoryBuilder, Initialisable, MuleContextAware
{

    private TlsContextFactory defaultTlsContextFactory;
    private MuleContext muleContext;

    /**
     * Creates a default {@link TlsContextFactory} and registers it under key
     * {@link MuleProperties#DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY}
     *
     * @throws InitialisationException if the {@link #defaultTlsContextFactory} could not be created or registered
     */
    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            defaultTlsContextFactory = new DefaultTlsContextFactory();
            muleContext.getRegistry().registerObject(MuleProperties.DEFAULT_TLS_CONTEXT_FACTORY_REGISTRY_KEY, defaultTlsContextFactory);
        }
        catch (Exception e)
        {
            throw new InitialisationException(createStaticMessage("Failed to create default " + TlsContextFactory.class.getSimpleName()), e, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TlsContextFactory buildDefault()
    {
        return defaultTlsContextFactory;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }
}