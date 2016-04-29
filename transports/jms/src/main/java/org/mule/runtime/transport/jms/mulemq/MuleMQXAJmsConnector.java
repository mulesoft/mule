/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms.mulemq;

import org.mule.runtime.core.api.MuleContext;


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
