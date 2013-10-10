/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
