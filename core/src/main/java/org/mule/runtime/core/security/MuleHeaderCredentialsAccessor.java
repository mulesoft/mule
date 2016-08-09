/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.security;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.security.CredentialsAccessor;

import java.io.Serializable;

/**
 * <code>MuleHeaderCredentialsAccessor</code> obtains and sets the user credentials as Mule property headers.
 */
public class MuleHeaderCredentialsAccessor implements CredentialsAccessor {

  @Override
  public Serializable getCredentials(MuleEvent event) {
    return event.getMessage().getInboundProperty(MULE_USER_PROPERTY);
  }

  @Override
  public void setCredentials(MuleEvent event, Serializable credentials) {
    event.setMessage(MuleMessage.builder(event.getMessage()).addOutboundProperty(MULE_USER_PROPERTY, credentials).build());
  }
}
