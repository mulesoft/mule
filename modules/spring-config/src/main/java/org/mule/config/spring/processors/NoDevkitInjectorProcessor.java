/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.util.ClassUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.PropertyValues;

/**
 * A {@link SelectiveInjectorProcessor} used to keep
 * mule from injecting dependencies into components
 * built with Devkit, by skipping injection of objects
 * which are instances of {@code org.mule.api.devkit.ProcessAdapter}
 * or {@code org.mule.modules.process.ProcessAdapter}
 *
 * @since 3.7.0
 */
public class NoDevkitInjectorProcessor extends SelectiveInjectorProcessor
{

    private static final String PROCESS_ADAPTER_CLASS_NAME = "org.mule.api.devkit.ProcessAdapter";
    private static final String LEGACY_PROCESS_ADAPTER_CLASS_NAME = "org.mule.modules.process.ProcessAdapter";

    private final Class<?>[] excludedClasses;

    public NoDevkitInjectorProcessor()
    {
        List<Class<?>> presentClasses = new ArrayList<>();
        fetchExcludedClassByName(PROCESS_ADAPTER_CLASS_NAME, presentClasses);
        fetchExcludedClassByName(LEGACY_PROCESS_ADAPTER_CLASS_NAME, presentClasses);

        excludedClasses = new Class<?>[presentClasses.size()];
        presentClasses.toArray(excludedClasses);
    }

    @Override
    protected boolean shouldInject(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName)
    {
        for (Class<?> excludedClass : excludedClasses)
        {
            if (excludedClass.isInstance(bean))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * fetch classes by name to avoid circular dependency between spring module
     * and devkit support. If the class is in fact present, then it's
     * added to {@code presentClasses}
     */
    private void fetchExcludedClassByName(String name, List<Class<?>> presentClasses)
    {
        try
        {
            Class<?> excludedClass = ClassUtils.loadClass(name, getClass());
            if (excludedClass != null)
            {
                presentClasses.add(excludedClass);
            }
        }
        catch (ClassNotFoundException e)
        {
            // silently continue
        }
    }
}
