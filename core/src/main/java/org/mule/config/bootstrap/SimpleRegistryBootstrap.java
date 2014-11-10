/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.bootstrap;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;

/**
 * Provides a hook to bootstrap a {@link MuleContext} during context initialization.
 */
public class SimpleRegistryBootstrap implements Initialisable, MuleContextAware
{

    protected MuleContext context;

    private BootstrapArtifactType supportedBootstrapArtifactType = BootstrapArtifactType.APP;

    /**
     * {@inheritDoc}
     */
    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    public void initialise() throws InitialisationException
    {
        RegistryBootstrapService registryBootstrapService = context.getRegistryBootstrapService();

        try
        {
            if (registryBootstrapService != null)
            {
                registryBootstrapService.bootstrap(context, supportedBootstrapArtifactType);
            }
        }
        catch (BootstrapException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    /**
     * This attributes define which types or registry bootstrap entries will be
     * created depending on the entry applyToArtifactType parameter value.
     *
     * @param supportedBootstrapArtifactType type of the artifact to support.
     */
    public void setSupportedBootstrapArtifactType(BootstrapArtifactType supportedBootstrapArtifactType)
    {
        this.supportedBootstrapArtifactType = supportedBootstrapArtifactType;
    }
}
