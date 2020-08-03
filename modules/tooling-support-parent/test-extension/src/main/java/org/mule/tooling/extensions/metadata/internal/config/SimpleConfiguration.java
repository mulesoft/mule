/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tooling.extensions.metadata.internal.config;

import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.tooling.extensions.metadata.internal.operation.SimpleOperations;
import org.mule.tooling.extensions.metadata.internal.source.SimpleSource;
import org.mule.tooling.extensions.metadata.internal.value.ActingParameterVP;

@Operations({SimpleOperations.class})
@Sources({SimpleSource.class})
@Configuration(name="config")
public class SimpleConfiguration {

  @Parameter
  private String actingParameter;

  @Parameter
  @Optional
  @OfValues(ActingParameterVP.class)
  private String providedParameter;

}
