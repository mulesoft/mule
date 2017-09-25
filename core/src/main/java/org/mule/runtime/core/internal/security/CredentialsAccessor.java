/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
