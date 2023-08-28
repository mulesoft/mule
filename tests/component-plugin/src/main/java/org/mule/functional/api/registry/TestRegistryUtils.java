/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.functional.api.registry;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.LegacyRegistryUtils;
import org.mule.runtime.core.privileged.registry.RegistrationException;

/**
 * Provides a way for integration tests to access certain functionality of the internal {@link MuleRegistry}.
 *
 * @since 4.0
 *
 * @deprecated inject {@link Registry} where possible instead of using this utility.
 */
@Deprecated
public class TestRegistryUtils {

  /**
   * Registers an object in the registry with a key.
   *
   * @param key   the key to store the value against. This is a non-null value
   * @param value the object to store in the registry. This is a non-null value
   * @throws MuleRuntimeException wrapping a RegistrationException if an object with the same key already exists
   */
  public static void registerObject(MuleContext context, String key, Object value) {
    try {
      LegacyRegistryUtils.registerObject(context, key, value);
    } catch (RegistrationException e) {
      throw new MuleRuntimeException(e);
    }
  }


}
