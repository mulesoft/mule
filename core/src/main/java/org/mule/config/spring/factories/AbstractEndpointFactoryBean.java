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

import org.mule.impl.ManagementContextAware;
import org.mule.impl.endpoint.EndpointURIEndpointBuilder;
import org.mule.umo.endpoint.UMOImmutableEndpoint;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Abstract spring FactoryBean used to creating endpoints via spring.
 */
public abstract class AbstractEndpointFactoryBean extends EndpointURIEndpointBuilder
    implements FactoryBean, ManagementContextAware, Initialisable
{

    protected final Log logger = LogFactory.getLog(getClass());

    public AbstractEndpointFactoryBean()
    {
        super();
    }

    public Class getObjectType()
    {
        // TODO MULE-2292 Use role-specific interface
        return UMOImmutableEndpoint.class;
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
        // No initialization
    }

    protected abstract Object doGetObject() throws Exception;

}
