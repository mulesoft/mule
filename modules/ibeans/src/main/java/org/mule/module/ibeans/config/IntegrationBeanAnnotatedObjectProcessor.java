/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
