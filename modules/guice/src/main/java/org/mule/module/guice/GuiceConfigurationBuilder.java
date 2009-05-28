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
import org.mule.api.config.ConfigurationException;
import org.mule.config.builders.AbstractConfigurationBuilder;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.ClassUtils;
import org.mule.util.scan.ClasspathScanner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * Provides the configuration entry point for loading Guice modules into Mule.  Users can pass in an array of
 * {@link com.google.inject.Module} into this builder or provide a base search path that Mule will use to search the
 * classpath for any modules that implement {@link com.google.inject.Module}. The basepath is a path on the classpath.
 * Note for better performance, any basepath set should be qualified to your application. For example, if your application
 * has a package com.mycompany.app, its better to set the base path to 'com/mycompany/app' over 'com/' or '/' since they will
 * seach everything on the classpath that matches the specified package.
 */
public class GuiceConfigurationBuilder extends AbstractConfigurationBuilder
{
    public static final String INJECTOR_OBJECT_NAME = "_guiceInjector";

    private String basepath = "";

    private Module[] modules = null;

    private Stage stage;

    public GuiceConfigurationBuilder()
    {
        super();
    }

    public GuiceConfigurationBuilder(String basepath)
    {
        this.basepath = basepath;
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
        //TODO how do people specify specific modules and stage for this configuration
        Injector injector;
        if(basepath!=null && basepath.startsWith("/"))
        {
            basepath = basepath.substring(1);
        }

        if (modules == null)
        {
            ClasspathScanner scanner = new ClasspathScanner(new String[]{basepath});
            Set<Class> classes = scanner.scanFor(Module.class);
            
            //TODO add the ability to do excludes
            classes.remove(AbstractMuleGuiceModule.class);

            if(classes.size()==0)
            {
                throw new ConfigurationException(CoreMessages.createStaticMessage("There are no Guice module objects on the classpath under: " + basepath));
            }
            
            modules = new Module[classes.size()];
            int i = 0;
            for (Class module : classes)
            {
                Module m = (Module) ClassUtils.instanciateClass(module, ClassUtils.NO_ARGS);
                modules[i++] = m;
            }
        }

        Map<String, Object> stringBindings = new HashMap<String, Object>();
        for (int i = 0; i < modules.length; i++)
        {
            Module module = modules[i];
            if (module instanceof AbstractMuleGuiceModule)
            {
                stringBindings.putAll(((AbstractMuleGuiceModule) module).getStringBindings());
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
        MuleInjectorImpl muleInjector = new MuleInjectorImpl(injector, stringBindings);
        GuiceRegistry registry = new GuiceRegistry(muleInjector);
        muleContext.addRegistry(2, registry);
        //Make this available in the registry so that we can use GuiceInjectorAware
        muleContext.getRegistry().registerObject(INJECTOR_OBJECT_NAME, muleInjector);

        //TODO We could wire mule system objects here
//        for (Map.Entry<String, Object> entry : stringBindings.entrySet())
//        {
//            if(entry.getValue() instanceof Class && Connector.class.isAssignableFrom(((Class)entry.getValue())))
//            {
//                Connector c = (Connector)muleInjector.getInstance(entry.getKey());
//                c.setName(entry.getKey());
//                c.setMuleContext(muleContext);
//                muleContext.getRegistry().registerConnector(c);
//            }
//        }
        registry.initialise();
    }
}
