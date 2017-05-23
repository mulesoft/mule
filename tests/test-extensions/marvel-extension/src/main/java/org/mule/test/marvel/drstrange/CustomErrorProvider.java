/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.drstrange;

import static org.mule.test.marvel.drstrange.DrStrangeErrorTypeDefinition.CUSTOM_ERROR;
import org.mule.runtime.extension.api.annotation.error.ErrorTypeProvider;
import org.mule.runtime.extension.api.error.ErrorTypeDefinition;

import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class CustomErrorProvider implements ErrorTypeProvider {

  @Override
  public Set<ErrorTypeDefinition> getErrorTypes() {
    return ImmutableSet.<ErrorTypeDefinition>builder().add(CUSTOM_ERROR).build();
  }
}
