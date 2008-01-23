/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.model;

import org.mule.RegistryContext;
import org.mule.api.model.Model;
import org.mule.api.model.ModelServiceDescriptor;
import org.mule.api.registry.ServiceDescriptorFactory;
import org.mule.api.registry.ServiceException;

/**
 * Will locate the model service using the model type as the key and construct the model.
 * @deprecated This class doesn't make much sense, remove for 2.x
 */
public final class ModelFactory
{
    public static final String DEFAULT_MODEL_NAME = "main";

    public static Model createModel(String type) throws ServiceException
    {
        return createModel(type, DEFAULT_MODEL_NAME);
    }

    public static Model createModel(String type, String name) throws ServiceException
    {
        ModelServiceDescriptor sd = (ModelServiceDescriptor)
            RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.MODEL_SERVICE_TYPE, type, null);
        if (sd != null)
        {
            Model model = sd.createModel();
            model.setName(name);
            return model;
        }
        else return null;
    }
    
    public static Class getModelClass(String type) throws ServiceException
    {
        ModelServiceDescriptor sd = (ModelServiceDescriptor) 
            RegistryContext.getRegistry().lookupServiceDescriptor(ServiceDescriptorFactory.MODEL_SERVICE_TYPE, type, null);
        if (sd != null)
        {
            return sd.getModelClass();
        }
        else return null;
    }
}
