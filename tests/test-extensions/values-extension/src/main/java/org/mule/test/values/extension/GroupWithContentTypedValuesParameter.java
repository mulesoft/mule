/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.values.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.Parameter;

public class GroupWithContentTypedValuesParameter {

  @Parameter
  @Content(primary = true)
  private TypedValue<Object> typedBody;

  public TypedValue<Object> getBody() {
    return typedBody;
  }

}
