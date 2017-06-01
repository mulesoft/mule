/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.runtime.operation;

import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.ATTACHMENTS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.HEADERS_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.OPERATION_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.REQUEST_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.SERVICE_PARAM;
import static org.mule.runtime.module.extension.soap.internal.loader.SoapInvokeOperationDeclarer.TRANSPORT_HEADERS_PARAM;
import static reactor.core.publisher.Mono.error;
import static reactor.core.publisher.Mono.justOrEmpty;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.runtime.operation.OperationExecutor;
import org.mule.runtime.extension.api.soap.SoapAttachment;
import org.mule.runtime.module.extension.internal.runtime.resolver.ConnectionArgumentResolver;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.soap.api.client.SoapClient;
import org.mule.runtime.soap.api.message.SoapRequest;
import org.mule.runtime.soap.api.message.SoapRequestBuilder;
import org.mule.runtime.soap.api.message.SoapResponse;
import org.mule.runtime.soap.internal.exception.error.SoapExceptionEnricher;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.reactivestreams.Publisher;

/**
 * {@link OperationExecutor} implementation that executes SOAP operations using a provided {@link SoapClient}.
 *
 * @since 4.0
 */
public final class SoapOperationExecutor implements OperationExecutor {

  @Inject
  private MuleExpressionLanguage expressionExecutor;

  private final ConnectionArgumentResolver connectionResolver = new ConnectionArgumentResolver();
  private final SoapExceptionEnricher soapExceptionEnricher = new SoapExceptionEnricher();

  /**
   * {@inheritDoc}
   */
  @Override
  public Publisher<Object> execute(ExecutionContext<OperationModel> context) {
    try {
      String serviceId = context.getParameter(SERVICE_PARAM);
      ForwardingSoapClient connection = (ForwardingSoapClient) connectionResolver.resolve(context);
      Map<String, String> customHeaders = connection.getCustomHeaders(serviceId, getOperation(context));
      SoapRequest request = getRequest(context, customHeaders);
      SoapResponse response = connection.getSoapClient(serviceId).consume(request);
      return justOrEmpty(response.getAsResult());
    } catch (Exception e) {
      return error(soapExceptionEnricher.enrich(e));
    } catch (Throwable t) {
      return error(wrapFatal(t));
    }
  }

  /**
   * Builds a Soap Request with the execution context to be sent using the {@link SoapClient}.
   */
  private SoapRequest getRequest(ExecutionContext<OperationModel> context, Map<String, String> fixedHeaders) {
    Optional<InputStream> request = getParam(context, REQUEST_PARAM);
    Optional<InputStream> headers = getParam(context, HEADERS_PARAM);
    Optional<Map<String, SoapAttachment>> attachments = getParam(context, ATTACHMENTS_PARAM);
    Optional<Map<String, String>> transportHeaders = getParam(context, TRANSPORT_HEADERS_PARAM);

    SoapRequestBuilder builder = SoapRequest.builder().withOperation(getOperation(context));
    request.ifPresent(builder::withContent);
    headers.ifPresent(hs -> builder.withSoapHeaders((Map<String, String>) evaluateHeaders(hs)));
    builder.withSoapHeaders(fixedHeaders);
    transportHeaders.ifPresent(builder::withTransportHeaders);
    attachments.ifPresent(as -> as.forEach((k, v) -> {
      SoapAttachment attachment = new SoapAttachment(v.getContent(), v.getContentType());
      builder.withAttachment(k, attachment);
    }));

    return builder.build();
  }

  private String getOperation(ExecutionContext<OperationModel> context) {
    return (String) getParam(context, OPERATION_PARAM)
        .orElseThrow(() -> new IllegalStateException("Execution Context does not have the required operation parameter"));
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
}
