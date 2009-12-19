/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.atom.endpoint;

import org.springframework.beans.factory.FactoryBean;

/**
 * A spring factory for creating an ATOM endpoint.
 */
public class AtomInboundEndpointFactoryBean extends AtomEndpointBuilder implements FactoryBean
{
    public Class getObjectType()
    {
        return AtomInboundEndpoint.class;
    }

    public boolean isSingleton()
    {
        return true;
    }

    public Object getObject() throws Exception
    {
        return buildInboundEndpoint();
    }

}
