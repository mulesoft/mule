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

import org.mule.providers.service.ConnectorFactory;
import org.mule.umo.model.UMOModel;
import org.mule.util.BeanUtils;
import org.mule.util.ClassUtils;
import org.mule.util.SpiUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Will locate the model service in META-INF/service using the model type as the key.
 * Then construct the model
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class ModelFactory
{

    public static final String MODEL_SERVICE_PATH = "org/mule/models";
    public static final String MODEL_PROPERTY = "model";
    public static final String MODEL_TYPE_POSTFIX = ".properties";

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
        String location = SpiUtils.SERVICE_ROOT + MODEL_SERVICE_PATH;
        InputStream is = SpiUtils.findServiceDescriptor(MODEL_SERVICE_PATH, type + MODEL_TYPE_POSTFIX, ConnectorFactory.class);
        try
        {
            if (is != null)
            {
                Properties props = new Properties();
                props.load(is);
                return props;
            }
            else
            {
                throw new ModelServiceNotFoundException(location + "/" + type);
            }
        }
        catch (IOException e)
        {
            throw new ModelServiceNotFoundException(location + "/" + type, e);
        }
    }
}
