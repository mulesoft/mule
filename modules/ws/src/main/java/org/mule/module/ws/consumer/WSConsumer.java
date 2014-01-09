/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.Connector;
import org.mule.api.transport.DispatchException;
import org.mule.config.i18n.MessageFactory;
import org.mule.endpoint.MuleEndpointURI;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.module.ws.security.SecurityStrategy;
import org.mule.module.ws.security.WSSecurity;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.transformer.simple.AutoTransformer;

import java.util.List;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;

public class WSConsumer implements MessageProcessor, Initialisable
{

    private final MessageProcessor mp;
    private final String wsdlLocation;
    private final String wsdlService;
    private final String wsdlPort;
    private final String wsdlOperation;

    private String soapAction;

    public WSConsumer(String wsdlLocation, String wsdlService, String wsdlPort, String wsdlOperation, String serviceAddress,
                      Connector connector, WSSecurity security, MuleContext muleContext) throws MuleException
    {
        this.wsdlLocation = wsdlLocation;
        this.wsdlService = wsdlService;
        this.wsdlPort = wsdlPort;
        this.wsdlOperation = wsdlOperation;

        ProxyClientMessageProcessorBuilder cxfBuilder = new ProxyClientMessageProcessorBuilder();
        cxfBuilder.setMuleContext(muleContext);

        if (security != null)
        {
            for (SecurityStrategy strategy : security.getStrategies())
            {
                strategy.apply(cxfBuilder);
            }
        }

        MessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();
        AutoTransformer auto = new AutoTransformer();
        auto.setReturnDataType(DataType.STRING_DATA_TYPE);

        chainBuilder.chain(new AbstractInterceptingMessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                try
                {
                    return processNext(event);
                }
                catch (DispatchException e)
                {
                    /* When a Soap Fault is returned in the response, CXF raises a SoapFault exception.
                     * We need to wrap the information of this exception into a new exception of the WS consumer module */

                    if (e.getCause() instanceof SoapFault)
                    {
                        SoapFault soapFault = (SoapFault) e.getCause();
                        throw new SoapFaultException(soapFault.getFaultCode(), soapFault.getSubCode(), soapFault.getMessage(), soapFault.getDetail());
                    }
                    else
                    {
                        throw e;
                    }
                }
            }
        });

        chainBuilder.chain(new ResponseMessageProcessorAdapter(auto));

        CxfOutboundMessageProcessor cxfOutboundMessageProcessor = cxfBuilder.build();

        // We need this interceptor so that an exception is thrown when the response contains a SOAPFault.
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new CheckFaultInterceptor());

        chainBuilder.chain(cxfOutboundMessageProcessor);
        chainBuilder.chain(createEndpoint(serviceAddress, connector, muleContext));
        mp = chainBuilder.build();

    }

    @Override
    public void initialise() throws InitialisationException
    {
        try
        {
            WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
            Definition wsdlDefinition = wsdlReader.readWSDL(wsdlLocation);

            Service service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), wsdlService));
            if (service == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage(String.format("Service %s not found in WSDL", wsdlService)), this);
            }

            Port port = service.getPort(wsdlPort);
            if (port == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage(String.format("Port %s not found in WSDL", wsdlPort)), this);
            }

            Binding binding = port.getBinding();
            if (binding == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage(String.format("Port %s has no binding", wsdlPort)), this);
            }

            BindingOperation operation = port.getBinding().getBindingOperation(wsdlOperation, null, null);
            if (operation == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage(String.format("Operation %s not found in WSDL", wsdlOperation)), this);
            }

            this.soapAction = getSoapAction(operation);
        }
        catch (WSDLException e)
        {
            throw new InitialisationException(e, this);
        }
    }

    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (soapAction != null)
        {
            event.getMessage().setOutboundProperty("SOAPAction", soapAction);
        }
        return mp.process(event);
    }

    protected OutboundEndpoint createEndpoint(String address, Connector connector, MuleContext muleContext)
            throws MuleException
    {
        if (connector != null)
        {
            String protocol = new MuleEndpointURI(address, muleContext).getScheme();
            if (!connector.supportsProtocol(protocol))
            {
                throw new RuntimeException("Connector " + connector + " does not support protocol: " + protocol);
            }
        }
        EndpointBuilder builder = muleContext.getEndpointFactory().getEndpointBuilder(address);
        builder.setConnector(connector);
        return muleContext.getEndpointFactory().getOutboundEndpoint(builder);
    }

    /**
     * Returns the Soap Action for a binding operation of a WSDL, or null if there is no action defined.
     */
    private String getSoapAction(BindingOperation bindingOperation)
    {
        List extensions = bindingOperation.getExtensibilityElements();
        for (Object extension : extensions)
        {
            if (extension instanceof SOAPOperation)
            {
                return ((SOAPOperation) extension).getSoapActionURI();
            }
        }

        return null;
    }
}
