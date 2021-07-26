/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.security;

import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.message.InternalMessage;

import java.io.Serializable;

/**
 * <code>MuleHeaderCredentialsAccessor</code> obtains and sets the user credentials as Mule property headers.
 */
public class MuleHeaderCredentialsAccessor implements CredentialsAccessor {

  @Override
  public Serializable getCredentials(CoreEvent event) {
    return ((InternalMessage) event.getMessage()).getInboundProperty(MULE_USER_PROPERTY);
  }
}
