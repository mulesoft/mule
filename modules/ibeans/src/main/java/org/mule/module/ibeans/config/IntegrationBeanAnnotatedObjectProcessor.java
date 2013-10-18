/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.InjectProcessor;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.util.Set;

import org.ibeans.annotation.IntegrationBean;

public class IntegrationBeanAnnotatedObjectProcessor implements InjectProcessor, MuleContextAware
{
    private MuleContext muleContext;
    private MuleIBeansPlugin plugin;

    public IntegrationBeanAnnotatedObjectProcessor()
    {
        super();
    }

    public IntegrationBeanAnnotatedObjectProcessor(MuleContext muleContext)
    {
        this();
        setMuleContext(muleContext);
    }

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
        this.plugin = new MuleIBeansPlugin(context);
    }

    public Object process(Object object)
    {
        Set<AnnotationMetaData> annos = AnnotationUtils.getFieldAnnotationsForHierarchy(object.getClass(), IntegrationBean.class);

        for (AnnotationMetaData data : annos)
        {
            Field field = (Field) data.getMember();
            field.setAccessible(true);
            try
            {
                if (field.get(object) != null)
                {
                    continue;
                }
            }
            catch (IllegalAccessException e)
            {
                continue;
            }
            IBeanBinding binding = createBinding(field.getType().getSimpleName());
            binding.setInterface(field.getType());
            Object proxy = binding.createProxy(new Object());
            try
            {
                field.set(object, proxy);
            }
            catch (IllegalAccessException e)
            {
                throw new RuntimeException("Failed to create IntegrationBean proxy for: " + field.getType(), e);
            }
        }
        return object;
    }

    protected IBeanBinding createBinding(String name)
    {
        IBeanFlowConstruct flow = new IBeanFlowConstruct(name + ".ibean", muleContext);
        try
        {
            muleContext.getRegistry().registerObject(flow.getName(), flow, FlowConstruct.class);
        }
        catch (MuleException e)
        {
            throw new MuleRuntimeException(e);
        }
        return new IBeanBinding(flow, muleContext, plugin);
    }
}
