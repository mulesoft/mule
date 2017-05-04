/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.ws.internal;


import static org.apache.commons.lang.StringUtils.isBlank;
import org.mule.extension.ws.api.SoapMessageBuilder;
import org.mule.extension.ws.internal.metadata.ConsumeAttributesResolver;
import org.mule.extension.ws.internal.metadata.ConsumeOutputResolver;
import org.mule.extension.ws.internal.metadata.OperationKeysResolver;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.error.Throws;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.metadata.OutputResolver;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.services.soap.api.client.SoapClient;
import org.mule.services.soap.api.exception.SoapFaultException;
import org.mule.services.soap.api.message.SoapAttributes;
import org.mule.services.soap.api.message.SoapRequest;
import org.mule.services.soap.api.message.SoapRequestBuilder;
import org.mule.services.soap.api.message.SoapResponse;

import java.util.Map;

import javax.inject.Inject;

/**
 * The only {@link WebServiceConsumer} operation. the {@link ConsumeOperation} consumes an operation of the connected web service
 * and returns it's response.
 * <p>
 * The consume operation expects an XML body and a set of headers and attachments if required.
 * <p>
 *
 * @since 4.0
 */
public class ConsumeOperation {

  @Inject
  private MuleExpressionLanguage expressionExecutor;

  /**
   * Consumes an operation from a SOAP Web Service.
   *
   * @param connection the connection resolved to execute the operation.
   * @param operation  the name of the web service operation that aims to invoke.
   * @param message    the constructed SOAP message to perform the request.
   */
  @OnException(WscExceptionEnricher.class)
  @Throws(ConsumeErrorTypeProvider.class)
  @OutputResolver(output = ConsumeOutputResolver.class, attributes = ConsumeAttributesResolver.class)
  public Result<?, SoapAttributes> consume(@Connection SoapClient connection,
                                           @MetadataKeyId(OperationKeysResolver.class) String operation,
                                           @ParameterGroup(name = "Message", showInDsl = true) SoapMessageBuilder message)
      throws SoapFaultException {
    SoapRequestBuilder requestBuilder = getSoapRequest(operation, message);
    SoapResponse response = connection.consume(requestBuilder.build());
    return response.getAsResult();
  }

  private SoapRequestBuilder getSoapRequest(String operation, SoapMessageBuilder message) {
    SoapRequestBuilder requestBuilder = SoapRequest.builder();
    requestBuilder.withAttachments(message.getAttachments());
    requestBuilder.withOperation(operation);

    if (!isBlank(message.getHeaders())) {
      requestBuilder.withSoapHeaders((Map<String, String>) evaluateHeaders(message.getHeaders()));
    }

    if (!isBlank(message.getBody())) {
      requestBuilder.withContent(message.getBody());
    }
    return requestBuilder;
  }

  private Object evaluateHeaders(String headers) {
    BindingContext context =
        BindingContext.builder().addBinding("payload", new TypedValue<>(headers, DataType.XML_STRING)).build();
    return expressionExecutor.evaluate("%dw 2.0 \n"
        + "%output application/java \n"
        + "---\n"
        + "payload.headers mapObject (value, key) -> {\n"
        + "    '$key' : write((key): value, \"application/xml\")\n"
        + "}", context).getValue();
  }
}
