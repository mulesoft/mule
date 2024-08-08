/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Marker {@link ModelProperty} to signal that the enriched component was written using the new Java sdk-api
 *
 * @since 4.8.0
 */
public class SdkApiDefinedModelProperty implements ModelProperty {

  public static final SdkApiDefinedModelProperty INSTANCE = new SdkApiDefinedModelProperty();

  private SdkApiDefinedModelProperty() {}

  @Override
  public String getName() {
    return "sdkApiDefined";
  }

  @Override
  public boolean isPublic() {
    return false;
  }
}
