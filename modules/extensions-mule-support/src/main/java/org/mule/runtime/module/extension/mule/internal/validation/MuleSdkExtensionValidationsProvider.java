/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.mule.internal.validation;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import org.mule.runtime.ast.api.validation.ArtifactValidation;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;

import java.util.List;

public class MuleSdkExtensionValidationsProvider implements ValidationsProvider {

  @Override
  public List<Validation> get() {
    return emptyList();
  }

  @Override
  public List<ArtifactValidation> getArtifactValidations() {
    return singletonList(new ExtensionStructureValidations());
  }
}
