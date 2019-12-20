/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
