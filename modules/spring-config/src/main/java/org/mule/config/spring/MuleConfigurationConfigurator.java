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

import org.mule.api.MuleContext;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.MuleConfiguration;
import org.mule.api.context.MuleContextAware;
import org.mule.config.DefaultMuleConfiguration;
import org.mule.config.i18n.MessageFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.SmartFactoryBean;

/**
 * This class is a "SmartFactoryBean" which allows a few XML attributes to be set on the 
 * otherwise read-only MuleConfiguration.  It looks up the MuleConfiguration from the 
 * MuleContext and does some class-casting to be able to modify it.  Note that this will
 * only work if the MuleContext has not yet been started, otherwise the modifications 
 * will be ignored (and warnings logged).
 */
public class MuleConfigurationConfigurator implements MuleContextAware, SmartFactoryBean
{
    private MuleContext muleContext;
    
    // We instantiate DefaultMuleConfiguration to make sure we get the default values for 
    // any properties not set by the user.
    private DefaultMuleConfiguration config = new DefaultMuleConfiguration();

    protected transient Log logger = LogFactory.getLog(MuleConfigurationConfigurator.class);

    public void setMuleContext(MuleContext context)
    {
        this.muleContext = context;
    }

    public boolean isEagerInit()
    {
        return true;
    }

    public boolean isPrototype()
    {
        return false;
    }

    public Object getObject() throws Exception
    {
        MuleConfiguration configuration = muleContext.getConfiguration();
        if (configuration instanceof DefaultMuleConfiguration)
        {
            ((DefaultMuleConfiguration) configuration).setDefaultSynchronousEndpoints(config.isDefaultSynchronousEndpoints());
            ((DefaultMuleConfiguration) configuration).setDefaultResponseTimeout(config.getDefaultResponseTimeout());
            ((DefaultMuleConfiguration) configuration).setDefaultTransactionTimeout(config.getDefaultTransactionTimeout());
            ((DefaultMuleConfiguration) configuration).setShutdownTimeout(config.getShutdownTimeout());
            return configuration;
        }
        else
        {
            throw new ConfigurationException(MessageFactory.createStaticMessage("Unable to set properties on read-only MuleConfiguration: " + configuration.getClass()));
        }
    }

    public Class getObjectType()
    {
        return MuleConfiguration.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void setDefaultSynchronousEndpoints(boolean synchronous)
    {
        config.setDefaultSynchronousEndpoints(synchronous);
    }
    
    public void setDefaultResponseTimeout(int responseTimeout)
    {
        config.setDefaultResponseTimeout(responseTimeout);
    }
    
    
    public void setDefaultTransactionTimeout(int defaultTransactionTimeout)
    {
        config.setDefaultTransactionTimeout(defaultTransactionTimeout);
    }

    public void setShutdownTimeout(int shutdownTimeout)
    {
        config.setShutdownTimeout(shutdownTimeout);
    }

}
