/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security.filter;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.api.security.SecurityContext;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for all security filters, namely configuring the
 * SecurityManager for this instance
 */
@Deprecated
public abstract class AbstractOperationSecurityFilter extends AbstractAuthenticationFilter {

  @Override
  public SecurityContext doFilter(CoreEvent event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    return super.doFilter(event);
  }

  @Override
  public SecurityContext authenticate(CoreEvent event)
      throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    // TODO - See MULE-9307 - define proper way to identify if the component should do inbound or outbound authentication
    return authenticateInbound(event);
  }

  protected abstract SecurityContext authenticateInbound(CoreEvent event) throws SecurityException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

}
