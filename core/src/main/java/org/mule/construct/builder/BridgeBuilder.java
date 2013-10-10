/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.construct.builder;

import java.util.Arrays;

import org.mule.MessageExchangePattern;
import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.transaction.TransactionConfig;
import org.mule.api.transaction.TransactionException;
import org.mule.api.transaction.TransactionFactory;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.MessageFactory;
import org.mule.construct.Bridge;
import org.mule.transaction.MuleTransactionConfig;
import org.mule.transaction.XaTransactionFactory;
import org.mule.util.ClassUtils;
import org.mule.util.StringUtils;

public class BridgeBuilder extends
    AbstractFlowConstructWithSingleInboundAndOutboundEndpointBuilder<BridgeBuilder, Bridge>
{
    protected MessageExchangePattern exchangePattern = MessageExchangePattern.REQUEST_RESPONSE;
    protected boolean transacted = false;

    @Override
    protected MessageExchangePattern getInboundMessageExchangePattern()
    {
        return exchangePattern;
    }

    @Override
    protected MessageExchangePattern getOutboundMessageExchangePattern()
    {
        return exchangePattern;
    }

    public BridgeBuilder exchangePattern(MessageExchangePattern exchangePattern)
    {
        this.exchangePattern = exchangePattern;
        return this;
    }

    public BridgeBuilder transacted(boolean transacted)
    {
        this.transacted = transacted;
        return this;
    }

    public BridgeBuilder transformers(Transformer... transformers)
    {
        this.transformers = Arrays.asList((MessageProcessor[]) transformers);
        return this;
    }

    public BridgeBuilder responseTransformers(Transformer... responseTransformers)
    {
        this.responseTransformers = Arrays.asList((MessageProcessor[]) responseTransformers);
        return this;
    }

    @Override
    protected void doConfigureInboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder)
    {
        if (transacted)
        {
            MuleTransactionConfig transactionConfig = new MuleTransactionConfig(TransactionConfig.ACTION_BEGIN_OR_JOIN);
            transactionConfig.setMuleContext(muleContext);
            endpointBuilder.setTransactionConfig(transactionConfig);
        }
    }

    @Override
    protected void doConfigureOutboundEndpointBuilder(MuleContext muleContext, EndpointBuilder endpointBuilder)
    {
        if (transacted)
        {
            MuleTransactionConfig transactionConfig = new MuleTransactionConfig(TransactionConfig.ACTION_ALWAYS_JOIN);
            transactionConfig.setMuleContext(muleContext);
            endpointBuilder.setTransactionConfig(transactionConfig);
        }
    }

    @Override
    protected Bridge buildFlowConstruct(MuleContext muleContext) throws MuleException
    {
        InboundEndpoint inboundEndpoint = getOrBuildInboundEndpoint(muleContext);
        OutboundEndpoint outboundEndpoint = getOrBuildOutboundEndpoint(muleContext);

        if (transacted)
        {
            setTransactionFactoriesIfNeeded(inboundEndpoint, outboundEndpoint);
        }

        return new Bridge(name, muleContext, inboundEndpoint, outboundEndpoint, transformers,
            responseTransformers, exchangePattern, transacted);
    }

    private void setTransactionFactoriesIfNeeded(InboundEndpoint inboundEndpoint,
                                                 OutboundEndpoint outboundEndpoint) throws MuleException
    {
        String inboundProtocol = inboundEndpoint.getConnector().getProtocol();
        String outboundProtocol = outboundEndpoint.getConnector().getProtocol();

        boolean needXA = !inboundProtocol.equals(outboundProtocol);

        TransactionConfig inboundTransactionConfig = inboundEndpoint.getTransactionConfig();

        if (inboundTransactionConfig.getFactory() == null)
        {
            TransactionFactory transactionFactory = needXA
                                                          ? new XaTransactionFactory()
                                                          : getTransactionFactory(inboundProtocol);

            inboundTransactionConfig.setFactory(transactionFactory);
        }

        TransactionConfig outboundTransactionConfig = outboundEndpoint.getTransactionConfig();

        if (outboundTransactionConfig.getFactory() == null)
        {
            TransactionFactory transactionFactory = needXA
                                                          ? new XaTransactionFactory()
                                                          : getTransactionFactory(outboundProtocol);
            outboundTransactionConfig.setFactory(transactionFactory);
        }
    }

    /**
     * Very simplistic attempt to locate a protocol specific transaction factory.
     */
    private TransactionFactory getTransactionFactory(String protocol) throws MuleException
    {
        String protocolTransactionFactoryClassName = "org.mule.transport." + StringUtils.lowerCase(protocol)
                                                     + "." + StringUtils.capitalize(protocol)
                                                     + "TransactionFactory";

        if (!ClassUtils.isClassOnPath(protocolTransactionFactoryClassName, getClass()))
        {
            throw new TransactionException(
                MessageFactory.createStaticMessage("Failed to locate a transaction factory for protocol: "
                                                   + protocol));
        }

        try
        {
            return (TransactionFactory) ClassUtils.instanciateClass(protocolTransactionFactoryClassName);
        }
        catch (Exception e)
        {
            throw new TransactionException(
                MessageFactory.createStaticMessage("Failed to instantiate a transaction factory for protocol: "
                                                   + protocol), e);
        }
    }
}
