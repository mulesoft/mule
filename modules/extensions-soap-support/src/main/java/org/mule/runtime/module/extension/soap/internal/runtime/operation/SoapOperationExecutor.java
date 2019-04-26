/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.operation;

import static org.mule.runtime.api.metadata.DataType.INPUT_STREAM;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.BODY_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.HEADERS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.MESSAGE_GROUP;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.OPERATION_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.SERVICE_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.TRANSPORT_HEADERS_PARAM;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.transformer.MessageTransformerException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.runtime.module.extension.internal.runtime.client.strategy.ExtensionsClientProcessorsStrategyFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ExtensionsClientArgumentResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.StreamingHelperArgumentResolver;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.exception.error.SoapExceptionEnricher;
import org.mule.runtime.soap.api.message.SoapRequest;
import org.mule.runtime.soap.api.message.SoapRequestBuilder;
import org.mule.runtime.soap.api.message.SoapResponse;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

/**
 * {@link CompletableComponentExecutor} implementation that executes SOAP operations using a provided {@link SoapClient}.
 *
 * @since 4.0
 */
public final class SoapOperationExecutor implements CompletableComponentExecutor<OperationModel>, Initialisable {

  @Inject
  private MuleExpressionLanguage expressionExecutor;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ExtensionsClientProcessorsStrategyFactory extensionsClientProcessorsStrategyFactory;

  private final ConnectionArgumentResolver connectionResolver = new ConnectionArgumentResolver();
  private final StreamingHelperArgumentResolver streamingHelperArgumentResolver = new StreamingHelperArgumentResolver();
  private final SoapExceptionEnricher soapExceptionEnricher = new SoapExceptionEnricher();
  private ExtensionsClientArgumentResolver extensionsClientArgumentResolver;

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(ExecutionContext<OperationModel> context, ExecutorCallback callback) {
    try {
      String serviceId = context.getParameter(SERVICE_PARAM);
      ForwardingSoapClient connection = (ForwardingSoapClient) connectionResolver.resolve(context).get();
      Map<String, String> customHeaders = connection.getCustomHeaders(serviceId, getOperation(context));
      SoapRequest request = getRequest(context, customHeaders);
      SoapClient soapClient = connection.getSoapClient(serviceId);
      SoapResponse response = connection.getExtensionsClientDispatcher(() -> extensionsClientArgumentResolver
          .resolve(context)
          .get())
          .map(d -> soapClient.consume(request, d))
          .orElseGet(() -> soapClient.consume(request));

      callback.complete((response.getAsResult(streamingHelperArgumentResolver.resolve(context).get())));
    } catch (MessageTransformerException | TransformerException e) {
      callback.error(e);
    } catch (Exception e) {
      callback.error(soapExceptionEnricher.enrich(e));
    } catch (Throwable t) {
      callback.error(wrapFatal(t));
    }
  }

  public void initialise() {
    this.extensionsClientArgumentResolver = new ExtensionsClientArgumentResolver(extensionsClientProcessorsStrategyFactory);
  }

  /**
   * Builds a Soap Request with the execution context to be sent using the {@link SoapClient}.
   */
  private SoapRequest getRequest(ExecutionContext<OperationModel> context, Map<String, String> fixedHeaders)
      throws MessageTransformerException, TransformerException {
    SoapRequestBuilder builder = SoapRequest.builder().operation(getOperation(context));
    builder.soapHeaders(fixedHeaders);

    Optional<Object> optionalMessageGroup = getParam(context, MESSAGE_GROUP);
    if (optionalMessageGroup.isPresent()) {
      Map<String, Object> message = (Map<String, Object>) optionalMessageGroup.get();
      InputStream body = (InputStream) message.get(BODY_PARAM);
      if (body != null) {
        builder.content(body);
      }

      InputStream headers = (InputStream) message.get(HEADERS_PARAM);
      if (headers != null) {
        builder.soapHeaders((Map<String, String>) evaluateHeaders(headers));
      }

      Map<String, TypedValue<?>> attachments = (Map<String, TypedValue<?>>) message.get(ATTACHMENTS_PARAM);
      if (attachments != null) {
        toSoapAttachments(attachments).forEach(builder::attachment);
      }
    }

    getParam(context, TRANSPORT_HEADERS_PARAM)
        .ifPresent(th -> builder.transportHeaders((Map<String, String>) th));
    return builder.build();
  }

  private String getOperation(ExecutionContext<OperationModel> context) {
    return (String) getParam(context, OPERATION_PARAM)
        .orElseThrow(
                     () -> new IllegalStateException("Execution Context does not have the required operation parameter"));
  }

  private <T> Optional<T> getParam(ExecutionContext<OperationModel> context, String param) {
    return context.hasParameter(param) ? Optional.ofNullable(context.getParameter(param)) : Optional.empty();
  }

  private Object evaluateHeaders(InputStream headers) {
    String hs = IOUtils.toString(headers);
    BindingContext context = BindingContext.builder().addBinding("payload", new TypedValue<>(hs, DataType.XML_STRING)).build();
    return expressionExecutor.evaluate("%dw 2.0 \n"
        + "output application/java \n"
        + "---\n"
        + "payload.headers mapObject (value, key) -> {\n"
        + "    '$key' : write((key): value, \"application/xml\")\n"
        + "}", context).getValue();
  }

  private Map<String, SoapAttachment> toSoapAttachments(Map<String, TypedValue<?>> attachments)
      throws MessageTransformerException, TransformerException {
    Map<String, SoapAttachment> soapAttachmentMap = new HashMap<>();

    for (Map.Entry<String, TypedValue<?>> attachment : attachments.entrySet()) {
      SoapAttachment soapAttachment =
          new SoapAttachment(toInputStream(attachment.getValue()), attachment.getValue().getDataType().getMediaType());
      soapAttachmentMap.put(attachment.getKey(), soapAttachment);
    }

    return soapAttachmentMap;
  }

  private InputStream toInputStream(TypedValue typedValue) throws MessageTransformerException, TransformerException {

    Object value = typedValue.getValue();
    if (value instanceof InputStream) {
      return (InputStream) value;
    }
    return (InputStream) transformationService.transform(value, DataType.fromObject(value), INPUT_STREAM);
  }
}
