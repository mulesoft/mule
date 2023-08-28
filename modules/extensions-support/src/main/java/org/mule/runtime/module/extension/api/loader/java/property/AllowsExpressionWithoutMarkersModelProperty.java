/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
