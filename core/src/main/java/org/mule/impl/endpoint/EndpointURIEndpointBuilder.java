/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.impl.endpoint;

import org.mule.impl.ManagementContextAware;
import org.mule.umo.UMOManagementContext;
import org.mule.umo.endpoint.UMOEndpointURI;

public class EndpointURIEndpointBuilder extends AbstractEndpointBuilder implements ManagementContextAware
{

    public EndpointURIEndpointBuilder()
    {
        super();
    }

    public EndpointURIEndpointBuilder(final UMOEndpointURI endpointURI, UMOManagementContext managementContext)
    {
        this.managementContext = managementContext;
        this.endpointURI = endpointURI;
    }

}
