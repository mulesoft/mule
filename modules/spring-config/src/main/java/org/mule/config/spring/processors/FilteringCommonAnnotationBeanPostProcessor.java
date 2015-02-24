/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import java.beans.PropertyDescriptor;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.context.annotation.CommonAnnotationBeanPostProcessor;

/**
 * A subclass of {@link CommonAnnotationBeanPostProcessor} which ignores objects from
 * a specific set of packages
 *
 * @since 3.7.0
 */
public class FilteringCommonAnnotationBeanPostProcessor extends CommonAnnotationBeanPostProcessor
{

    private final Set<String> filteredPackages;

    public FilteringCommonAnnotationBeanPostProcessor(Set<String> filteredPackages)
    {
        this.filteredPackages = filteredPackages;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException
    {
        String packageName = bean.getClass().getName();
        for (String filteredPackage : filteredPackages)
        {
            if (packageName.startsWith(filteredPackage))
            {
                return pvs;
            }
        }

        return super.postProcessPropertyValues(pvs, pds, bean, beanName);
    }
}
