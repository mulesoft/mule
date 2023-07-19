/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
