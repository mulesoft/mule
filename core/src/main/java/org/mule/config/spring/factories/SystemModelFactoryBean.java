/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.RegistryContext;
import org.mule.config.MuleConfiguration;
import org.mule.config.MuleProperties;
import org.mule.impl.ManagementContextAware;
import org.mule.impl.model.ModelFactory;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.model.UMOModel;

import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * TODO
 * @deprecated Do we really need a special factory to create this?  It's just a SEDA model.
 */
public class SystemModelFactoryBean extends AbstractFactoryBean implements ManagementContextAware
{

    private String type;

    private UMOModel model;

    private UMOManagementContext managementContext;

    public Class getObjectType()
    {
        return UMOModel.class;
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

        model.setManagementContext(managementContext);
       // model.initialise();
        
        return model;
    }

    public void setManagementContext(UMOManagementContext context)
    {
        managementContext = context;


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
