/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
