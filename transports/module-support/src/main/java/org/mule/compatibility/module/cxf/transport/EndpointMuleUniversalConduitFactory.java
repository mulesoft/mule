/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.cxf.transport;

import org.mule.runtime.module.cxf.CxfConfiguration;
import org.mule.runtime.module.cxf.transport.MuleUniversalConduit;
import org.mule.runtime.module.cxf.transport.MuleUniversalConduitFactory;
import org.mule.runtime.module.cxf.transport.MuleUniversalTransport;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class EndpointMuleUniversalConduitFactory implements MuleUniversalConduitFactory
{

    @Override
    public MuleUniversalConduit create(MuleUniversalTransport transport, CxfConfiguration configuration, EndpointInfo ei, EndpointReferenceType t)
    {
        return new EndpointMuleUniversalConduit(transport, configuration, ei, t);
    }

}
