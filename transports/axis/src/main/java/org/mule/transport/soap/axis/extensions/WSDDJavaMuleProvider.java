/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.soap.axis.extensions;

import org.mule.transport.soap.axis.AxisConnector;

import org.apache.axis.EngineConfiguration;
import org.apache.axis.deployment.wsdd.WSDDConstants;
import org.apache.axis.deployment.wsdd.WSDDProvider;
import org.apache.axis.deployment.wsdd.WSDDService;

/**
 * <code>WSDDJavaMuleProvider</code> is a factory class for creating the
 * MuleProvider.
 * 
 * @see MuleRPCProvider
 */
public class WSDDJavaMuleProvider extends WSDDProvider
{
    private AxisConnector connector;

    public WSDDJavaMuleProvider(AxisConnector connector)
    {
        this.connector = connector;
    }

    /**
     * Factory method for creating an <code>MuleRPCProvider</code>.
     * 
     * @param wsddService a <code>WSDDService</code> value
     * @param engineConfiguration an <code>EngineConfiguration</code> value
     * @return a <code>Handler</code> value
     * @exception Exception if an error occurs
     */
    public org.apache.axis.Handler newProviderInstance(WSDDService wsddService,
                                                       EngineConfiguration engineConfiguration)
        throws Exception
    {
        String serviceStyle = wsddService.getStyle().toString();
        if (serviceStyle.equals("message"))
        {
            return new MuleMsgProvider(connector);
        }
        return new MuleRPCProvider(connector);
    }

    /**
     * @return String
     * @see org.apache.axis.deployment.wsdd.WSDDProvider#getName()
     */
    public String getName()
    {
        return WSDDConstants.PROVIDER_RPC;
    }
}
