/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal.listener;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.http.internal.multipart.HttpPartDataSource.multiPartPayloadForAttachments;
import static org.mule.runtime.module.http.internal.util.HttpToMuleMessage.getMediaType;
import static org.mule.service.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.service.http.api.HttpHeaders.Values.APPLICATION_X_WWW_FORM_URLENCODED;
import static org.mule.service.http.api.utils.HttpEncoderDecoderUtils.decodeUrlEncodedBody;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.error.HttpMessageParsingException;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.http.internal.listener.ListenerPath;
import org.mule.service.http.api.domain.entity.EmptyHttpEntity;
import org.mule.service.http.api.domain.entity.HttpEntity;
import org.mule.service.http.api.domain.entity.InputStreamHttpEntity;
import org.mule.service.http.api.domain.entity.multipart.MultipartHttpEntity;
import org.mule.service.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.api.domain.request.HttpRequestContext;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Component that transforms an HTTP request to a proper {@link Result}.
 *
 * @since 4.0
 */
public class HttpRequestToResult {

  public static Result<Object, HttpRequestAttributes> transform(final HttpRequestContext requestContext,
                                                                final Charset encoding,
                                                                Boolean parseRequest,
                                                                ListenerPath listenerPath)
      throws HttpMessageParsingException {
    final HttpRequest request = requestContext.getRequest();

    final MediaType mediaType = getMediaType(request.getHeaderValueIgnoreCase(CONTENT_TYPE), encoding);

    Object payload = null;
    if (parseRequest) {
      final HttpEntity entity = request.getEntity();
      if (entity != null && !(entity instanceof EmptyHttpEntity)) {
        if (entity instanceof MultipartHttpEntity) {
          try {
            payload = multiPartPayloadForAttachments((MultipartHttpEntity) entity);
          } catch (IOException e) {
            throw new HttpMessageParsingException(createStaticMessage("Unable to process multipart request"), e);
          }
        } else {
          if (mediaType != null) {
            if (mediaType.matches(APPLICATION_X_WWW_FORM_URLENCODED)) {
              try {
                payload = decodeUrlEncodedBody(IOUtils.toString(((InputStreamHttpEntity) entity).getInputStream()),
                                               mediaType.getCharset().get());
              } catch (IllegalArgumentException e) {
                throw new HttpMessageParsingException(createStaticMessage("Cannot decode %s payload",
                                                                          APPLICATION_X_WWW_FORM_URLENCODED.getSubType()),
                                                      e);
              }
            } else if (entity instanceof InputStreamHttpEntity) {
              payload = ((InputStreamHttpEntity) entity).getInputStream();
            }
          } else if (entity instanceof InputStreamHttpEntity) {
            payload = ((InputStreamHttpEntity) entity).getInputStream();
          }
        }
      }
    } else {
      final InputStreamHttpEntity inputStreamEntity = request.getInputStreamEntity();
      if (inputStreamEntity != null) {
        payload = inputStreamEntity.getInputStream();
      }
    }

    HttpRequestAttributes attributes =
        new HttpRequestAttributesBuilder().setRequestContext(requestContext).setListenerPath(listenerPath).build();

    return Result.<Object, HttpRequestAttributes>builder().output(payload).mediaType(mediaType).attributes(attributes).build();
  }

}
