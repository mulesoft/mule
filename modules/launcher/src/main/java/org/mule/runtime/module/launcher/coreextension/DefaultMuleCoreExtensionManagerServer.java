/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher.coreextension;

import org.mule.runtime.container.api.CoreExtensionsAware;
import org.mule.runtime.container.api.MuleCoreExtension;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.module.launcher.DeploymentListener;
import org.mule.runtime.module.launcher.DeploymentService;
import org.mule.runtime.module.launcher.DeploymentServiceAware;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMuleCoreExtensionManagerServer implements MuleCoreExtensionManagerServer
{

    protected static final Logger logger = LoggerFactory.getLogger(DefaultMuleCoreExtensionManagerServer.class);

    private final MuleCoreExtensionDiscoverer coreExtensionDiscoverer;
    private final MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver;
    private List<MuleCoreExtension> coreExtensions = new LinkedList<>();
    private DeploymentService deploymentService;
    private List<MuleCoreExtension> orderedCoreExtensions;

    public DefaultMuleCoreExtensionManagerServer(MuleCoreExtensionDiscoverer coreExtensionDiscoverer, MuleCoreExtensionDependencyResolver coreExtensionDependencyResolver)
    {
        this.coreExtensionDiscoverer = coreExtensionDiscoverer;
        this.coreExtensionDependencyResolver = coreExtensionDependencyResolver;
    }

    @Override
    public void dispose()
    {
        for (MuleCoreExtension extension : coreExtensions)
        {
            try
            {
                extension.dispose();
            }
            catch (Exception ex)
            {
                logger.error("Error disposing core extension " + extension.getName(), ex);
            }
        }
    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            coreExtensions = coreExtensionDiscoverer.discover();

            orderedCoreExtensions = coreExtensionDependencyResolver.resolveDependencies(coreExtensions);

            initializeCoreExtensions();

        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public void start() throws MuleException
    {
        logger.info("Starting core extensions");
        for (MuleCoreExtension extension : orderedCoreExtensions)
        {
            extension.start();
        }
    }

    @Override
    public void stop() throws MuleException
    {
        if (orderedCoreExtensions == null)
        {
            return;
        }

        for (int i = orderedCoreExtensions.size() - 1; i >= 0; i--)
        {
            MuleCoreExtension extension = orderedCoreExtensions.get(i);

            try
            {
                extension.stop();
            }
            catch (MuleException e)
            {
                logger.warn("Error stopping core extension: " + extension.getName(), e);
            }
        }
    }

    private void initializeCoreExtensions() throws InitialisationException, DefaultMuleException
    {
        logger.info("Initializing core extensions");

        for (MuleCoreExtension extension : orderedCoreExtensions)
        {
            if (extension instanceof DeploymentServiceAware)
            {
                ((DeploymentServiceAware) extension).setDeploymentService(deploymentService);
            }

            if (extension instanceof DeploymentListener)
            {
                deploymentService.addDeploymentListener((DeploymentListener) extension);
            }

            if (extension instanceof CoreExtensionsAware)
            {
                ((CoreExtensionsAware) extension).setCoreExtensions(orderedCoreExtensions);
            }

            extension.initialise();
        }
    }

    @Override
    public void setDeploymentService(DeploymentService deploymentService)
    {
        this.deploymentService = deploymentService;
    }
}
