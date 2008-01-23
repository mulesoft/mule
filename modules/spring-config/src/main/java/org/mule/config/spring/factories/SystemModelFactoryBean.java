/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.RegistryContext;
import org.mule.api.MuleContext;
import org.mule.api.config.MuleProperties;
import org.mule.api.context.MuleContextAware;
import org.mule.api.model.Model;
import org.mule.config.MuleConfiguration;
import org.mule.model.ModelFactory;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * TODO
 * @deprecated Do we really need a special factory to create this?  It's just a SEDA model.
 */
public class SystemModelFactoryBean extends AbstractFactoryBean implements MuleContextAware
{

    private String type;

    private Model model;

    private MuleContext muleContext;

    public Class getObjectType()
    {
        return Model.class;
    }

    protected Object createInstance() throws Exception
    {
        // Registry may not yet exist when this method is called.
        if (RegistryContext.getRegistry() != null && RegistryContext.getRegistry().getConfiguration() != null)
        {
            type = RegistryContext.getRegistry().getConfiguration().getSystemModelType();
        }
        if (type == null)
        {
            type = MuleConfiguration.DEFAULT_SYSTEM_MODEL_TYPE; 
        }
        
        model = ModelFactory.createModel(type);
        model.setName(MuleProperties.OBJECT_SYSTEM_MODEL);

        model.setMuleContext(muleContext);
       // model.initialise();
        
        return model;
    }

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;


    }

    //@java.lang.Override
    public void afterPropertiesSet() throws Exception
    {
        super.afterPropertiesSet();
        model.initialise();
    }

    //@java.lang.Override
    public void destroy() throws Exception
    {
        super.destroy();
        model.dispose();
    }
}
