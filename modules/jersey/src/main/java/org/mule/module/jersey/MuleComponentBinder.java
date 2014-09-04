/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import org.mule.api.MuleContext;
import org.mule.api.component.JavaComponent;
import org.mule.component.BindingUtils;

import java.util.List;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

public class MuleComponentBinder extends AbstractBinder
{

    private final List<JavaComponent> components;
    private final MuleContext muleContext;

    public MuleComponentBinder(MuleContext muleContext, List<JavaComponent> components)
    {
        this.muleContext = muleContext;
        this.components = components;
    }

    @Override
    protected void configure()
    {
        for (JavaComponent component : components)
        {
            try
            {
                Object instance = component.getObjectFactory().getInstance(muleContext);
                BindingUtils.configureBinding(component, instance);
                bind(instance).to((Class<? super Object>) instance.getClass());
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
