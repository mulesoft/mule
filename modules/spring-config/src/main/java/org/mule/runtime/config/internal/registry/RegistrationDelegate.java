/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.registry;

import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Map;

/**
 * Delegate interface for object registration
 *
 * @since 3.7.0
 */
interface RegistrationDelegate {

  void registerObject(String key, Object value) throws RegistrationException;

  void registerObject(String key, Object value, Object metadata) throws RegistrationException;

  void registerObjects(Map<String, Object> objects) throws RegistrationException;

  Object unregisterObject(String key) throws RegistrationException;
}
