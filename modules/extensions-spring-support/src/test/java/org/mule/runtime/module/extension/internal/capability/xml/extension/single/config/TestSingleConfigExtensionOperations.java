/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class TestSingleConfigExtensionOperations {

  /**
   * Operation with description
   *
   * @param friend This is one of my friends
   * @param otherFriend Some other friend
   */
  public void operation(@Connection String friend, String otherFriend) {}

  /**
   * Operation with parameter group
   *
   * @param value value param description
   * @param group group description
   */
  public void operationWithParameterGroup(String value, @ParameterGroup(name = "Group") SingleConfigParameterGroup group) {}
}
