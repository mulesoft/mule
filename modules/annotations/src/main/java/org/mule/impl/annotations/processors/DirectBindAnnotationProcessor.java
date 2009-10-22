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

import org.mule.api.MuleRuntimeException;
import org.mule.config.annotations.endpoints.Bind;
import org.mule.config.i18n.CoreMessages;
import org.mule.impl.endpoint.AnnotatedEndpointData;
import org.mule.impl.endpoint.MEP;
import org.mule.routing.binding.DefaultInterfaceBinding;
import org.mule.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * TODO
 */
public class DirectBindAnnotationProcessor extends AbstractAnnotationProcessor
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(DirectBindAnnotationProcessor.class);

    public Object process(Object object)
    {
        try
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
                if (field.isAnnotationPresent(Bind.class))
                {
                    Bind binding = field.getAnnotation(Bind.class);

                    AnnotatedEndpointData epd = new AnnotatedEndpointData(MEP.OutIn);
                    epd.setConnectorName(binding.connector());
                    epd.setAddress(binding.uri());

                    org.mule.api.routing.InterfaceBinding router = new DefaultInterfaceBinding();
                    router.setInterface(field.getType());
                    if (!StringUtils.isBlank(binding.method()))
                    {
                        router.setMethod(getValue(binding.method()));
                        for (int j = 0; j < object.getClass().getMethods().length; j++)
                        {
                            Method m = object.getClass().getMethods()[j];
                            if (m.getName().equals(router.getMethod()))
                            {
                                epd.setMEPUsingMethod(m, false);
                                break;
                            }
                        }
                    }
                    router.setEndpoint(builder.processEndpoint(epd));

                    field.setAccessible(true);
                    field.set(object, router.createProxy(object));
                }
            }
        }
        catch (Exception e)
        {
            //TODO i18n
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Failed to process @Bind annotation"), e);
        }

        return object;
    }
}
