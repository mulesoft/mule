/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring;

import org.mule.config.support.InheritedModel;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.lifecycle.UMOLifecycleManager;
import org.mule.umo.model.UMOModel;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Creates a ManagementContext but has some additional logic to handle the way the context should
 * be built between Mule 1.x and 2.x
 *
 * Users will have no need to use this class themselves as it gets created automatically depending on the
 * type of configuration being used to create the Mule instance.
 *
 * @see org.mule.config.spring.ManagementContextFactoryBean
 * @see org.mule.umo.UMOManagementContext
 */
public class LegacyManagementContextFactoryBean extends ManagementContextFactoryBean
{

    /**
     * logger used by this class
     */
    protected static Log logger = LogFactory.getLog(LegacyManagementContextFactoryBean.class);

    public static final String MULE_MODEL_EXCEPTION_STRATEGY_BEAN_NAME = "muleModelExceptionStrategy";


    public LegacyManagementContextFactoryBean(UMOLifecycleManager lifecycleManager)
    {
        super(lifecycleManager);
    }

    /**
     * In Mule 1.x all endpoints in the the context should be registered with the manager
     * @param endpoints
     * @throws InitialisationException
     */
    //@Override
    protected void setEndpoints(Collection endpoints) throws UMOException
    {
        for (Iterator iterator = endpoints.iterator(); iterator.hasNext();)
        {
            UMOEndpoint ep  = (UMOEndpoint)iterator.next();
            //TODO LM: Replace
            registry.registerEndpoint(ep);
        }
    }

    /**
     * In Mule 1.x Inherited model types need to be handled differently to the other model types.  this
     * method has the logic to discover the correct model and copy service descriptors to the correct model
     * @param models
     * @throws UMOException
     */
    //@Override
    protected void setModels(Map models) throws UMOException
    {
        if (models == null)
        {
            return;
        }
        Map.Entry entry;
        for (Iterator iterator = models.entrySet().iterator(); iterator.hasNext();)
        {
            entry = (Map.Entry)iterator.next();
            UMOModel model = (UMOModel)entry.getValue();
            //In the Mule 1.x configuration we have to handle inherited models slightly differently, since we can't
            //replace the bean definition from the XSL
            if(model instanceof InheritedModel)
            {
                UMOModel realModel = (UMOModel)models.get(((InheritedModel)model).getParentName());
                for (Iterator iterator1 = model.getComponentNames(); iterator1.hasNext();)
                {
                    String name = (String)iterator1.next();
                    UMODescriptor descriptor = registry.lookupService(name);
                    realModel.registerComponent(descriptor);
                }
            }
            else
            {
                model.setName(entry.getKey().toString());
                //TODO LM: Replace
                registry.registerModel(model);

            }
        }
    }
}
