/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.config.extension;

import org.mule.runtime.core.internal.el.datetime.Date;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.Map;

@Extension(name = "SimpleExtension")
public class SimpleExtension {

  @Parameter
  private Map<String, Date> testParameter;

}
