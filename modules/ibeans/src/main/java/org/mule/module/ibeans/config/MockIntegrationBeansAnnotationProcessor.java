/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.ibeans.config;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.registry.InjectProcessor;
import org.mule.module.ibeans.spi.MuleIBeansPlugin;
import org.mule.util.annotation.AnnotationMetaData;
import org.mule.util.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import org.ibeans.annotation.MockIntegrationBean;
import org.ibeans.impl.test.MockIBean;
import org.ibeans.impl.test.MockIntegrationBeanInvocationHandler;
import org.mockito.Mockito;

/**
 * Will process any fields on an object with the {@link org.ibeans.annotation.MockIntegrationBean} annotation, inserting
 * a Mockito Mock object.  This is only used for testing.
 */
public class MockIntegrationBeansAnnotationProcessor implements InjectProcessor, MuleContextAware
{
    public static final String NAME = "_mockIntegrationBeanProcessor";

    private MuleIBeansPlugin plugin;

    public MockIntegrationBeansAnnotationProcessor()
    {
    }

    public void setMuleContext(MuleContext muleContext)
    {
        this.plugin = new MuleIBeansPlugin(muleContext);
    }

    public Object process(Object object)
    {
        List<AnnotationMetaData> annos = AnnotationUtils.getFieldAnnotations(object.getClass(), MockIntegrationBean.class);

        if (annos.size() > 0)
        {
            for (AnnotationMetaData data : annos)
            {
                Field field = (Field) data.getMember();
                field.setAccessible(true);
                Object mockito = Mockito.mock(field.getType(), field.getName());
                try
                {
                    //InvocationHandler handler = new MockIBeanHandler(field.getType(), muleContext, mockito);
                    InvocationHandler handler = new MockIntegrationBeanInvocationHandler(field.getType(), plugin, mockito);

                    Object mock = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{field.getType(), MockIBean.class}, handler);

                    field.set(object, mock);
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
        return object;
    }
}
