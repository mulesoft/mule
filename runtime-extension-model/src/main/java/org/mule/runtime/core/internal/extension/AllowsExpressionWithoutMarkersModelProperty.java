/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.extension;

import org.mule.runtime.api.meta.model.ModelProperty;

/**
 * Declares that parameters with this property may have its value set to an expression without the {@code #[...]} markers and
 * still be considered an expression.
 * <p>
 *
 * @since 1.4
 * @deprecated This exists for backwards compatibility with previously existing components.
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
