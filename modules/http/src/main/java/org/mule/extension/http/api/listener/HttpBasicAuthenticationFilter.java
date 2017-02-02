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
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.UnsupportedAuthenticationSchemeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.module.http.internal.filter.BasicUnauthorisedException;
import org.mule.service.http.api.domain.ParameterMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Filter for basic authentication over an HTTP request
 */
public class HttpBasicAuthenticationFilter extends AbstractAuthenticationFilter {

  protected static final Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

  private String realm;

  private boolean realmRequired = true;
  private HttpRequestAttributes attributes;

  /**
   * Creates a filter based on the HTTP listener error response builder status code and headers configuration.
   */
  public HttpBasicAuthenticationFilter() {
    super();
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (realm == null) {
      if (isRealmRequired()) {
        throw new InitialisationException(createStaticMessage("The realm must be set on this security filter"), this);
      } else {
        logger.warn("There is no security realm set, using default: null");
      }
    }
  }

  public String getRealm() {
    return realm;
  }

  public void setRealm(String realm) {
    this.realm = realm;
  }

  public boolean isRealmRequired() {
    return realmRequired;
  }

  public void setRealmRequired(boolean realmRequired) {
    this.realmRequired = realmRequired;
  }

  public void setAttributes(HttpRequestAttributes attributes) {
    this.attributes = attributes;
  }


  protected Authentication createAuthentication(String username, String password) {
    return new DefaultMuleAuthentication(new DefaultMuleCredentials(username, password.toCharArray()));
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

  /**
   * Authenticates the current message if authenticate is set to true. This method will always populate the secure context in the
   * {@link Event} session
   *
   * @param event the current message received
   * @throws SecurityException if authentication fails
   */
  @Override
  public Event authenticate(Event event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
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

      Authentication authResult;
      Authentication authentication = createAuthentication(username, password);

      try {
        authResult = getSecurityManager().authenticate(authentication);
      } catch (UnauthorisedException e) {
        if (logger.isDebugEnabled()) {
          logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
        }
        throw new BasicUnauthorisedException(authFailedForUser(username), e, createUnauthenticatedMessage());
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Authentication success: " + authResult.toString());
      }

      SecurityContext context = getSecurityManager().createSecurityContext(authResult);
      context.setAuthentication(authResult);
      event.getSession().setSecurityContext(context);
      return event;
    } else if (header == null) {
      throw new BasicUnauthorisedException(null, this, "HTTP listener", createUnauthenticatedMessage());
    } else {
      throw new UnsupportedAuthenticationSchemeException(createStaticMessage("Http Basic filter doesn't know how to handle header "
          + header), createUnauthenticatedMessage());
    }
  }
}
