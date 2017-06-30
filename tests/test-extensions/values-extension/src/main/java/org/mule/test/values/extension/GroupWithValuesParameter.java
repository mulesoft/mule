/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.values.extension;

import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.test.values.extension.resolver.SimpleValuesProvider;

public class GroupWithValuesParameter {

  @OfValues(SimpleValuesProvider.class)
  @Parameter
  String channels;

  @Optional
  @Parameter
  String anyParameter;

}
