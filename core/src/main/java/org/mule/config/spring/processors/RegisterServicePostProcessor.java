/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.processors;

import org.mule.RegistryContext;
import org.mule.impl.model.ModelFactory;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.model.UMOModel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * TODO
 */
public class RegisterServicePostProcessor implements BeanPostProcessor
{
    /**
     * logger used by this class
     */
    protected transient final Log logger = LogFactory.getLog(RegisterServicePostProcessor.class);
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        if (bean instanceof UMODescriptor)
        {
            UMODescriptor descriptor = (UMODescriptor) bean;
            if(descriptor.getModelName()==null)
            {
                logger.warn("No model name set on the service descriptor. Attempting to use default: " + ModelFactory.DEFAULT_MODEL_NAME);
                descriptor.setModelName(ModelFactory.DEFAULT_MODEL_NAME);
               // throw new NullPointerException(new Message(Messages.X_IS_NULL, descriptor.getName() + ":UMODescriptor.getModelName()").getMessage());
            }
            UMOModel model = RegistryContext.getRegistry().lookupModel(descriptor.getModelName());
            if (model == null)
            {
                throw new IllegalArgumentException("No model named '" + descriptor.getModelName() + "' registered. Offending service is: " + descriptor);
            }
            try
            {
                model.registerComponent(descriptor);
            }
            catch (UMOException e)
            {
                throw new BeanInitializationException("failed to register service with model: " + descriptor, e);
            }
        }
        return bean;
    }

    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        return bean;
    }
}
