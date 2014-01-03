/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.transport.Connector;
import org.mule.module.ws.consumer.WSConsumer;
import org.mule.module.ws.security.WSSecurity;

import org.springframework.beans.factory.FactoryBean;

public class WSConsumerFactoryBean implements FactoryBean<WSConsumer>, MuleContextAware
{

    protected String wsdlLocation;
    protected String wsdlService;
    protected String wsdlPort;
    protected String wsdlOperation;
    protected String serviceAddress;
    protected Connector connector;
    protected WSSecurity security;

    protected MuleContext muleContext;

    @Override
    public WSConsumer getObject() throws Exception
    {
        return new WSConsumer(wsdlLocation, wsdlService, wsdlPort, wsdlOperation, serviceAddress, connector,
            security, muleContext);
    }

    @Override
    public Class<?> getObjectType()
    {
        return WSConsumer.class;
    }

    @Override
    public boolean isSingleton()
    {
        return false;
    }

    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;

    }

    public String getWsdlLocation()
    {
        return wsdlLocation;
    }

    public void setWsdlLocation(String wsdlLocation)
    {
        this.wsdlLocation = wsdlLocation;
    }

    public String getWsdlPort()
    {
        return wsdlPort;
    }

    public void setWsdlPort(String wsdlPort)
    {
        this.wsdlPort = wsdlPort;
    }

    public String getWsdlOperation()
    {
        return wsdlOperation;
    }

    public void setWsdlOperation(String wsdlOperation)
    {
        this.wsdlOperation = wsdlOperation;
    }

    public String getServiceAddress()
    {
        return serviceAddress;
    }

    public void setServiceAddress(String serviceAddress)
    {
        this.serviceAddress = serviceAddress;
    }

    public MuleContext getMuleContext()
    {
        return muleContext;
    }

    public String getWsdlService()
    {
        return wsdlService;
    }

    public void setWsdlService(String wsdlService)
    {
        this.wsdlService = wsdlService;
    }

    public Connector getConnector()
    {
        return connector;
    }

    public void setConnector(Connector connector)
    {
        this.connector = connector;
    }

    public WSSecurity getSecurity()
    {
        return security;
    }

    public void setSecurity(WSSecurity security)
    {
        this.security = security;
    }
}
