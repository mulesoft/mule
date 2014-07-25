/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.guice;

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.scan.ClasspathScanner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.guiceyfruit.mule.MuleModule;


/**
 * Provides the configuration entry point for loading Guice modules into Mule.  Users can pass in an array of
 * {@link Module} into this builder or provide a base search path that Mule will use to search the
 * classpath for any modules that implement {@link Module}. The basepath is a path on the classpath.
 * Note for better performance, any basepath set should be qualified to your application. For example, if your application
 * has a package com.mycompany.app, its better to set the base path to 'com/mycompany/app' over 'com/' or '/' since they will
 * search everything on the classpath that matches the specified package.
 *
 * @deprecated Guice module is deprecated and will be removed in Mule 4.
 */
@Deprecated
public class GuiceConfigurationBuilder extends AbstractConfigurationBuilder
{
    public static final String DEFAULT_PACKAGE = "";

    protected String basepath = DEFAULT_PACKAGE;

    protected Module[] modules = null;

    protected Stage stage;

    protected ClassLoader classLoader;

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

    @Override
    protected void doConfigure(MuleContext muleContext) throws Exception
    {

        List<Module> allModules = getSystemModules(muleContext);

        Injector injector;
        if (basepath != null && basepath.startsWith("/"))
        {
            basepath = basepath.substring(1);
        }

        //No modules were set explicitly on this ConfigurationBuilder so we now try and discover
        //modules and {@link GuiceModuleFactory} instances on the classpath
        if (modules == null)
        {
            ClasspathScanner scanner = new ClasspathScanner(classLoader, basepath);
            Set<Class<Module>> classes = scanner.scanFor(Module.class);
            Set<Class<GuiceModuleFactory>> factories = scanner.scanFor(GuiceModuleFactory.class);

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
                //lets just log a noticeable exception as a warning since the Guice build can compliment other configuration builders
                logger.warn(new ConfigurationException(CoreMessages.createStaticMessage("There are no Guice modules or module factories on the classpath under: " + basepath)));
                return;
            }

            for (Class<Module> moduleClass : classes)
            {
                allModules.add(ClassUtils.instanciateClass(moduleClass, ClassUtils.NO_ARGS));
            }
            for (Class<GuiceModuleFactory> factoryClass : factories)
            {
                GuiceModuleFactory factory = ClassUtils.instanciateClass(factoryClass, ClassUtils.NO_ARGS);
                allModules.add(factory.createModule());
            }
        }
        else
        {
            allModules.addAll(Arrays.asList(modules));
        }

        for (Module module : allModules)
        {
            if (module instanceof AbstractMuleGuiceModule)
            {
                ((AbstractMuleGuiceModule) module).setMuleContext(muleContext);
            }
        }

        if (stage != null)
        {
            injector = Guice.createInjector(stage, allModules);
        }
        else
        {
            injector = Guice.createInjector(allModules);
        }
        
        GuiceRegistry registry = new GuiceRegistry(injector, muleContext);
        registry.initialise();
        muleContext.addRegistry(registry);
    }

    protected List<Module> getSystemModules(MuleContext muleContext)
    {
        List<Module> systemModules = new ArrayList<Module>();
        //JSR-250 lifecycle and @Resource annotation support & Mule lifecycle support
        systemModules.add(new MuleModule());
        systemModules.add(new MuleSupportModule(muleContext));
        return systemModules;
    }
}
