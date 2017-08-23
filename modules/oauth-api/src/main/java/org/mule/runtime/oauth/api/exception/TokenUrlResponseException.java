/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.oauth.api.exception;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.io.IOException;

/**
 * There was a problem with the call to the tokenUrl.
 */
public class TokenUrlResponseException extends MuleException {

  private static final long serialVersionUID = 201036315336735350L;

  public TokenUrlResponseException(String tokenUrl, HttpResponse response, String body) throws IOException {
    super(createStaticMessage(format("Error response when calling token URL '%s'. Response was:"
        + lineSeparator() + "%s"
        + lineSeparator() + "%s",
                                     tokenUrl, response.toString(), body)));

  }

  public TokenUrlResponseException(String tokenUrl, Exception cause) {
    super(createStaticMessage(format("Exception when calling token URL '%s'.", tokenUrl)), cause);

  }
}
