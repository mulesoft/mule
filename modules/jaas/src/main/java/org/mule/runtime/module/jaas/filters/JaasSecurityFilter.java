/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.jaas.filters;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.Authentication;
import org.mule.runtime.core.api.security.Credentials;
import org.mule.runtime.core.api.security.CredentialsNotSetException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.module.jaas.JaasAuthentication;
import org.mule.runtime.core.security.AbstractOperationSecurityFilter;
import org.mule.runtime.core.security.MuleCredentials;
import org.mule.runtime.core.security.MuleHeaderCredentialsAccessor;

public class JaasSecurityFilter extends AbstractOperationSecurityFilter {

  public JaasSecurityFilter() {
    setCredentialsAccessor(new MuleHeaderCredentialsAccessor());
  }

  @Override
  protected final void authenticateInbound(MuleEvent event)
      throws SecurityException, CryptoFailureException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException {
    String userHeader = (String) getCredentialsAccessor().getCredentials(event);
    if (userHeader == null) {
      throw new CredentialsNotSetException(event, event.getSession().getSecurityContext(), this);
    }

    Credentials user = new MuleCredentials(userHeader, getSecurityManager());
    Authentication authResult;
    JaasAuthentication authentication = new JaasAuthentication(user);
    authentication.setEvent(event);
    try {
      authResult = getSecurityManager().authenticate(authentication);
    } catch (SecurityException se) {
      // Security Exception occurred
      if (logger.isDebugEnabled()) {
        logger.debug("Security Exception raised. Authentication request for user: " + user.getUsername() + " failed: "
            + se.toString());
      }
      throw se;
    } catch (Exception e) {
      // Authentication failed
      if (logger.isDebugEnabled()) {
        logger.debug("Authentication request for user: " + user.getUsername() + " failed: " + e.toString());
      }
      throw new UnauthorisedException(CoreMessages.authFailedForUser(user.getUsername()), event, e);
    }

    // Authentication success
    if (logger.isDebugEnabled()) {
      logger.debug("Authentication success: " + authResult.toString());
    }

    SecurityContext context = getSecurityManager().createSecurityContext(authResult);
    context.setAuthentication(authResult);
    event.getSession().setSecurityContext(context);
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    // empty constructor
  }
}

