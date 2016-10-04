/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.ws.consumer;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mule.runtime.core.message.DefaultEventBuilder.EventImplementation.getVariableValueOrNull;
import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;
import static org.mule.runtime.core.util.IOUtils.toDataHandler;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
import org.mule.runtime.core.api.connector.DispatchException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.FlowConstructAware;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.api.lifecycle.Disposable;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.registry.RegistrationException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.config.i18n.I18nMessageFactory;
import org.mule.runtime.core.exception.WrapperErrorMessageAwareException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.processor.AbstractRequestResponseMessageProcessor;
import org.mule.runtime.core.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.processor.chain.DefaultMessageProcessorChainBuilder;
import org.mule.runtime.core.util.Base64;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.module.cxf.CxfConstants;
import org.mule.runtime.module.cxf.CxfOutboundMessageProcessor;
import org.mule.runtime.module.cxf.builder.ProxyClientMessageProcessorBuilder;
import org.mule.runtime.module.ws.security.SecurityStrategy;
import org.mule.runtime.module.ws.security.WSSecurity;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
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


public class WSConsumer
    implements Processor, Initialisable, MuleContextAware, FlowConstructAware, Disposable, NonBlockingMessageProcessor {

  public static final String SOAP_HEADERS_PROPERTY_PREFIX = "soap.";

  private static final Logger logger = LoggerFactory.getLogger(WSConsumer.class);

  private MuleContext muleContext;
  private FlowConstruct flowConstruct;
  private String operation;
  private WSConsumerConfig config;
  private Processor messageProcessor;
  private String soapAction;
  private String requestBody;
  private SoapVersion soapVersion;
  private boolean mtomEnabled;

  @Override
  public void initialise() throws InitialisationException {
    initializeConfiguration();
    parseWsdl();

    try {
      messageProcessor = createMessageProcessor();
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }


  @Override
  public Event process(Event event) throws MuleException {
    return messageProcessor.process(event);
  }


  /**
   * Initializes the configuration for this web service consumer. If no reference to WSConsumerConfig is set, then it is looked up
   * in the registry (only one object of this type must exist in the registry, or an InitializationException will be thrown).
   */
  private void initializeConfiguration() throws InitialisationException {
    if (config == null) {
      try {
        config = muleContext.getRegistry().lookupObject(WSConsumerConfig.class);
        if (config == null) {
          throw new InitialisationException(CoreMessages
              .createStaticMessage("No configuration defined for the web service " + "consumer. Add a consumer-config element."),
                                            this);
        }
      } catch (RegistrationException e) {
        throw new InitialisationException(e, this);
      }
    }
  }

  /**
   * Creates the message processor chain in which we will delegate the process of mule events. The chain composes a string
   * transformer, a CXF client proxy and and outbound endpoint.
   */
  private Processor createMessageProcessor() throws MuleException {
    MessageProcessorChainBuilder chainBuilder = new DefaultMessageProcessorChainBuilder();

    chainBuilder.chain(createCopyAttachmentsMessageProcessor());

    // Add a message processor that removes the invocation property CxfConstants.OPERATION if present
    // (as it may change the behavior of CXF proxy client). It is added again after executing the proxy client.
    chainBuilder.chain(createPropertyRemoverMessageProcessor(CxfConstants.OPERATION));

    chainBuilder.chain(createCxfOutboundMessageProcessor(config.getSecurity()));

    chainBuilder.chain(createSoapHeadersPropertiesRemoverMessageProcessor());

    chainBuilder.chain(config.createOutboundMessageProcessor(flowConstruct));

    MessageProcessorChain chain = chainBuilder.build();
    chain.setFlowConstruct(flowConstruct);
    chain.setMuleContext(muleContext);
    return chain;
  }

  private Processor createCopyAttachmentsMessageProcessor() {
    return new AbstractRequestResponseMessageProcessor() {

      @Override
      protected Event processRequest(Event event) throws MuleException {
        InternalMessage message = event.getMessage();
        final Event.Builder builder = Event.builder(event);
        /*
         * If the requestBody variable is set, it will be used as the payload to send instead of the payload of the message. This
         * will happen when an operation required no input parameters.
         */
        if (requestBody != null) {
          message = InternalMessage.builder(event.getMessage()).payload(requestBody).build();
          builder.message(message);
        }

        copyAttachmentsRequest(builder, message);

        return super.processRequest(builder.build());
      }

      @Override
      protected Event processNext(Event event) throws MuleException {
        try {
          return super.processNext(event);
        } catch (MessagingException e) {
          /*
           * When a Soap Fault is returned in the response, CXF raises a SoapFault exception. We need to wrap the information of
           * this exception into a new exception of the WS consumer module
           */

          if (e.getCause() instanceof DispatchException && e.getCause().getCause() instanceof SoapFault) {
            SoapFault soapFault = (SoapFault) e.getCause().getCause();
            org.mule.runtime.api.message.Message errorMessage = e.getEvent().getMessage();
            if (e.getEvent().getError().isPresent() && e.getEvent().getError().get().getErrorMessage() != null) {
              errorMessage = e.getEvent().getError().get().getErrorMessage();
            }

            SoapFaultException soapFaultException = new SoapFaultException(soapFault);
            throw new WrapperErrorMessageAwareException(InternalMessage.builder(errorMessage)
                .payload(soapFault.getDetail() != null ? soapFault.getDetail() : null).build(), soapFaultException);
          } else {
            throw e;
          }
        }
      }

      @Override
      protected Event processResponse(Event response, final Event request) throws MuleException {
        return super.processResponse(copyAttachmentsResponse(response), request);
      }
    };
  }

  private Processor createPropertyRemoverMessageProcessor(final String propertyName) {
    return new AbstractRequestResponseMessageProcessor() {

      private Object propertyValue;

      @Override
      protected Event processRequest(Event event) throws MuleException {
        propertyValue = getVariableValueOrNull(propertyName, event);
        event = Event.builder(event).removeVariable(propertyName).build();
        return super.processRequest(event);
      }

      @Override
      protected Event processResponse(Event response, final Event request) throws MuleException {
        if (propertyValue != null) {
          response = Event.builder(response).addVariable(propertyName, propertyValue).build();
        }
        return super.processResponse(response, request);
      }
    };
  }

  private Processor createSoapHeadersPropertiesRemoverMessageProcessor() {
    return new AbstractRequestResponseMessageProcessor() {

      @Override
      protected Event processRequest(Event event) throws MuleException {
        // Remove outbound properties that are mapped to SOAP headers, so that the
        // underlying transport does not include them as headers.

        List<String> outboundProperties = new ArrayList<>(event.getMessage().getOutboundPropertyNames());

        InternalMessage.Builder builder = InternalMessage.builder(event.getMessage());
        for (String outboundProperty : outboundProperties) {
          if (outboundProperty.startsWith(SOAP_HEADERS_PROPERTY_PREFIX)) {
            builder.removeOutboundProperty(outboundProperty);
          }
        }

        return super.processRequest(Event.builder(event).message(builder.build()).build());
      }

      @Override
      protected Event processResponse(Event response, final Event request) throws MuleException {
        // Ensure that the http.status code inbound property (if present) is a String.
        Object statusCode = response.getMessage().getInboundProperty(HTTP_STATUS_PROPERTY, null);
        if (statusCode != null && !(statusCode instanceof String)) {
          response = Event.builder(request).message(InternalMessage.builder(response.getMessage())
              .addInboundProperty(HTTP_STATUS_PROPERTY, statusCode.toString()).build()).build();
        }
        return super.processResponse(response, request);
      }
    };
  }

  /**
   * Creates the CXF message processor that will be used to create the SOAP envelope.
   */
  private CxfOutboundMessageProcessor createCxfOutboundMessageProcessor(WSSecurity security) throws MuleException {
    ProxyClientMessageProcessorBuilder cxfBuilder = new ProxyClientMessageProcessorBuilder();
    Map<String, Object> outConfigProperties = new HashMap<>();
    Map<String, Object> inConfigProperties = new HashMap<>();

    cxfBuilder.setMtomEnabled(mtomEnabled);
    cxfBuilder.setMuleContext(muleContext);
    cxfBuilder.setSoapVersion(soapVersion.getVersion());

    if (security != null && security.hasStrategies()) {
      for (SecurityStrategy strategy : security.getStrategies()) {
        strategy.apply(outConfigProperties, inConfigProperties);
      }
      if (cxfBuilder.getOutInterceptors() == null) {
        cxfBuilder.setOutInterceptors(new ArrayList<Interceptor<? extends Message>>());
      }
      if (cxfBuilder.getInInterceptors() == null) {
        cxfBuilder.setInInterceptors(new ArrayList<Interceptor<? extends Message>>());
      }

      if (!outConfigProperties.isEmpty()) {
        cxfBuilder.getOutInterceptors().add(new WSS4JOutInterceptor(outConfigProperties));
      }
      if (!inConfigProperties.isEmpty()) {
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

    if (soapAction != null) {
      cxfOutboundMessageProcessor.getClient().getOutInterceptors().add(new SoapActionInterceptor(soapAction));
    }

    cxfOutboundMessageProcessor.getClient().getOutInterceptors().add(new InputSoapHeadersInterceptor(muleContext));
    cxfOutboundMessageProcessor.getClient().getInInterceptors().add(new OutputSoapHeadersInterceptor(muleContext));



    return cxfOutboundMessageProcessor;
  }

  /**
   * Parses the WSDL file in order to validate the service, port and operation, to get the SOAP Action (if defined) and to check
   * if the operation requires input parameters or not.
   */
  private void parseWsdl() throws InitialisationException {
    Definition wsdlDefinition = null;

    URL url = IOUtils.getResourceAsUrl(config.getWsdlLocation(), getClass());
    if (url == null) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Can't find wsdl at %s", config.getWsdlLocation()),
                                        this);
    }

    try {
      WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();

      URLConnection urlConnection = url.openConnection();
      if (url.getUserInfo() != null) {
        urlConnection.setRequestProperty("Authorization", "Basic " + Base64.encodeBytes(url.getUserInfo().getBytes()));
      }

      wsdlDefinition = wsdlReader.readWSDL(url.toString(), new InputSource(urlConnection.getInputStream()));
    } catch (WSDLException | IOException e) {
      throw new InitialisationException(e, this);
    }

    Service service = wsdlDefinition.getService(new QName(wsdlDefinition.getTargetNamespace(), config.getService()));
    if (service == null) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Service %s not found in WSDL",
                                                                               config.getService()),
                                        this);
    }

    Port port = service.getPort(config.getPort());
    if (port == null) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Port %s not found in WSDL", config.getPort()),
                                        this);
    }

    Binding binding = port.getBinding();
    if (binding == null) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Port %s has no binding", config.getPort()), this);
    }

    BindingOperation bindingOperation = binding.getBindingOperation(this.operation, null, null);
    if (bindingOperation == null) {
      throw new InitialisationException(I18nMessageFactory.createStaticMessage("Operation %s not found in WSDL", this.operation),
                                        this);
    }

    this.soapVersion = WSDLUtils.getSoapVersion(binding);
    this.soapAction = getSoapAction(bindingOperation);

    RequestBodyGenerator requestBodyGenerator = new RequestBodyGenerator(wsdlDefinition);
    this.requestBody = requestBodyGenerator.generateRequestBody(bindingOperation);

  }

  /**
   * Returns the SOAP action related to an operation, or null if not specified.
   */
  private String getSoapAction(BindingOperation bindingOperation) {
    List extensions = bindingOperation.getExtensibilityElements();
    for (Object extension : extensions) {
      if (extension instanceof SOAPOperation) {
        return ((SOAPOperation) extension).getSoapActionURI();
      }
      if (extension instanceof SOAP12Operation) {
        return ((SOAP12Operation) extension).getSoapActionURI();
      }
    }
    return null;
  }

  /**
   * Reads outbound attachments from the Message and sets the CxfConstants.ATTACHMENTS invocation properties with a set of CXF
   * Attachment objects.
   */
  private void copyAttachmentsRequest(Event.Builder eventBuilder, InternalMessage message) throws MessagingException {
    final Builder builder = InternalMessage.builder(message);

    List<Attachment> attachments = new ArrayList<>();
    for (String outboundAttachmentName : message.getOutboundAttachmentNames()) {
      attachments
          .add(new AttachmentImpl(outboundAttachmentName, message.getOutboundAttachment(outboundAttachmentName)));
    }

    try {
      if (message.getPayload().getValue() instanceof MultiPartPayload) {
        for (org.mule.runtime.api.message.Message part : ((MultiPartPayload) message.getPayload().getValue()).getParts()) {
          final String partName = ((PartAttributes) part.getAttributes()).getName();
          attachments
              .add(new AttachmentImpl(partName, toDataHandler(partName, part.getPayload().getValue(),
                                                              part.getPayload().getDataType().getMediaType())));
        }
        builder.nullPayload();
      }
    } catch (IOException e) {
      throw new MessagingException(CoreMessages.createStaticMessage("Exception processing attachments."), eventBuilder.build(), e,
                                   this);
    }

    eventBuilder.addVariable(CxfConstants.ATTACHMENTS, attachments).message(builder.outboundAttachments(emptyMap()).build());
  }

  /**
   * Takes the set of CXF attachments from the CxfConstants.ATTACHMENTS invocation properties and sets them as inbound attachments
   * in the Mule Message.
   *
   * @return
   */
  private Event copyAttachmentsResponse(Event event) throws MessagingException {
    InternalMessage message = event.getMessage();

    if (event.getVariable(CxfConstants.ATTACHMENTS).getValue() != null) {
      Collection<Attachment> attachments = (Collection<Attachment>) event.getVariable(CxfConstants.ATTACHMENTS).getValue();
      InternalMessage.Builder builder = InternalMessage.builder(message);

      if (!attachments.isEmpty()) {
        List<org.mule.runtime.api.message.Message> parts = new ArrayList<>();
        parts.add(InternalMessage.builder().payload(message.getPayload().getValue())
            .mediaType(message.getPayload().getDataType().getMediaType())
            .attributes(BODY_ATTRIBUTES).build());

        for (Attachment attachment : attachments) {
          Map<String, LinkedList<String>> headers = new HashMap<>();
          for (Iterator<String> iterator = attachment.getHeaderNames(); iterator.hasNext();) {
            String headerName = iterator.next();
            headers.put(headerName, new LinkedList<>(singletonList(attachment.getHeader(headerName))));
          }

          try {
            parts.add(InternalMessage.builder().payload(attachment.getDataHandler().getInputStream())
                .mediaType(MediaType.parse(attachment.getDataHandler().getContentType()))
                .attributes(new PartAttributes(attachment.getId())).build());
          } catch (Exception e) {
            throw new MessagingException(CoreMessages.createStaticMessage("Could not set inbound attachment %s",
                                                                          attachment.getId()),
                                         event, e, this);
          }
        }

        builder.payload(new DefaultMultiPartPayload(parts));
      }
      return Event.builder(event).message(builder.build()).build();
    } else {
      return event;
    }

  }

  @Override
  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  @Override
  public void setFlowConstruct(FlowConstruct flowConstruct) {
    this.flowConstruct = flowConstruct;
  }

  public WSConsumerConfig getConfig() {
    return config;
  }

  public void setConfig(WSConsumerConfig config) {
    this.config = config;
  }

  public String getOperation() {
    return operation;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }

  public boolean isMtomEnabled() {
    return mtomEnabled;
  }

  public void setMtomEnabled(boolean mtomEnabled) {
    this.mtomEnabled = mtomEnabled;
  }

  @Override
  public void dispose() {
    if (messageProcessor instanceof Disposable) {
      ((Disposable) messageProcessor).dispose();
    }
  }
}
