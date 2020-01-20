/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.implicit.exclusive.config.extension.extension;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;

@Operations({OperationWithConfigOverride.class})
@Configuration(name = "bla-with-default")
public class ImplicitConfigWithOptionalParameter {

  public static final String OPTIONAL_PARAMETER_DEFAULT_VALUE = "Default Value";

  @Parameter
  @Optional(defaultValue = OPTIONAL_PARAMETER_DEFAULT_VALUE)
  private String optionalWithStaticDefault;

}
