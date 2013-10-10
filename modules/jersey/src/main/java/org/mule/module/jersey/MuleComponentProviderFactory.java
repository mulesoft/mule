/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.jersey;

import org.mule.api.MuleContext;
import org.mule.api.component.JavaComponent;
import org.mule.api.object.ObjectFactory;
import org.mule.component.BindingUtils;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProvider;
import com.sun.jersey.core.spi.component.ioc.IoCComponentProviderFactory;
import com.sun.jersey.core.spi.component.ioc.IoCInstantiatedComponentProvider;

import java.util.List;

public class MuleComponentProviderFactory implements IoCComponentProviderFactory
{

    private final List<JavaComponent> components;
    private final MuleContext muleContext;

    public MuleComponentProviderFactory(MuleContext muleContext, List<JavaComponent> components)
    {
        this.muleContext = muleContext;
        this.components = components;
    }

    public IoCComponentProvider getComponentProvider(Class<?> cls)
    {
        for (JavaComponent c : components)
        {
            if (c.getObjectType().isAssignableFrom(cls))
            {
                return getComponentProvider(null, cls);
            }
        }
        return null;
    }

    public IoCComponentProvider getComponentProvider(ComponentContext ctx, final Class<?> cls)
    {
        final JavaComponent selected = getSelectedComponent(cls);
        
        if (selected == null)
        {
            return null;
        }
        
        return new IoCInstantiatedComponentProvider()
        {
            public Object getInjectableInstance(Object o)
            {
                return o;
            }

            public Object getInstance()
            {
                try
                {
                    ObjectFactory objectFactory = selected.getObjectFactory();
                    Object instance = objectFactory.getInstance(muleContext);
                    BindingUtils.configureBinding(selected, instance);

                    return instance;
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    private JavaComponent getSelectedComponent(final Class<?> cls)
    {
        JavaComponent selected = null;
        for (JavaComponent c : components)
        {
            if (c.getObjectType().isAssignableFrom(cls))
            {
                selected = c;
            }
        }
        return selected;
    }

}
