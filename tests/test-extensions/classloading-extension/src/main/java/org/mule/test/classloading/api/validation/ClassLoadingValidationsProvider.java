/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
