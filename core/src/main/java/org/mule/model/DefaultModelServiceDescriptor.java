/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.model;

import org.mule.api.config.MuleProperties;
import org.mule.api.model.Model;
import org.mule.api.model.ModelServiceDescriptor;
import org.mule.api.registry.AbstractServiceDescriptor;
import org.mule.api.registry.ServiceException;
import org.mule.config.i18n.CoreMessages;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.util.Properties;

public class DefaultModelServiceDescriptor extends AbstractServiceDescriptor implements ModelServiceDescriptor
{
    private String modelClass;
    private Properties properties;
    
    public DefaultModelServiceDescriptor(String service, Properties properties)
    {
        super(service);
        this.properties = properties;
        modelClass = removeProperty(MuleProperties.MODEL_CLASS, properties);
    }

    @Deprecated
    public Model createModel() throws ServiceException
    {
        if (modelClass != null)
        {
            try 
            {
                Model model = (Model)ClassUtils.instanciateClass(modelClass, ClassUtils.NO_ARGS, getClass());
                BeanUtils.populateWithoutFail(model, properties, false);
                return model;
            }
            catch (Exception e)
            {
                throw new ServiceException(CoreMessages.failedToCreate(modelClass), e);
            }            
        }
        else return null;
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    public Class<Model> getModelClass() throws ServiceException
    {
        try 
        {
            return ClassUtils.getClass(modelClass);
        }
        catch (ClassNotFoundException e)
        {
            throw new ServiceException(CoreMessages.cannotLoadFromClasspath(modelClass), e);
        }
    }
}
