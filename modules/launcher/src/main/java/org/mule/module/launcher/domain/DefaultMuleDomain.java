/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.launcher.domain;

import static org.mule.util.SplashScreen.miniSplash;

import org.mule.MuleServer;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationBuilder;
import org.mule.api.config.DomainMuleContextAwareConfigurationBuilder;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.config.builders.AutoConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.module.launcher.DeploymentInitException;
import org.mule.module.launcher.DeploymentListener;
import org.mule.module.launcher.DeploymentStartException;
import org.mule.module.launcher.DeploymentStopException;
import org.mule.module.launcher.MuleDeploymentService;
import org.mule.module.launcher.application.Application;
import org.mule.module.launcher.application.NullDeploymentListener;
import org.mule.module.launcher.artifact.ArtifactClassLoader;
import org.mule.module.launcher.artifact.MuleContextDeploymentListener;
import org.mule.module.launcher.descriptor.ApplicationDescriptor;
import org.mule.util.ClassUtils;
import org.mule.util.ExceptionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultMuleDomain implements Domain
{

    protected transient final Log logger = LogFactory.getLog(getClass());
    protected transient final Log deployLogger = LogFactory.getLog(MuleDeploymentService.class);

    private final static String DOMAIN_CONFIG_FILE_LOCATION = "mule-domain-config.xml";

    private final DomainClassLoaderRepository domainClassLoaderRepository;
    private final String name;
    private MuleContext muleContext;
    private DeploymentListener deploymentListener;
    private ArtifactClassLoader deploymentClassLoader;

    private File configResourceFile;

    public DefaultMuleDomain(DomainClassLoaderRepository domainClassLoaderRepository, String name)
    {
        this.domainClassLoaderRepository = domainClassLoaderRepository;
        this.deploymentListener = new NullDeploymentListener();
        this.name = name;
        this.deploymentClassLoader = domainClassLoaderRepository.getDomainClassLoader(name);
        URL resource = deploymentClassLoader.findLocalResource(this.DOMAIN_CONFIG_FILE_LOCATION);
        if (resource != null)
        {
            this.configResourceFile = new File(resource.getFile());
        }
    }

    public void setDeploymentListener(DeploymentListener deploymentListener)
    {
        this.deploymentListener = deploymentListener;
    }

    public String getName()
    {
        return name;
    }

    @Override
    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    @Override
    public ConfigurationBuilder createApplicationConfigurationBuilder(Application application) throws Exception
    {
        String configBuilderClassName = determineConfigBuilderClassNameForApplication(application);
        ConfigurationBuilder configurationBuilder = (ConfigurationBuilder) ClassUtils.instanciateClass(configBuilderClassName,
                                                                                                       new Object[] {application.getDescriptor().getAbsoluteResourcePaths()}, application.getArtifactClassLoader().getClassLoader());

        if (!containsSharedResources())
        {
            return configurationBuilder;
        }
        else
        {
            if (configurationBuilder instanceof DomainMuleContextAwareConfigurationBuilder)
            {
                ((DomainMuleContextAwareConfigurationBuilder) configurationBuilder).setDomainContext(getMuleContext());
            }
            else
            {
                throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("ConfigurationBuilder %s does not support domain context", configurationBuilder.getClass().getCanonicalName())));
            }
            return configurationBuilder;
        }
    }

    protected String determineConfigBuilderClassNameForApplication(Application defaultMuleApplication)
    {
        // Provide a shortcut for Spring: "-builder spring"
        final String builderFromDesc = defaultMuleApplication.getDescriptor().getConfigurationBuilder();
        if ("spring".equalsIgnoreCase(builderFromDesc))
        {
            return ApplicationDescriptor.CLASSNAME_SPRING_CONFIG_BUILDER;
        }
        else if (builderFromDesc == null)
        {
            return AutoConfigurationBuilder.class.getName();
        }
        else
        {
            return builderFromDesc;
        }
    }

    @Override
    public void install()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("New domain '%s'", getArtifactName())));
        }
    }


    @Override
    public void init()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Initializing domain '%s'", getArtifactName())));
        }

        try
        {
            if (this.configResourceFile != null)
            {
                validateConfigurationFileDoNotUsesCoreNamespace();

                ConfigurationBuilder cfgBuilder = createConfigurationBuilder();
                if (!cfgBuilder.isConfigured())
                {
                    List<ConfigurationBuilder> builders = new ArrayList<ConfigurationBuilder>(3);

                    // We need to add this builder before spring so that we can use Mule annotations in Spring or any other builder
                    addAnnotationsConfigBuilderIfPresent(builders);

                    builders.add(cfgBuilder);

                    DefaultMuleContextFactory muleContextFactory = new DefaultMuleContextFactory();
                    if (deploymentListener != null)
                    {
                        muleContextFactory.addListener(new MuleContextDeploymentListener(getArtifactName(), deploymentListener));
                    }
                    DomainMuleContextBuilder domainMuleContextBuilder = new DomainMuleContextBuilder(name);
                    muleContext = muleContextFactory.createMuleContext(builders, domainMuleContextBuilder);
                }
            }
        }
        catch (Exception e)
        {
            // log it here so it ends up in app log, sys log will only log a message without stacktrace
            logger.error(null, ExceptionUtils.getRootCause(e));
            throw new DeploymentInitException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
        }
    }

    private void validateConfigurationFileDoNotUsesCoreNamespace() throws FileNotFoundException
    {
        Scanner scanner = null;
        try
        {
            scanner = new Scanner(configResourceFile);
            while (scanner.hasNextLine())
            {
                final String lineFromFile = scanner.nextLine();
                if (lineFromFile.contains("<mule "))
                {
                    throw new MuleRuntimeException(CoreMessages.createStaticMessage("Domain configuration file can not be created using core namespace. Use mule-domain namespace instead."));
                }
            }
        }
        finally
        {
            if (scanner != null)
            {
                scanner.close();
            }
        }
    }

    protected void addAnnotationsConfigBuilderIfPresent(List<ConfigurationBuilder> builders) throws Exception
    {
        // If the annotations module is on the classpath, add the annotations config builder to
        // the list. This will enable annotations config for this instance.
        if (ClassUtils.isClassOnPath(MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, getClass()))
        {
            Object configBuilder = ClassUtils.instanciateClass(
                    MuleServer.CLASSNAME_ANNOTATIONS_CONFIG_BUILDER, ClassUtils.NO_ARGS, getClass());
            builders.add((ConfigurationBuilder) configBuilder);
        }
    }

    private ConfigurationBuilder createConfigurationBuilder()
    {
        try
        {
            return (ConfigurationBuilder) ClassUtils.instanciateClass("org.mule.config.spring.SpringXmlDomainConfigurationBuilder",
                                                                      new Object[] {getResourceFiles()[0].getName()}, deploymentClassLoader.getClassLoader());
        }
        catch (Exception e)
        {
            throw new MuleRuntimeException(e);
        }
    }

    @Override
    public void start()
    {
        try
        {
            if (this.muleContext != null)
            {
                try
                {
                    this.muleContext.start();
                }
                catch (MuleException e)
                {
                    logger.error(null, ExceptionUtils.getRootCause(e));
                    throw new DeploymentStartException(CoreMessages.createStaticMessage(ExceptionUtils.getRootCauseMessage(e)), e);
                }
            }
            // null CCL ensures we log at 'system' level
            // TODO create a more usable wrapper for any logger to be logged at sys level
            final ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader(null);
                deployLogger.info(miniSplash(String.format("Started domain '%s'", getArtifactName())));
            }
            finally
            {
                Thread.currentThread().setContextClassLoader(oldCl);
            }
        }
        catch (Exception e)
        {
            throw new DeploymentStartException(CoreMessages.createStaticMessage("Failure trying to start domain " + getArtifactName()), e);
        }
    }

    @Override
    public void stop()
    {
        try
        {
            if (logger.isInfoEnabled())
            {
                logger.info(miniSplash(String.format("Stopping domain '%s'", getArtifactName())));
            }
            if (this.muleContext != null)
            {
                this.muleContext.stop();
            }
        }
        catch (Exception e)
        {
            throw new DeploymentStopException(CoreMessages.createStaticMessage("Failure trying to stop domain " + getArtifactName()), e);
        }
    }

    @Override
    public void dispose()
    {
        if (logger.isInfoEnabled())
        {
            logger.info(miniSplash(String.format("Disposing domain '%s'", getArtifactName())));
        }
        if (this.muleContext != null)
        {
            this.muleContext.dispose();
        }
        this.deploymentClassLoader.dispose();
    }

    @Override
    public String getArtifactName()
    {
        return name;
    }

    @Override
    public File[] getResourceFiles()
    {
        return configResourceFile == null ? new File[0] : new File[] {configResourceFile};
    }

    @Override
    public ArtifactClassLoader getArtifactClassLoader()
    {
        return deploymentClassLoader;
    }

    public void initialise()
    {
        try
        {
            if (this.muleContext != null)
            {
                this.muleContext.initialise();
            }
        }
        catch (InitialisationException e)
        {
            throw new DeploymentInitException(CoreMessages.createStaticMessage("Failure trying to initialise domain " + getArtifactName()), e);
        }
    }

    public boolean containsSharedResources()
    {
        return this.muleContext != null;
    }
}
