/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.cxf.support;

import org.mule.module.cxf.builder.AbstractInboundMessageProcessorBuilder;

import javax.xml.namespace.QName;

import org.apache.cxf.service.factory.AbstractServiceConfiguration;

/**
 * This is the {@link AbstractServiceConfiguration ServiceConfiguration} that is
 * associated to the {@link AbstractInboundMessageProcessorBuilder
 * InboundMessageProcessorBuilder}, so it can obtain properties directly from it.
 */
public class MuleServiceConfiguration extends AbstractServiceConfiguration
{
    final AbstractInboundMessageProcessorBuilder builder;

    public MuleServiceConfiguration(AbstractInboundMessageProcessorBuilder builder)
    {
        this.builder = builder;
    }

    @Override
    public String getServiceNamespace()
    {
        final String builderNameSpace = builder.getNamespace();
        if (builderNameSpace != null)
        {
            return builderNameSpace;
        }
        else
        {
            return super.getServiceNamespace();
        }
    }

    @Override
    public String getServiceName()
    {
        final String builderServiceName = builder.getService();
        if (builderServiceName != null)
        {
            return builderServiceName;
        }
        else
        {
            return super.getServiceName();
        }
    }

    @Override
    public QName getEndpointName()
    {
        final String builderPort = builder.getPort();
        if (builderPort != null)
        {
            return new QName(getServiceNamespace(), builderPort);
        }
        else
        {
            return super.getEndpointName();
        }
    }
}
