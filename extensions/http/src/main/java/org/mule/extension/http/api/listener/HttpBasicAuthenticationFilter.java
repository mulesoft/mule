/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.listener;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.service.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;

import org.mule.extension.http.api.HttpListenerResponseAttributes;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.internal.filter.BasicUnauthorisedException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.Credentials;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnsupportedAuthenticationSchemeException;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.security.AuthenticationHandler;
import org.mule.service.http.api.domain.ParameterMap;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Filter for basic authentication over an HTTP request.
 *
 * @since 4.0
 */
public class HttpBasicAuthenticationFilter {

  protected static final Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

  /**
   * Authentication realm.
   */
  @Parameter
  private String realm;

  /**
   * The delegate-security-provider to use for authenticating. Use this in case you have multiple security managers defined in
   * your configuration.
   */
  @Parameter
  @Optional
  @NullSafe
  private List<String> securityProviders;

  /**
   * The {@link HttpRequestAttributes} coming from an HTTP listener source to check the 'Authorization' header.
   */
  @Parameter
  @Optional(defaultValue = "#[attributes]")
  HttpRequestAttributes attributes;

  /**
   * Authenticates an HTTP message based on the provided {@link HttpRequestAttributes}.
   *
   * @throws SecurityException if authentication fails
   */
  public void authenticate(AuthenticationHandler authenticationHandler)
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    String header = attributes.getHeaders().get(AUTHORIZATION.toLowerCase());

    if (logger.isDebugEnabled()) {
      logger.debug("Authorization header: " + header);
    }

    if ((header != null) && header.startsWith("Basic ")) {
      String base64Token = header.substring(6);
      String token = new String(decodeBase64(base64Token.getBytes()));

      String username = "";
      String password = "";
      int delim = token.indexOf(":");

      if (delim != -1) {
        username = token.substring(0, delim);
        password = token.substring(delim + 1);
      }

      Credentials credentials = authenticationHandler.createCredentialsBuilder()
          .withUsername(username)
          .withPassword(password.toCharArray())
          .build();

      try {
        authenticationHandler.setAuthentication(securityProviders, authenticationHandler.createAuthentication(credentials));
      } catch (UnauthorisedException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
        }
        throw new BasicUnauthorisedException(authFailedForUser(username), e, createUnauthenticatedMessage());
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Authentication success.");
      }

    } else if (header == null) {
      throw new BasicUnauthorisedException(null, "HTTP basic authentication", "HTTP listener", createUnauthenticatedMessage());
    } else {
      throw new UnsupportedAuthenticationSchemeException(createStaticMessage("Http Basic filter doesn't know how to handle header "
          + header), createUnauthenticatedMessage());
    }
  }

  private Message createUnauthenticatedMessage() {
    String realmHeader = "Basic realm=";
    if (realm != null) {
      realmHeader += "\"" + realm + "\"";
    }
    ParameterMap headers = new ParameterMap();
    headers.put(WWW_AUTHENTICATE, realmHeader);
    return Message.builder().nullPayload().attributes(new HttpListenerResponseAttributes(UNAUTHORIZED.getStatusCode(),
                                                                                         UNAUTHORIZED.getReasonPhrase(),
                                                                                         headers))
        .build();
  }

}
