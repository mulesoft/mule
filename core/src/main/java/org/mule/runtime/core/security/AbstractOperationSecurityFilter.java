/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;

/**
 * <code>AbstractEndpointSecurityFilter</code> provides basic initialisation for all security filters, namely configuring the
 * SecurityManager for this instance
 */
@Deprecated
public abstract class AbstractOperationSecurityFilter extends AbstractAuthenticationFilter {

  @Override
  public void doFilter(MuleEvent event) throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    super.doFilter(event);
  }

  public void authenticate(MuleEvent event) throws SecurityException, UnknownAuthenticationTypeException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
    // TODO - See MULE-9307 - define proper way to identify if the component should do inbound or outbound authentication
    authenticateInbound(event);
  }

  protected abstract void authenticateInbound(MuleEvent event) throws SecurityException, CryptoFailureException,
      SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException;

}
