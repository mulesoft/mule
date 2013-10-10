/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
