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

/**
 * It was not possible to retrieve the access token or the refresh token from the token URL response
 */
public class TokenNotFoundException extends MuleException {

  private static final long serialVersionUID = -4527896867466127563L;

  public TokenNotFoundException(String tokenUrl, HttpResponse response, String body) {
    super(createStaticMessage(format("Could not extract access token or refresh token from token URL '%s'. Response was:"
        + lineSeparator() + "%s"
        + lineSeparator() + "%s",
                                     tokenUrl, response.toString(), body)));
  }

}
