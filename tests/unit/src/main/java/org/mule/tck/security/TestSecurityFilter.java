/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.security;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.core.api.security.SecurityException;
import org.mule.runtime.core.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.core.api.security.UnauthorisedException;
import org.mule.runtime.core.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.security.AbstractAuthenticationFilter;

public class TestSecurityFilter extends AbstractAuthenticationFilter {

  private boolean accept;
  private boolean called;

  public static final String SECURITY_EXCEPTION_MESSAGE = "unauthorized!!";

  public TestSecurityFilter(boolean accept) {
    this.accept = accept;
  }

  @Override
  public void authenticate(MuleEvent event) throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
      EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException {
    called = true;
    if (!accept) {
      throw new StaticMessageUnauthorisedException();
    }
  }

  @Override
  protected void doInitialise() throws InitialisationException {}

  public boolean wasCalled() {
    return called;
  }

  public static class StaticMessageUnauthorisedException extends UnauthorisedException {

    public StaticMessageUnauthorisedException() {
      super(null);
    }

    @Override
    public String getLocalizedMessage() {
      return SECURITY_EXCEPTION_MESSAGE;
    }
  }

}
