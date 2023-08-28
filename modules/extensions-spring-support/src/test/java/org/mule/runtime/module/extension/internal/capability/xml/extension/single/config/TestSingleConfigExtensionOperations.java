/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class TestSingleConfigExtensionOperations {

  /**
   * Operation with description
   *
   * @param friend      This is one of my friends
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
