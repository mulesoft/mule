/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.store;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.store.ObjectStoreSettings;

/**
 * Utility class to perform conversions between components of the SDK API and the Mule API
 *
 * @since 4.5.0
 */
public class SdkObjectStoreUtils {

  /**
   * Converts the SDK api OS settings {@link org.mule.sdk.api.store.ObjectStoreSettings} into the corresponding Mule api OS
   * settings {@code ObjectStoreSettings}
   *
   * @param sdkObjectStoreSettings the SDK api OS settings to be converted
   * @return the converted Mule api OS settings
   */
  public static ObjectStoreSettings convertToMuleObjectStoreSettings(
                                                                     org.mule.sdk.api.store.ObjectStoreSettings sdkObjectStoreSettings) {
    checkArgument(sdkObjectStoreSettings != null, "Cannot convert null value");
    return convertSettings(sdkObjectStoreSettings);
  }

  /**
   * Converts the Mule object store settings {@code ObjectStoreSettings} into the corresponding SDK api OS settings
   * {@link org.mule.sdk.api.store.ObjectStoreSettings}.
   *
   * @param muleObjectStoreSettings the Mule api OS settings to be converted
   * @return the converted SDK api OS settings
   */
  public static org.mule.sdk.api.store.ObjectStoreSettings convertToSdkObjectStoreSettings(
                                                                                           ObjectStoreSettings muleObjectStoreSettings) {
    checkArgument(muleObjectStoreSettings != null, "Cannot convert null value");
    return convertSettings(muleObjectStoreSettings);
  }

  private static ObjectStoreSettings convertSettings(org.mule.sdk.api.store.ObjectStoreSettings objectStoreSettings) {
    return ObjectStoreSettings.builder()
        .persistent(objectStoreSettings.isPersistent())
        .maxEntries(objectStoreSettings.getMaxEntries().orElse(null))
        .entryTtl(objectStoreSettings.getEntryTTL().orElse(null))
        .expirationInterval(objectStoreSettings.getExpirationInterval())
        .build();
  }

  private static org.mule.sdk.api.store.ObjectStoreSettings convertSettings(ObjectStoreSettings objectStoreSettings) {
    return org.mule.sdk.api.store.ObjectStoreSettings.builder()
        .persistent(objectStoreSettings.isPersistent())
        .maxEntries(objectStoreSettings.getMaxEntries().orElse(null))
        .entryTtl(objectStoreSettings.getEntryTTL().orElse(null))
        .expirationInterval(objectStoreSettings.getExpirationInterval())
        .build();
  }
}
