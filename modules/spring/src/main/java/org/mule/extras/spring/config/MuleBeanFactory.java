/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.spring.config;

import java.util.Iterator;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.MuleBeanDefinitionValueResolver;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.BeansException;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.BeanWrapperImpl;

/**
 * A workaround class to customize the {@link org.springframework.beans.factory.support.BeanDefinitionValueResolver}.
 * Currently there's no way to replace the implementation of this resolver, so some private methods
 * have to be opened and overridden.
 */
public class MuleBeanFactory extends DefaultListableBeanFactory {

    /**
     * Create a new MuleBeanFactory.
     */
    public MuleBeanFactory() {
        super();
    }

    /**
     * Create a new MuleBeanFactory with the given parent.
     * @param parentBeanFactory parent bean factory
     */
    public MuleBeanFactory(BeanFactory parentBeanFactory) {
        super(parentBeanFactory);
    }


    /**
	 * Apply the given property values, resolving any runtime references
	 * to other beans in this bean factory. Must use deep copy, so we
	 * don't permanently modify this property.
	 * @param beanName bean name passed for better exception information
	 * @param bw BeanWrapper wrapping the target object
	 * @param pvs new property values
	 */
	protected void applyPropertyValues(
			String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw, PropertyValues pvs)
			throws BeansException {

		if (pvs == null) {
			return;
		}

        /*
            This is the only change from the original version.
            See http://opensource.atlassian.com/projects/spring/browse/SPR-2285 for details.
         */
        MuleBeanDefinitionValueResolver valueResolver =
				new MuleBeanDefinitionValueResolver(this, beanName, mergedBeanDefinition);

		// Create a deep copy, resolving any references for values.
		MutablePropertyValues deepCopy = new MutablePropertyValues();
		PropertyValue[] pvArray = pvs.getPropertyValues();
		for (int i = 0; i < pvArray.length; i++) {
			PropertyValue pv = pvArray[i];
			Object resolvedValue =
					valueResolver.resolveValueIfNecessary("bean property '" + pv.getName() + "'", pv.getValue());
			deepCopy.addPropertyValue(pvArray[i].getName(), resolvedValue);
		}

		// Set our (possibly massaged) deep copy.
		try {
			// Synchronize if custom editors are registered.
			// Necessary because PropertyEditors are not thread-safe.
			if (!getCustomEditors().isEmpty()) {
				synchronized (this) {
					bw.setPropertyValues(deepCopy);
				}
			}
			else {
				bw.setPropertyValues(deepCopy);
			}
		}
		catch (BeansException ex) {
			// Improve the message by showing the context.
			throw new BeanCreationException(
					mergedBeanDefinition.getResourceDescription(), beanName, "Error setting property values", ex);
		}
	}


    /**
     * Unchanged from the original, just need to pull it down to fix visibility problems for some methods.
     * @param existingBean
     * @param beanName
     * @throws BeansException
     */
    public void applyBeanPropertyValues(Object existingBean, String beanName) throws BeansException {
        RootBeanDefinition bd = getMergedBeanDefinition(beanName, true);
        BeanWrapper bw = new BeanWrapperImpl(existingBean);
        initBeanWrapper(bw);
        applyPropertyValues(beanName, bd, bw, bd.getPropertyValues());
    }


    /**
     * Unchanged from the original, just need to pull it down to fix visibility problems for some methods.
     * @param beanName             name of the bean
     * @param mergedBeanDefinition the bean definition for the bean
     * @param bw                   BeanWrapper with bean instance
     */
    protected void populateBean(String beanName, RootBeanDefinition mergedBeanDefinition, BeanWrapper bw) throws BeansException {
        PropertyValues pvs = mergedBeanDefinition.getPropertyValues();

        if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME ||
                mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
            MutablePropertyValues newPvs = new MutablePropertyValues(pvs);

            // Add property values based on autowire by name if applicable.
            if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_NAME) {
                autowireByName(beanName, mergedBeanDefinition, bw, newPvs);
            }

            // Add property values based on autowire by type if applicable.
            if (mergedBeanDefinition.getResolvedAutowireMode() == RootBeanDefinition.AUTOWIRE_BY_TYPE) {
                autowireByType(beanName, mergedBeanDefinition, bw, newPvs);
            }

            pvs = newPvs;
        }

        for (Iterator it = getBeanPostProcessors().iterator(); it.hasNext(); ) {
            BeanPostProcessor beanProcessor = (BeanPostProcessor) it.next();
            if (beanProcessor instanceof InstantiationAwareBeanPostProcessor) {
                InstantiationAwareBeanPostProcessor ibp = (InstantiationAwareBeanPostProcessor) beanProcessor;
                pvs = ibp.postProcessPropertyValues(pvs, bw.getWrappedInstance(), beanName);
                if (pvs == null) {
                    return;
                }
            }
        }

        checkDependencies(beanName, mergedBeanDefinition, bw, pvs);
        applyPropertyValues(beanName, mergedBeanDefinition, bw, pvs);
    }
}
