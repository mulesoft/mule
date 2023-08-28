/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.security;

import org.mule.runtime.core.api.security.SecretKeyFactory;

/**
 * Empty mock for tests
 */
public class MockKeyFactory extends Named implements SecretKeyFactory {

  public byte[] getKey() {
    return "key".getBytes();
  }

}
