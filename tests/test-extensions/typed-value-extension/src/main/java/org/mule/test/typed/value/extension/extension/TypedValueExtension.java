/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.typed.value.extension.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;

@Operations(TypedValueParameterOperations.class)
@Extension(name = "TypedValue")
@Sources(TypedValueSource.class)
public class TypedValueExtension {

  @Parameter
  @Optional
  TypedValue<String> stringTypedValue;

  @Parameter
  @Optional
  DifferedKnockableDoor differedDoor;

  public TypedValue<String> getStringTypedValue() {
    return stringTypedValue;
  }

  public DifferedKnockableDoor getDifferedDoor() {
    return differedDoor;
  }
}
