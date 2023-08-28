/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.security;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy;

/**
 * A factory class for providing secret keys to an instance of {@link SecretKeyEncryptionStrategy}.
 * 
 * @see SecretKeyEncryptionStrategy
 */
@NoImplement
public interface SecretKeyFactory {

  byte[] getKey();
}
