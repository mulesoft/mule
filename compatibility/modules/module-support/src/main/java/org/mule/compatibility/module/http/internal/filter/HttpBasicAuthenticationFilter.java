/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.module.http.internal.filter;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;
import static org.mule.runtime.module.http.api.HttpConstants.ResponseProperties.HTTP_STATUS_PROPERTY;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.HttpResponseAttributes;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.UnsupportedAuthenticationSchemeException;
import org.mule.runtime.core.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.security.DefaultMuleAuthentication;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.runtime.module.http.internal.filter.BasicUnauthorisedException;
import org.mule.service.http.api.domain.ParameterMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter for basic authentication over an HTTP request
 */
public class HttpBasicAuthenticationFilter extends AbstractAuthenticationFilter {

  /**
   * logger used by this class
   */
  protected static final Logger logger = LoggerFactory.getLogger(HttpBasicAuthenticationFilter.class);

  private String realm;

  private boolean realmRequired = true;

  public HttpBasicAuthenticationFilter() {
    super();
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    if (realm == null) {
      if (isRealmRequired()) {
        throw new InitialisationException(createStaticMessage("The \"realm\" must be set on this security filter"), this);
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


  protected Authentication createAuthentication(String username, String password, Event event) {
    return new DefaultMuleAuthentication(new MuleCredentials(username, password.toCharArray()), event);
  }

  protected Event setUnauthenticated(Event event) {
    String realmHeader = "Basic realm=";
    if (realm != null) {
      realmHeader += "\"" + realm + "\"";
    }
    ParameterMap headers = new ParameterMap();
    headers.put(WWW_AUTHENTICATE, realmHeader);
    String finalRealmHeader = realmHeader;
    return Event.builder(event)
        .message(InternalMessage.builder(event.getMessage())
            .attributes(new HttpResponseAttributes(UNAUTHORIZED.getStatusCode(), UNAUTHORIZED.getReasonPhrase(), headers))
            .addOutboundProperty(WWW_AUTHENTICATE, finalRealmHeader)
            .addOutboundProperty(HTTP_STATUS_PROPERTY, UNAUTHORIZED.getStatusCode())
            .build())
        .build();
  }

  /**
   * Authenticates the current message if authenticate is set to true. This method will always populate the secure context in the
   * session
   *
   * @param event the current message recieved
   * @throws org.mule.runtime.core.api.security.SecurityException if authentication fails
   */
  @Override
  public Event authenticate(Event event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    String header;
    if (event.getMessage().getAttributes() instanceof HttpRequestAttributes) {
      header = ((HttpRequestAttributes) event.getMessage().getAttributes()).getHeaders().get(AUTHORIZATION.toLowerCase());
    } else {
      header = event.getMessage().getInboundProperty(AUTHORIZATION);
    }

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
      Authentication authentication = createAuthentication(username, password, event);

      try {
        authResult = getSecurityManager().authenticate(authentication);
      } catch (UnauthorisedException e) {
        // Authentication failed
        if (logger.isDebugEnabled()) {
          logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
        }
        event = setUnauthenticated(event);
        throw new BasicUnauthorisedException(authFailedForUser(username), e, event.getMessage());
      }

      // Authentication success
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication success: " + authResult.toString());
      }

      SecurityContext context = getSecurityManager().createSecurityContext(authResult);
      context.setAuthentication(authResult);
      event.getSession().setSecurityContext(context);
      return event;
    } else if (header == null) {
      event = setUnauthenticated(event);
      throw new BasicUnauthorisedException(event, event.getSession().getSecurityContext(), this);
    } else {
      event = setUnauthenticated(event);
      throw new UnsupportedAuthenticationSchemeException(createStaticMessage("Http Basic filter doesn't know how to handle header "
          + header), event.getMessage());
    }
  }
}
