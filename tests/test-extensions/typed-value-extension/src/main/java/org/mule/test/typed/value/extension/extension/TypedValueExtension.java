/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.typed.value.extension.extension;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

@Operations(TypedValueParameterOperations.class)
@Extension(name = "TypedValue")
@Sources(TypedValueSource.class)
@Import(type = KnockeableDoor.class)
@Import(type = DifferedKnockableDoor.class)
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
