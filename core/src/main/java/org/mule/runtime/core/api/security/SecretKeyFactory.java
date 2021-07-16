/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.security;

import org.mule.api.annotation.NoImplement;

/**
 * A factory class for providing secret keys to an instance of {@code SecretKeyEncryptionStrategy}.
 * 
 * @see SecretKeyEncryptionStrategy
 * 
 * @deprecated SecretKeyEncryptionStrategy does not exist anymore.
 */
@NoImplement
@Deprecated
public interface SecretKeyFactory {

  byte[] getKey();
}
