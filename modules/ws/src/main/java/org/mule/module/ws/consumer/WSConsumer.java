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
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.cxf.CxfConstants;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.module.ws.security.SecurityStrategy;
import org.mule.module.ws.security.WSSecurity;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.ResponseMessageProcessorAdapter;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.transformer.simple.AutoTransformer;
import org.mule.transformer.types.SimpleDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class WSConsumer implements MessageProcessor, Initialisable, MuleContextAware
{

    public static final String SOAP_ACTION_PROPERTY = "SOAPAction";
    public static final String SOAP_HEADERS_PROPERTY_PREFIX = "soap.";

    private MuleContext muleContext;
    private String operation;
    private WSConsumerConfig config;
    private MessageProcessor messageProcessor;
    private String soapAction;


    @Override
    public void initialise() throws InitialisationException
    {
        initializeConfiguration();
        initializeSoapActionFromWsdl();

        try
        {
            messageProcessor = createMessageProcessor();
        }
        catch (MuleException e)
        {
            throw new InitialisationException(e, this);
        }
    }


    @Override
    public MuleEvent process(MuleEvent event) throws MuleException
    {
        if (soapAction != null)
        {
            event.getMessage().setOutboundProperty(SOAP_ACTION_PROPERTY, soapAction);
        }
        return messageProcessor.process(event);
    }


    /**
     * Initializes the configuration for this web service consumer. If no reference to WSConsumerConfig is set,
     * then it is looked up in the registry (only one object of this type must exist in the registry, or an
     * InitializationException will be thrown).
     */
    private void initializeConfiguration() throws InitialisationException
    {
        if (config == null)
        {
            try
            {
                config = muleContext.getRegistry().lookupObject(WSConsumerConfig.class);
                if (config == null)
                {
                    throw new InitialisationException(CoreMessages.createStaticMessage("No configuration defined for the web service " +
                                                                                       "consumer. Add a consumer-config element."), this);
                }
            }
            catch (RegistrationException e)
            {
                throw new InitialisationException(e, this);
            }
        }
    }

    /**
     * Creates the message processor chain in which we will delegate the process of mule events.
     * The chain composes a string transformer, a CXF client proxy and and outbound endpoint.
     */
    private MessageProcessor createMessageProcessor() throws MuleException
    {
        MessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();

        // TODO: MULE-7242 Define if this transformer should be added to the chain or not.

        final AutoTransformer autoTransformer = new AutoTransformer();
        autoTransformer.setReturnDataType(DataType.STRING_DATA_TYPE);

        chainBuilder.chain(new ResponseMessageProcessorAdapter(autoTransformer));

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

                        event.getMessage().setPayload(soapFault.getDetail());

                        // Manually call the AutoTransformer to convert the payload to String.
                        autoTransformer.process(event);

                        throw new SoapFaultException(event, soapFault.getFaultCode(), soapFault.getSubCode(),
                                                     soapFault.getMessage(), soapFault.getDetail());
                    }
                    else
                    {
                        throw e;
                    }
                }
            }
        });

        chainBuilder.chain(createCxfOutboundMessageProcessor(config.getSecurity()));

        chainBuilder.chain(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                /* We need to force CXF to generate the payload, so that the SOAP headers are added to the envelope,
                 * and can be safely removed from the event. Otherwise, CXF keeps a reference to the event and
                 * generates lazy the envelope when the outbound endpoint requests the payload, and in that point the
                 * outbound properties that contain the SOAP headers would have been already removed from the message. */

                 event.getMessage().setPayload(event.getMessage().getPayload(DataType.BYTE_ARRAY_DATA_TYPE));

                List<String> outboundProperties = new ArrayList<String>(event.getMessage().getOutboundPropertyNames());

                for (String outboundProperty : outboundProperties)
                {
                    if (outboundProperty.startsWith(SOAP_HEADERS_PROPERTY_PREFIX))
                    {
                        event.getMessage().removeProperty(outboundProperty, PropertyScope.OUTBOUND);
                    }
                }
                return event;
            }
        });

        chainBuilder.chain(config.createOutboundEndpoint());

        return chainBuilder.build();
    }

    /**
     * Creates the CXF message processor that will be used to create the SOAP envelope.
     */
    private CxfOutboundMessageProcessor createCxfOutboundMessageProcessor(WSSecurity security) throws MuleException
    {
        ProxyClientMessageProcessorBuilder cxfBuilder = new ProxyClientMessageProcessorBuilder();
        Map<String, Object> configProperties = new HashMap<String, Object>();

        cxfBuilder.setMuleContext(muleContext);

        if (security != null)
        {
            for (SecurityStrategy strategy : security.getStrategies())
            {
                strategy.apply(configProperties);
            }
            if (cxfBuilder.getOutInterceptors() == null)
            {
                cxfBuilder.setOutInterceptors(new ArrayList<Interceptor<? extends Message>>());
            }

            cxfBuilder.getOutInterceptors().add(new WSS4JOutInterceptor(configProperties));
        }


        CxfOutboundMessageProcessor cxfOutboundMessageProcessor = cxfBuilder.build();

        // We need this interceptor so that an exception is thrown when the response contains a SOAPFault.
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new CheckFaultInterceptor());

        Transformer stringToXmlDocumentTransformer = muleContext.getRegistry().lookupTransformer(DataType.STRING_DATA_TYPE, new SimpleDataType<Object>(Document.class));
        Transformer xmlElementToStringTransformer = muleContext.getRegistry().lookupTransformer(new SimpleDataType<Object>(Element.class), DataType.STRING_DATA_TYPE);

        cxfOutboundMessageProcessor.getClient().getOutInterceptors().add(new InputSoapHeadersInterceptor(stringToXmlDocumentTransformer));
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new OutputSoapHeadersInterceptor(xmlElementToStringTransformer));

        return cxfOutboundMessageProcessor;
    }

    /**
     * Parses the WSDL file in order to get the SOAP Action (if defined) for the specified service, operation and port.
     */
    private void initializeSoapActionFromWsdl() throws InitialisationException
    {
        try
        {
            WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
            Definition wsdlDefinition = wsdlReader.readWSDL(config.getWsdlLocation());

            Service service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), config.getService()));
            if (service == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Service %s not found in WSDL", config.getService()), this);
            }

            Port port = service.getPort(config.getPort());
            if (port == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Port %s not found in WSDL", config.getPort()), this);
            }

            Binding binding = port.getBinding();
            if (binding == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Port %s has no binding", config.getPort()), this);
            }

            BindingOperation operation = binding.getBindingOperation(this.operation, null, null);
            if (operation == null)
            {
                throw new InitialisationException(MessageFactory.createStaticMessage("Operation %s not found in WSDL", this.operation), this);
            }

            List extensions = operation.getExtensibilityElements();
            for (Object extension : extensions)
            {
                if (extension instanceof SOAPOperation)
                {
                    this.soapAction = ((SOAPOperation) extension).getSoapActionURI();
                    return;
                }
            }
        }
        catch (WSDLException e)
        {
            throw new InitialisationException(e, this);
        }
    }


    @Override
    public void setMuleContext(MuleContext muleContext)
    {
        this.muleContext = muleContext;
    }

    public WSConsumerConfig getConfig()
    {
        return config;
    }

    public void setConfig(WSConsumerConfig config)
    {
        this.config = config;
    }

    public String getOperation()
    {
        return operation;
    }

    public void setOperation(String operation)
    {
        this.operation = operation;
    }


    /**
     * CXF interceptor that adds Soap headers to the SoapMessage based on outbound properties
     * from the Mule message that start with the soap prefix.
     */
    private class InputSoapHeadersInterceptor extends AbstractSoapInterceptor
    {

        private final Transformer stringToXmlDocumentTransformer;

        public InputSoapHeadersInterceptor(Transformer stringToXmlDocumentTransformer)
        {
            super(Phase.PRE_PROTOCOL);
            this.stringToXmlDocumentTransformer = stringToXmlDocumentTransformer;
        }

        @Override
        public void handleMessage(SoapMessage message) throws Fault
        {
            MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);

            if (event == null)
            {
                return;
            }

            for (String outboundPropertyName : event.getMessage().getOutboundPropertyNames())
            {
                if (outboundPropertyName.startsWith(SOAP_HEADERS_PROPERTY_PREFIX))
                {
                    String headerXML = event.getMessage().getOutboundProperty(outboundPropertyName);

                    try
                    {
                        Document document = (Document) stringToXmlDocumentTransformer.transform(headerXML);

                        // This QName is required by the SoapHeader but it is not used.
                        QName qname = new QName(null, document.getDocumentElement().getTagName());

                        message.getHeaders().add(new SoapHeader(qname, document.getDocumentElement()));
                    }
                    catch (TransformerException e)
                    {
                        throw new InvalidSoapHeaderException(CoreMessages.createStaticMessage("Outbound property %s " +
                                                                                              "contains an invalid XML string",
                                                                                              outboundPropertyName), e, event);
                    }
                }
            }
        }
    }

    /**
     * CXF interceptor that adds inbound properties to the Mule message based on the SOAP headers
     * received in the response.
     */
    private static class OutputSoapHeadersInterceptor extends AbstractSoapInterceptor
    {

        private final Transformer xmlElementToStringTransformer;

        public OutputSoapHeadersInterceptor(Transformer xmlElementToStringTransformer)
        {
            super(Phase.PRE_PROTOCOL);
            this.xmlElementToStringTransformer = xmlElementToStringTransformer;
        }

        @Override
        public void handleMessage(SoapMessage message) throws Fault
        {
            MuleEvent event = (MuleEvent) message.getExchange().get(CxfConstants.MULE_EVENT);

            if (event == null)
            {
                return;
            }

            for (Header header : message.getHeaders())
            {
                if (header instanceof SoapHeader)
                {
                    try
                    {
                        String key = SOAP_HEADERS_PROPERTY_PREFIX + header.getName().getLocalPart();
                        String value = (String) xmlElementToStringTransformer.transform(header.getObject());
                        event.getMessage().setProperty(key, value, PropertyScope.INBOUND);
                    }
                    catch (TransformerException e)
                    {
                        throw new InvalidSoapHeaderException(CoreMessages.createStaticMessage("Cannot parse content " +
                                                                                              "of SOAP header %s in the response",
                                                                                              header.getName().getLocalPart()), e, event);
                    }
                }
            }
        }
    }
}
