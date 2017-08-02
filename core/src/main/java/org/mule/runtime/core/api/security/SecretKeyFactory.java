/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy;

/**
 * A factory class for providing secret keys to an instance of {@link SecretKeyEncryptionStrategy}.
 * 
 * @see SecretKeyEncryptionStrategy
 */
public interface SecretKeyFactory {

  byte[] getKey();
}
