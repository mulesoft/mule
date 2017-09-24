/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.security;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.UnauthorisedException;

public class TestSecurityFilter extends AbstractAuthenticationFilter {

  private boolean accept;
  private boolean called;

  public static final String SECURITY_EXCEPTION_MESSAGE = "unauthorized!!";

  public TestSecurityFilter(boolean accept) {
    this.accept = accept;
  }

  @Override
  public SecurityContext authenticate(CoreEvent event)
      throws SecurityException, CryptoFailureException, SecurityProviderNotFoundException,
      EncryptionStrategyNotFoundException, UnknownAuthenticationTypeException {
    called = true;
    if (!accept) {
      throw new StaticMessageUnauthorisedException();
    }
    return event.getSecurityContext();
  }

  @Override
  protected void doInitialise() throws InitialisationException {}

  public boolean wasCalled() {
    return called;
  }

  public static class StaticMessageUnauthorisedException extends UnauthorisedException {

    public StaticMessageUnauthorisedException() {
      super(createStaticMessage(""));
    }

    @Override
    public String getLocalizedMessage() {
      return SECURITY_EXCEPTION_MESSAGE;
    }
  }

}
