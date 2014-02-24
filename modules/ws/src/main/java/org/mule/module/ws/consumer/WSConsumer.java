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
import org.mule.api.transport.DispatchException;
import org.mule.api.transport.PropertyScope;
import org.mule.common.metadata.DefaultXmlMetaDataModel;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.module.cxf.CxfOutboundMessageProcessor;
import org.mule.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.module.ws.security.SecurityStrategy;
import org.mule.module.ws.security.WSSecurity;
import org.mule.processor.AbstractInterceptingMessageProcessor;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.util.IOUtils;

import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Part;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.extensions.soap12.SOAP12Operation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.interceptor.CheckFaultInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WSConsumer implements MessageProcessor, Initialisable, MuleContextAware
{

    public static final String SOAP_ACTION_PROPERTY = "SOAPAction";
    public static final String SOAP_HEADERS_PROPERTY_PREFIX = "soap.";

    private static final Logger logger = LoggerFactory.getLogger(WSConsumer.class);

    private MuleContext muleContext;
    private String operation;
    private WSConsumerConfig config;
    private MessageProcessor messageProcessor;
    private String soapAction;
    private String requestBody;

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

        chainBuilder.chain(new AbstractInterceptingMessageProcessor()
        {

            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
                try
                {
                    /* If the requestBody variable is set, it will be used as the payload to send instead
                     * of the payload of the message. This will happen when an operation required no input parameters. */

                    if (requestBody != null)
                    {
                        event.getMessage().setPayload(requestBody);
                    }

                    MuleEvent result =  processNext(event);
                    return result;
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

        // Add a MessageProcessor to remove outbound properties that are mapped to SOAP headers, so that the
        // underlying transport does not include them as headers.
        chainBuilder.chain(new MessageProcessor()
        {
            @Override
            public MuleEvent process(MuleEvent event) throws MuleException
            {
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

        if (security != null && security.hasStrategies())
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

        // CXF Interceptors that will ensure the SOAP body payload carries every namespace declaration from the
        // parent elements
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new NamespaceSaverStaxInterceptor());
        cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new NamespaceRestorerStaxInterceptor());


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
            wsdlDefinition = wsdlReader.readWSDL(url.toString());
        }
        catch (WSDLException e)
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

        this.soapAction = getSoapAction(bindingOperation);
        this.requestBody = generateRequestBody(wsdlDefinition, bindingOperation);

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
     * Checks if the operation requires input parameters (if the XML required in the body is just one constant element).
     * If so, the body with this XML will be returned in order to send it in every request instead of the payload.
     */
    private String generateRequestBody(Definition wsdlDefinition, BindingOperation bindingOperation)
    {
        List<String> schemas;

        try
        {
            schemas = WSDLUtils.getSchemas(wsdlDefinition);
        }
        catch (TransformerException e)
        {
            logger.warn("Unable to get schemas from WSDL, cannot check if the operation requires input parameters", e);
            return null;
        }

        SOAPBody soapBody = WSDLUtils.getSoapBody(bindingOperation);

        if (soapBody == null)
        {
            logger.warn("No SOAP body defined in the WSDL for the specified operation, cannot check if the operation requires input parameters");
            return null;
        }

        Part part = getPart(soapBody, bindingOperation.getOperation().getInput().getMessage());

        DefaultXmlMetaDataModel model = new DefaultXmlMetaDataModel(schemas, part.getElementName(), Charset.defaultCharset());

        if (model.getFields().isEmpty())
        {
            logger.info("The selected operation does not require input parameters, the payload will be ignored");
            QName element = part.getElementName();
            return String.format("<ns:%s xmlns:ns=\"%s\" />", element.getLocalPart(), element.getNamespaceURI());
        }

        return null;
    }

    /**
     * Finds the part of the input message that must be used in the SOAP body.
     */
    private Part getPart(SOAPBody soapBody, javax.wsdl.Message inputMessage)
    {
        if (soapBody.getParts() == null || soapBody.getParts().isEmpty())
        {
            Map parts = inputMessage.getParts();

            if (parts.isEmpty())
            {
                return null;
            }
            if (parts.size() > 1)
            {
                logger.warn("Input messages with multiple parts are not supported");
            }
            return (Part) parts.values().iterator().next();
        }
        else
        {
            if (soapBody.getParts().size() > 1)
            {
                logger.warn("Input messages with multiple parts in the SOAP body are not supported");
            }
            String partName = (String) soapBody.getParts().get(0);
            return inputMessage.getPart(partName);
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

}