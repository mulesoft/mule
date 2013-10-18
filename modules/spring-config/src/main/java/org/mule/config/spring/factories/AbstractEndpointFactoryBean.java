/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.endpoint.EndpointException;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.endpoint.EndpointURIEndpointBuilder;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Abstract spring FactoryBean used to creating endpoints via spring.
 */
public abstract class AbstractEndpointFactoryBean extends EndpointURIEndpointBuilder
    implements FactoryBean, Initialisable
{

    protected final Log logger = LogFactory.getLog(getClass());

    public AbstractEndpointFactoryBean(EndpointURIEndpointBuilder global) throws EndpointException
    {
        super(global);
    }

    public AbstractEndpointFactoryBean()
    {
        super();
    }

    public Object getObject() throws Exception
    {
        return doGetObject();
    }

    public boolean isSingleton()
    {
        return true;
    }

    public void initialise() throws InitialisationException
    {
        // subclasses may override this method
    }
    
    protected abstract Object doGetObject() throws Exception;

}
