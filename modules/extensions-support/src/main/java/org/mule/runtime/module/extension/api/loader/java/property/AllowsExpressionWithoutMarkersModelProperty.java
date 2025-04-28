/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.property;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Keeps Java API compatibility
 *
 * @since 4.5
 *
 * @deprecated Use {@link org.mule.runtime.core.internal.extension.AllowsExpressionWithoutMarkersModelProperty} instead.
 */
@Deprecated
public class AllowsExpressionWithoutMarkersModelProperty implements ModelProperty {

  public static final String NAME = "allowsExpressionWithoutMarkers";

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean isPublic() {
    return false;
  }

}
