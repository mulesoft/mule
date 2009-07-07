/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.agent.Agent;
import org.mule.api.config.ConfigurationException;
import org.mule.api.transformer.Transformer;
import org.mule.api.transport.Connector;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.ObjectNameHelper;
import org.mule.util.scan.ClasspathScanner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;

import java.util.Iterator;
import java.util.Set;


/**
 * Provides the configuration entry point for loading Guice modules into Mule.  Users can pass in an array of
 * {@link Module} into this builder or provide a base search path that Mule will use to search the
 * classpath for any modules that implement {@link Module}. The basepath is a path on the classpath.
 * Note for better performance, any basepath set should be qualified to your application. For example, if your application
 * has a package com.mycompany.app, its better to set the base path to 'com/mycompany/app' over 'com/' or '/' since they will
 * seach everything on the classpath that matches the specified package.
 */
public class GuiceConfigurationBuilder extends AbstractConfigurationBuilder
{
    private String basepath = "";

    private Module[] modules = null;

    private Stage stage;

    private ClassLoader classLoader;

    public GuiceConfigurationBuilder()
    {
        super();
        classLoader = Thread.currentThread().getContextClassLoader();
    }

    public GuiceConfigurationBuilder(ClassLoader classLoader)
    {
        this.classLoader = classLoader;
    }

    public GuiceConfigurationBuilder(String basepath)
    {
        this();
        this.basepath = basepath;
    }

    public GuiceConfigurationBuilder(String basepath, ClassLoader classLoader)
    {
        this.basepath = basepath;
        this.classLoader = classLoader;
    }

    public GuiceConfigurationBuilder(Module... modules)
    {
        this.modules = modules;
    }

    public GuiceConfigurationBuilder(Stage stage, Module... modules)
    {
        this.stage = stage;
        this.modules = modules;
    }

    protected void doConfigure(MuleContext muleContext) throws Exception
    {
        Injector injector;
        if (basepath != null && basepath.startsWith("/"))
        {
            basepath = basepath.substring(1);
        }

        if (modules == null)
        {
            ClasspathScanner scanner = new ClasspathScanner(classLoader, new String[]{basepath});
            Set<Class> classes = scanner.scanFor(Module.class);
            Set<Class> factories = scanner.scanFor(GuiceModuleFactory.class);

            //TODO add the ability to do excludes
            classes.remove(AbstractMuleGuiceModule.class);

            if (classes.size() == 0 && factories.size() == 0)
            {
                try
                {
                    basepath = getClass().getClassLoader().getResources(basepath).toString();
                }
                catch (Exception e)
                {
                    basepath = (basepath.equals("") ? "/" : basepath);
                }
                //lets just log a noticable exception as a warning since the Guice build can compliment other configuration builders
                logger.warn(new ConfigurationException(CoreMessages.createStaticMessage("There are no Guice modules or module factories on the classpath under: " + basepath)));
                return;
            }

            modules = new Module[classes.size() + factories.size()];
            int i = 0;
            for (Class moduleClass : classes)
            {
                Module module = (Module) ClassUtils.instanciateClass(moduleClass, ClassUtils.NO_ARGS);
                modules[i++] = module;
            }
            for (Class factoryClass : factories)
            {
                GuiceModuleFactory factory = (GuiceModuleFactory) ClassUtils.instanciateClass(factoryClass, ClassUtils.NO_ARGS);
                modules[i++] = factory.createModule();
            }
        }

        for (int i = 0; i < modules.length; i++)
        {
            Module module = modules[i];
            if (module instanceof AbstractMuleGuiceModule)
            {
                ((AbstractMuleGuiceModule) module).setMuleContext(muleContext);
            }
        }
        if (stage != null)
        {
            injector = Guice.createInjector(stage, modules);
        }
        else
        {
            injector = Guice.createInjector(modules);
        }
        GuiceRegistry registry = new GuiceRegistry(injector);
        muleContext.addRegistry(2, registry);

        for (Iterator<Key<?>> iterator = injector.getBindings().keySet().iterator(); iterator.hasNext();)
        {
            Key key = iterator.next();
            if (Connector.class.isAssignableFrom(key.getTypeLiteral().getRawType()))
            {
                Connector c = (Connector) injector.getInstance(key);
                c.setName(new ObjectNameHelper(muleContext).getConnectorName(c));
                muleContext.getRegistry().registerConnector(c);
            }
            else if (Agent.class.isAssignableFrom(key.getTypeLiteral().getRawType()))
            {
                Agent a = (Agent) injector.getInstance(key);
                muleContext.getRegistry().registerAgent(a);
            }
            else if (Transformer.class.isAssignableFrom(key.getTypeLiteral().getRawType()))
            {
                Transformer t = (Transformer) injector.getInstance(key);
                muleContext.getRegistry().registerTransformer(t);
            }
            //TODO EndpointBuilders
            //TODO Security Providers

        }
        registry.initialise();
    }
}
