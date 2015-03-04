/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.processors;

import org.mule.api.MuleContext;
import org.mule.api.annotations.expressions.Lookup;
import org.mule.api.context.MuleContextAware;
import org.mule.api.expression.RequiredValueException;
import org.mule.api.registry.InjectProcessor;
import org.mule.config.i18n.AnnotationsMessages;
import org.mule.util.StringUtils;

import java.lang.reflect.Field;

import javax.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @deprecated as of 3.7.0. Use a registry which supports {@link Inject} annotations instead
 */
@Deprecated
public class LookupInjectionProcessor implements InjectProcessor, MuleContextAware
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(LookupInjectionProcessor.class);

    private MuleContext context;

    public LookupInjectionProcessor()
    {
    }

    public LookupInjectionProcessor(MuleContext context)
    {
        this.context = context;
    }

    public void setMuleContext(MuleContext context)
    {
        this.context = context;
    }

    public Object process(Object object)
    {
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
            if (field.isAnnotationPresent(Lookup.class))
            {
                try
                {
                    field.setAccessible(true);
                    Object value;
                    String name = field.getAnnotation(Lookup.class).value();
                    boolean optional = field.getAnnotation(Lookup.class).optional();
                    if(StringUtils.isBlank(name))
                    {
                        value = context.getRegistry().lookupObject(field.getType());
                    }
                    else
                    {
                        value = context.getRegistry().lookupObject(name);
                    }
                    if (value == null && !optional)
                    {
                        throw new RequiredValueException(AnnotationsMessages.lookupNotFoundInRegistry(field.getType(), name, object.getClass()));
                    }

                    field.set(object, value);
                }
                catch (RequiredValueException e)
                {
                    throw e;
                }
                catch (Exception e)
                {
                    throw new RequiredValueException(AnnotationsMessages.lookupFailedSeePreviousException(object), e);
                }
            }
        }
        return object;
    }
}
