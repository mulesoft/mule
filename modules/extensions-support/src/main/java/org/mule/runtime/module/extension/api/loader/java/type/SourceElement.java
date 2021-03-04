/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.loader.java.type;

import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getSourceReturnType;
import org.mule.api.annotation.NoImplement;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.extension.api.runtime.source.Source;

import java.util.List;
import java.util.Optional;

/**
 * A contract for an element from which a message source can be derived
 *
 * @since 4.0
 */
@NoImplement
public interface SourceElement extends ParameterizableTypeElement, WithReturnType {

  /**
   * @return The list of generics of the super class {@link Source}
   */
  List<Type> getSuperClassGenerics();

  // TODO: MULE-9220 not more than one
  Optional<MethodElement> getOnResponseMethod();

  // TODO: MULE-9220 not more than one
  Optional<MethodElement> getOnErrorMethod();

  Optional<MethodElement> getOnTerminateMethod();

  Optional<MethodElement> getOnBackPressureMethod();

  @Override
  default Type getReturnType() {
    return getSuperClassGenerics().get(0);
  }

  @Override
  default MetadataType getReturnMetadataType() {
    return getSourceReturnType(getReturnType());
  }

  @Override
  default MetadataType getAttributesMetadataType() {
    return getSourceReturnType(getSuperClassGenerics().get(1));
  }

}
