/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.internal;

import static com.google.common.net.HttpHeaders.AUTHORIZATION;
import static com.google.common.net.HttpHeaders.WWW_AUTHENTICATE;
import static org.apache.commons.codec.binary.Base64.decodeBase64;
import static org.mule.runtime.core.DefaultMuleEvent.getFlowVariableOrNull;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.runtime.core.util.Preconditions.checkArgument;
import static org.mule.runtime.module.http.api.HttpConstants.HttpStatus.UNAUTHORIZED;

import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.runtime.core.DefaultMuleEvent;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.UnsupportedAuthenticationSchemeException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.security.DefaultMuleAuthentication;
import org.mule.runtime.core.security.MuleCredentials;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Filter for basic authentication over an HTTP request
 */
public class HttpBasicAuthenticationFilter extends AbstractAuthenticationFilter {

  protected static final Log logger = LogFactory.getLog(HttpBasicAuthenticationFilter.class);

  private String realm;

  private boolean realmRequired = true;
  private final String headersFlowVar;
  private final String statusCodeFlowVar;

  /**
   * Creates a filter based on the HTTP listener error response builder status code and headers configuration.
   *
   * @param statusCodeFlowVar The flow variable name used to set the HTTP response status code.
   * @param headersFlowVar The flow variable name used to set the HTTP response headers.
   */
  public HttpBasicAuthenticationFilter(String statusCodeFlowVar, String headersFlowVar) {
    super();
    this.statusCodeFlowVar = statusCodeFlowVar;
    this.headersFlowVar = headersFlowVar;
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


  protected Authentication createAuthentication(String username, String password, MuleEvent event) {
    return new DefaultMuleAuthentication(new MuleCredentials(username, password.toCharArray()), event);
  }

  protected void setUnauthenticated(MuleEvent event) {
    String realmHeader = "Basic realm=";
    if (realm != null) {
      realmHeader += "\"" + realm + "\"";
    }
    Map<String, String> headers = getFlowVariableOrNull(headersFlowVar, event);
    if (headers == null) {
      headers = new HashMap<>();
      event.setFlowVariable(headersFlowVar, headers);
    }
    headers.put(WWW_AUTHENTICATE, realmHeader);
    event.setFlowVariable(statusCodeFlowVar, UNAUTHORIZED.getStatusCode());
  }

  /**
   * Authenticates the current message if authenticate is set to true. This method will always populate the secure context in the
   * {@link MuleEvent} session
   *
   * @param event the current message received
   * @throws SecurityException if authentication fails
   */
  @Override
  public void authenticate(MuleEvent event) throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    checkArgument(event.getMessage().getAttributes() instanceof HttpRequestAttributes,
                  "Message attributes must be HttpRequestAttributes.");
    String header = ((HttpRequestAttributes) event.getMessage().getAttributes()).getHeaders().get(AUTHORIZATION.toLowerCase());

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
        if (logger.isDebugEnabled()) {
          logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
        }
        setUnauthenticated(event);
        throw new UnauthorisedException(CoreMessages.authFailedForUser(username), event, e);
      }

      if (logger.isDebugEnabled()) {
        logger.debug("Authentication success: " + authResult.toString());
      }

      SecurityContext context = getSecurityManager().createSecurityContext(authResult);
      context.setAuthentication(authResult);
      event.getSession().setSecurityContext(context);
    } else if (header == null) {
      setUnauthenticated(event);
      throw new UnauthorisedException(event, event.getSession().getSecurityContext(), this);
    } else {
      setUnauthenticated(event);
      throw new UnsupportedAuthenticationSchemeException(createStaticMessage("Http Basic filter doesn't know how to handle header "
          + header), event);
    }
  }
}
