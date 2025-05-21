/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.typed.value.extension.extension;

import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Import;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.test.heisenberg.extension.model.DifferedKnockableDoor;
import org.mule.test.heisenberg.extension.model.KnockeableDoor;

@Operations(TypedValueParameterOperations.class)
@JavaVersionSupport({JAVA_21, JAVA_17})
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
