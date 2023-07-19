/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.security;

import org.mule.runtime.core.api.event.CoreEvent;

import java.io.Serializable;

/**
 * <code>CredentialsAccessor</code> is a template for obtaining user credentials from the current message.
 */
public interface CredentialsAccessor {

  Serializable getCredentials(CoreEvent event);

}
