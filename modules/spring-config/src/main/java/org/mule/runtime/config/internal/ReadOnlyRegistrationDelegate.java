/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.core.privileged.registry.RegistrationException;

import java.util.Map;

/**
 * Implementation of {@link RegistrationDelegate} which throws {@link UnsupportedOperationException} for every method
 *
 * @since 3.7.0
 */
final class ReadOnlyRegistrationDelegate implements RegistrationDelegate {

  @Override
  public void registerObject(String key, Object value) throws RegistrationException {
    unsupportedFeature();
  }

  @Override
  public void registerObject(String key, Object value, Object metadata) throws RegistrationException {
    unsupportedFeature();
  }

  @Override
  public void registerObjects(Map<String, Object> objects) throws RegistrationException {
    unsupportedFeature();
  }

  @Override
  public Object unregisterObject(String key) throws RegistrationException {
    unsupportedFeature();
    return null;
  }

  private void unsupportedFeature() {
    throw new UnsupportedOperationException("Registry is read-only so objects cannot be registered or unregistered.");
  }


}
