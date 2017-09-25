/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authFailedForUser;
import static org.mule.runtime.core.api.config.i18n.CoreMessages.authNoCredentials;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.security.AbstractAuthenticationFilter;
import org.mule.runtime.api.security.DefaultMuleAuthentication;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.api.security.UnauthorisedException;

/**
 * Performs authentication based on a username and password. The username and password are retrieved from the {@link Message}
 * based on expressions specified via the username and password setters. These are then used to create a DefaultMuleAuthentication
 * object which is passed to the authenticate method of the {@link SecurityManager}.
 */
public class UsernamePasswordAuthenticationFilter extends AbstractAuthenticationFilter {

  private String username = "#[mel:message.inboundProperties.username]";
  private String password = "#[mel:message.inboundProperties.password]";

  /**
   * Authenticates the current message.
   *
   * @param event the current message recieved
   * @throws SecurityException if authentication fails
   */
  @Override
  public SecurityContext authenticate(CoreEvent event)
      throws SecurityException, SecurityProviderNotFoundException, UnknownAuthenticationTypeException {
    Authentication authentication = getAuthenticationToken(event);
    Authentication authResult;
    try {
      authResult = getSecurityManager().authenticate(authentication);
    } catch (UnauthorisedException e) {
      // Authentication failed
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication request for user: " + username + " failed: " + e.toString());
      }
      throw new UnauthorisedException(authFailedForUser(authentication.getPrincipal().toString()), e);
    }

    // Authentication success
    if (logger.isDebugEnabled()) {
      logger.debug("Authentication success: " + authResult.toString());
    }

    SecurityContext context = getSecurityManager().createSecurityContext(authResult);
    context.setAuthentication(authResult);

    return context;
  }

  protected Authentication getAuthenticationToken(CoreEvent event) throws UnauthorisedException {
    ExpressionManager expressionManager = (ExpressionManager) registry.lookupByName(OBJECT_EXPRESSION_MANAGER).get();

    Object usernameEval = expressionManager.evaluate(username, event).getValue();
    Object passwordEval = expressionManager.evaluate(password, event).getValue();

    if (usernameEval == null) {
      throw new UnauthorisedException(authNoCredentials());
    }

    if (passwordEval == null) {
      throw new UnauthorisedException(authNoCredentials());
    }

    return new DefaultMuleAuthentication(new DefaultMuleCredentials(usernameEval.toString(),
                                                                    passwordEval.toString().toCharArray()));
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setPassword(String password) {
    this.password = password;
  }

}
