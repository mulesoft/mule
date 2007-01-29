/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.model;

import org.mule.MuleManager;
import org.mule.umo.model.UMOModel;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;

import java.util.Properties;

/**
 * Will locate the model service in META-INF/service using the model type as the key
 * and construct the model.
 */
public class ModelFactory
{
    public static final String MODEL_SERVICE_PATH = "org/mule/models";
    public static final String MODEL_SERVICE_TYPE = "model";
    public static final String MODEL_PROPERTY = "model";

    public static UMOModel createModel(String type) throws ModelServiceNotFoundException
    {

        try
        {
            Properties props = getModelDescriptor(type);

            String clazz = props.getProperty(MODEL_PROPERTY);
            UMOModel model = (UMOModel)ClassUtils.instanciateClass(clazz, ClassUtils.NO_ARGS,
            ModelFactory.class);
            BeanUtils.populateWithoutFail(model, props, false);
            return model;
        }
        catch (Exception e)
        {
            throw new ModelServiceNotFoundException(type, e);
        }
    }

    public static Class getModelClass(String type) throws ModelServiceNotFoundException
    {

        Properties props = getModelDescriptor(type);

        String clazz = props.getProperty(MODEL_PROPERTY);
        try
        {
            Class modelClass = ClassUtils.loadClass(clazz, ModelFactory.class);
            return modelClass;
        }
        catch (ClassNotFoundException e)
        {
            throw new ModelServiceNotFoundException(type, e);
        }
    }

    public static Properties getModelDescriptor(String type) throws ModelServiceNotFoundException
    {
        Properties props = MuleManager.getInstance().lookupServiceDescriptor(MODEL_SERVICE_TYPE, type);
        if (props != null)
        {
            return props;
        }
        else
        {
            throw new ModelServiceNotFoundException(type);
        }
    }
}
