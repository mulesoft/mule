/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
