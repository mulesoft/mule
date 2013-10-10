/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.jms.mulemq;

import org.mule.api.MuleContext;


public class MuleMQXAJmsConnector extends MuleMQJmsConnector
{
    public static final String MULEMQ_XA_CONNECTION_FACTORY_CLASS = "com.pcbsys.nirvana.nJMS.XAConnectionFactoryImpl";

    public MuleMQXAJmsConnector(MuleContext context)
    {
        super(context);
    }

    @Override
    protected String getMuleMQFactoryClass()
    {
        return MULEMQ_XA_CONNECTION_FACTORY_CLASS;
    }
}
