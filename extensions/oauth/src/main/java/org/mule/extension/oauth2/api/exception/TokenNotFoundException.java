/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.api.exception;

import static java.lang.String.format;
import static org.mule.extension.oauth2.api.exception.OAuthErrors.TOKEN_NOT_FOUND;

import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.extension.api.exception.ModuleException;
import org.mule.runtime.extension.api.runtime.operation.Result;

/**
 * It was not possible to retrieve the access token or the refresh token from the token URL response
 */
public class TokenNotFoundException extends ModuleException {

  private static final long serialVersionUID = -4161482371227766961L;

  public TokenNotFoundException(Result<Object, HttpResponseAttributes> tokenUrlResponse) {
    super(format("Could not extract access token or refresh token from token URL response: %s",
                 tokenUrlResponse.getOutput()),
          TOKEN_NOT_FOUND);
  }
}
