/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * <code>SecurityFilter</code> is a base filter for secure filtering of inbound and outbound event flow.
 */
public interface SecurityFilter extends Initialisable {

  SecurityManager getSecurityManager();

  String getSecurityProviders();

  SecurityContext doFilter(CoreEvent event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException;
}
