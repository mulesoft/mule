/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.ws.consumer;

import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.cxf.CxfConstants;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.module.ws.security.SecurityStrategy;
import org.mule.module.ws.security.WSSecurity;
import org.mule.processor.AbstractRequestResponseMessageProcessor;
import org.mule.processor.NonBlockingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.transport.http.HttpConnector;
import org.mule.util.Base64;
import org.mule.util.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;


public class WSConsumer implements MessageProcessor, Initialisable, MuleContextAware, Disposable, NonBlockingMessageProcessor
{

    public static final String SOAP_HEADERS_PROPERTY_PREFIX = "soap.";

    private static final Logger logger = LoggerFactory.getLogger(WSConsumer.class);

    private MuleContext muleContext;
    private String operation;
    private WSConsumerConfig config;
    private MessageProcessor messageProcessor;
    private String soapAction;
    private String requestBody;
    private SoapVersion soapVersion;
    private boolean mtomEnabled;

    @Override
    public void initialise() throws InitialisationException
    {
        initializeConfiguration();
        parseWsdl();

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

        chainBuilder.chain(createCopyAttachmentsMessageProcessor());

        // Add a message processor that removes the invocation property CxfConstants.OPERATION if present
        // (as it may change the behavior of CXF proxy client). It is added again after executing the proxy client.
        chainBuilder.chain(createPropertyRemoverMessageProcessor(CxfConstants.OPERATION));

        chainBuilder.chain(createCxfOutboundMessageProcessor(config.getSecurity()));

        chainBuilder.chain(createSoapHeadersPropertiesRemoverMessageProcessor());

        chainBuilder.chain(config.createOutboundMessageProcessor());

        return chainBuilder.build();
    }

    private MessageProcessor createCopyAttachmentsMessageProcessor()
    {
        return new AbstractRequestResponseMessageProcessor()
        {
            @Override
            protected MuleEvent processRequest(MuleEvent event) throws MuleException
            {
                /* If the requestBody variable is set, it will be used as the payload to send instead
                 * of the payload of the message. This will happen when an operation required no input parameters. */
                if (requestBody != null)
                {
                    event.getMessage().setPayload(requestBody);
                }

                copyAttachmentsRequest(event);

                return super.processRequest(event);
            }

            @Override
            protected MuleEvent processNext(MuleEvent event) throws MuleException
            {
                try
                {
                    return super.processNext(event);
                }
                catch (DispatchException e)
                {
                    /* When a Soap Fault is returned in the response, CXF raises a SoapFault exception.
                     * We need to wrap the information of this exception into a new exception of the WS consumer module */

                    if (e.getCause() instanceof SoapFault)
                    {
                        SoapFault soapFault = (SoapFault) e.getCause();

                        event.getMessage().setPayload(soapFault.getDetail());

                        throw new SoapFaultException(event, soapFault.getFaultCode(), soapFault.getSubCode(),
                                                     soapFault.getMessage(), soapFault.getDetail(), this);
                    }
                    else
                    {
                        throw e;
                    }
                }
            }

            @Override
            protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
            {
                copyAttachmentsResponse(response);
                return super.processResponse(response, request);
            }
        };
    }

    private MessageProcessor createPropertyRemoverMessageProcessor(final String propertyName)
    {
        return new AbstractRequestResponseMessageProcessor()
        {
            private Object propertyValue;

            @Override
            protected MuleEvent processRequest(MuleEvent event) throws MuleException
            {
                propertyValue = event.getMessage().removeProperty(propertyName, PropertyScope.INVOCATION);
                return super.processRequest(event);
            }

            @Override
            protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
            {
                if (propertyValue != null)
                {
                    response.getMessage().setInvocationProperty(propertyName, propertyValue);
                }
                return super.processResponse(response, request);
            }
        };
    }

    private MessageProcessor createSoapHeadersPropertiesRemoverMessageProcessor()
    {
        return new AbstractRequestResponseMessageProcessor()
        {

            @Override
            protected MuleEvent processRequest(MuleEvent event) throws MuleException
            {
                // Remove outbound properties that are mapped to SOAP headers, so that the
                // underlying transport does not include them as headers.

                List<String> outboundProperties = new ArrayList<>(event.getMessage().getOutboundPropertyNames());

                for (String outboundProperty : outboundProperties)
                {
                    if (outboundProperty.startsWith(SOAP_HEADERS_PROPERTY_PREFIX))
                    {
                        event.getMessage().removeProperty(outboundProperty, PropertyScope.OUTBOUND);
                    }
                }
                return super.processRequest(event);
            }

            @Override
            protected MuleEvent processResponse(MuleEvent response, final MuleEvent request) throws MuleException
            {
                // Ensure that the http.status code inbound property (if present) is a String.
                Object statusCode = response.getMessage().getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, null);
                if (statusCode != null && !(statusCode instanceof String))
                {
                    response.getMessage().setProperty(HttpConnector.HTTP_STATUS_PROPERTY, statusCode.toString(), PropertyScope.INBOUND);
                }
                return super.processResponse(response, request);
            }
        };
    }

    /**
     * Creates the CXF message processor that will be used to create the SOAP envelope.
     */
    private CxfOutboundMessageProcessor createCxfOutboundMessageProcessor(WSSecurity security) throws MuleException
    {
        ProxyClientMessageProcessorBuilder cxfBuilder = new ProxyClientMessageProcessorBuilder();
        Map<String, Object> outConfigProperties = new HashMap<>();
        Map<String, Object> inConfigProperties = new HashMap<>();

        cxfBuilder.setMtomEnabled(mtomEnabled);
        cxfBuilder.setMuleContext(muleContext);
        cxfBuilder.setSoapVersion(soapVersion.getVersion());

        if (security != null && security.hasStrategies())
        {
            for (SecurityStrategy strategy : security.getStrategies())
            {
                strategy.apply(outConfigProperties, inConfigProperties);
            }
            if (cxfBuilder.getOutInterceptors() == null)
            {
                cxfBuilder.setOutInterceptors(new ArrayList<Interceptor<? extends Message>>());
            }
            if (cxfBuilder.getInInterceptors() == null)
            {
                cxfBuilder.setInInterceptors(new ArrayList<Interceptor<? extends Message>>());
            }

            if (!outConfigProperties.isEmpty())
            {
                cxfBuilder.getOutInterceptors().add(new WSS4JOutInterceptor(outConfigProperties));
            }
            if (!inConfigProperties.isEmpty())
            {
                cxfBuilder.getInInterceptors().add(new WSS4JInInterceptor(inConfigProperties));
            }
        }


        CxfOutboundMessageProcessor cxfOutboundMessageProcessor = cxfBuilder.build();

        // We need this interceptor so that an exception is thrown when the response contains a SOAPFault.
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new CheckFaultInterceptor());

        // CXF Interceptors that will ensure the SOAP body payload carries every namespace declaration from the
        // parent elements
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new NamespaceSaverStaxInterceptor());
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new NamespaceRestorerStaxInterceptor());

        if (soapAction != null)
        {
            cxfOutboundMessageProcessor.getClient().getOutInterceptors().add(new SoapActionInterceptor(soapAction));
        }

        cxfOutboundMessageProcessor.getClient().getOutInterceptors().add(new InputSoapHeadersInterceptor(muleContext));
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new OutputSoapHeadersInterceptor(muleContext));



        return cxfOutboundMessageProcessor;
    }

    /**
     * Parses the WSDL file in order to validate the service, port and operation, to get the SOAP Action (if defined)
     * and to check if the operation requires input parameters or not.
     */
    private void parseWsdl() throws InitialisationException
    {
        Definition wsdlDefinition = null;

        URL url = IOUtils.getResourceAsUrl(config.getWsdlLocation(), getClass());
        if (url == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Can't find wsdl at %s", config.getWsdlLocation()), this);
        }

        try
        {
            WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();

            URLConnection urlConnection = url.openConnection();
            if (url.getUserInfo() != null)
            {
                urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(url.getUserInfo().getBytes()));
            }

            wsdlDefinition = wsdlReader.readWSDL(url.toString(), new InputSource(urlConnection.getInputStream()));
        }
        catch (WSDLException | IOException e)
        {
            throw new InitialisationException(e, this);
        }

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

        BindingOperation bindingOperation = binding.getBindingOperation(this.operation, null, null);
        if (bindingOperation == null)
        {
            throw new InitialisationException(MessageFactory.createStaticMessage("Operation %s not found in WSDL", this.operation), this);
        }

        this.soapVersion = WSDLUtils.getSoapVersion(binding);
        this.soapAction = getSoapAction(bindingOperation);

        RequestBodyGenerator requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition);
        this.requestBody = requestBodyGenerator.generateRequestBody(bindingOperation);

    }

    /**
     * Returns the SOAP action related to an operation, or null if not specified.
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
            if (extension instanceof SOAP12Operation)
            {
                return ((SOAP12Operation) extension).getSoapActionURI();
            }
        }
        return null;
    }

    /**
     * Reads outbound attachments from the MuleMessage and sets the CxfConstants.ATTACHMENTS invocation
     * properties with a set of CXF Attachment objects.
     */
    private void copyAttachmentsRequest(MuleEvent event)
    {
        MuleMessage message = event.getMessage();

        if (!message.getOutboundAttachmentNames().isEmpty())
        {
            Collection<Attachment> attachments = new HashSet<Attachment>(message.getOutboundAttachmentNames().size());

            for (String outboundAttachmentName : message.getOutboundAttachmentNames())
            {
                Attachment attachment = new AttachmentImpl(outboundAttachmentName, message.getOutboundAttachment(outboundAttachmentName));
                attachments.add(attachment);
            }
            message.setInvocationProperty(CxfConstants.ATTACHMENTS, attachments);

            message.clearAttachments();
        }
    }

    /**
     * Takes the set of CXF attachments from the CxfConstants.ATTACHMENTS invocation properties and sets
     * them as inbound attachments in the Mule Message.
     */
    private void copyAttachmentsResponse(MuleEvent event) throws MessagingException
    {
        MuleMessage message = event.getMessage();

        if (message.getInvocationProperty(CxfConstants.ATTACHMENTS) != null)
        {
            Collection<Attachment> attachments = message.getInvocationProperty(CxfConstants.ATTACHMENTS);
            for (Attachment attachment : attachments)
            {
                try
                {
                    ((DefaultMuleMessage)message).addInboundAttachment(attachment.getId(), attachment.getDataHandler());
                }
                catch (Exception e)
                {
                    throw new MessagingException(CoreMessages.createStaticMessage("Could not set inbound attachment %s",
                                                                                  attachment.getId()), event, e, this);
                }
            }
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

    public boolean isMtomEnabled()
    {
        return mtomEnabled;
    }

    public void setMtomEnabled(boolean mtomEnabled)
    {
        this.mtomEnabled = mtomEnabled;
    }

    @Override
    public void dispose()
    {
        if (messageProcessor instanceof Disposable)
        {
            ((Disposable) messageProcessor).dispose();
        }
    }
}