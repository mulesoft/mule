/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.multiple.config;

import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;

public class TestDocumentedExtensionOperations extends AbstractTestDocumentedOperations {

  /**
   * Test Operation
   *
   * @param value test value
   */
  public void operation(String value, @ParameterGroup(name = "group") TestDocumentedParameterGroup group) {

  }

  /**
   * This method greets a friend
   *
   * @param friend This is one of my friends
   * @param otherFriend Some other friend
   */
  public void greetFriend(String friend, String otherFriend) {}

  /**
   * Test Operation with blank parameter description
   *
   * @param value
   */
  public void operationWithBlankParameterDescription(@Connection String con, String value) {

  }

  /**
   * Operation that returns a {@link String} value
   *
   * @param value this is the {@link String} to be returned
   */
  public void operationWithJavadocLinkReferences(String value) {

  }

  /**
   * This operation should not be documented
   *
   * @param value test value
   */
  @Ignore
  public void ignoreOperationShouldBeIgnored(String value) {

  }

  /**
   * Private operation should not be documented
   *
   * @param value test value
   */
  private void privateOperationShouldBeIgnored(String value) {

  }
}
