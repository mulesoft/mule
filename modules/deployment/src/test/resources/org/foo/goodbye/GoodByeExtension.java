/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.goodbye;

import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.dsl.xml.Xml;
import org.mule.runtime.extension.api.annotation.param.Parameter;

/**
 * Extension for testing purposes
 */
@Extension(name = "Goodbye")
@Configurations({GoodByeConfiguration.class})
@Xml(prefix = "goodbye")
public class GoodByeExtension {

  @Parameter
  private String message;

  public GoodByeExtension() {
  }

}
