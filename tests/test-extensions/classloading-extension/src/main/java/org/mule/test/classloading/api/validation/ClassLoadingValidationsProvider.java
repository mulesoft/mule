/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.classloading.api.validation;

import static java.util.Collections.singletonList;

import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationsProvider;
import org.mule.test.classloading.internal.validation.CLValidation;

import java.util.List;

public class ClassLoadingValidationsProvider implements ValidationsProvider {

  @Override
  public List<Validation> get() {
    return singletonList(new CLValidation());
  }

}
