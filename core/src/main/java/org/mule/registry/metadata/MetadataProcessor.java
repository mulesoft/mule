/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.registry.metadata;

import java.lang.reflect.Field;

import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.umo.UMODescriptor;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.Registerable;
import org.mule.umo.manager.UMOAgent;
import org.mule.umo.model.UMOModel;
import org.mule.umo.provider.UMOConnector;
import org.mule.umo.transformer.UMOTransformer;
import org.mule.util.MuleObjectHelper;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 */

public class MetadataProcessor implements BeanPostProcessor
{
    public Object postProcessBeforeInitialization(Object o, String s) throws BeansException
    {
        try
        {
            if (o instanceof Registerable)
            {
                Field field = o.getClass().getField("objectMetadata");
                if (field != null)
                {
                    ObjectMetadata objectMetadata = (ObjectMetadata)field.get(o);

                    // In many cases, the className won't have been set in the
                    // class' static ObjectMetadata property so Spring has to
                    if (objectMetadata.getClassName() == null)
                    {
                        objectMetadata.setClassName(o.getClass().getName());
                    }

                    MetadataStore.addObjectMetadata(objectMetadata);
                }
                else
                {
                    //System.out.println("No objectMetadata for this guy");
                }
            }
        }
        catch (NoSuchFieldException e)
        {
            System.out.println("No objectMetadata for this guy");
        }
        catch (Exception e)
        {
            System.out.println(e);
        }

        return o;
    }

    public Object postProcessAfterInitialization(Object o, String s) throws BeansException
    {
        return o;
    }

}

