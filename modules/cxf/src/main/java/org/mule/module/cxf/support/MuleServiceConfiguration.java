/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
