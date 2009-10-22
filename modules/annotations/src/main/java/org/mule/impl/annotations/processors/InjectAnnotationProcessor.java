/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.impl.annotations.processors;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.ObjectProcessor;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes the JSR-330 {@link javax.inject.Inject} annotation. This can be used for injecting objects in the registry into
 * user object/beans. THis annotation injects by type. To inject by key use the {@link javax.inject.Named} annotation.
 */
public class InjectAnnotationProcessor implements ObjectProcessor, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(InjectAnnotationProcessor.class);

    private MuleContext context;

    public InjectAnnotationProcessor()
    {
    }

    public InjectAnnotationProcessor(MuleContext context)
    {
        this.context = context;
    }

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
        //TODO should maybe use the Annotation scanner here...
        Field[] fields;
        try
        {
            fields = object.getClass().getDeclaredFields();
        }
        catch (NoClassDefFoundError e)
        {
            //Only log the warning when debugging
            if (logger.isDebugEnabled())
            {
                logger.warn(e.toString());
            }
            return object;
        }
        for (int i = 0; i < fields.length; i++)
        {
            Field field = fields[i];
            if (field.isAnnotationPresent(Inject.class))
            {
                try
                {
                    field.setAccessible(true);
                    Object value;

                    value = context.getRegistry().lookupObject(field.getType());
                    if (value == null)
                    {
                        //TODO proper handling
                        throw new RuntimeException("Required object not found in registry of Type: " + field.getType());
                    }

                    field.set(object, value);
                }
                catch (RuntimeException e)
                {
                    //TODO proper handling
                    throw e;
                }
                catch (Exception e)
                {
                    //TODO proper handling
                    throw new RuntimeException(e);
                }
            }
        }
        return object;
    }
}
