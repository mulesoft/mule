/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

import java.io.Serializable;

/**
 * Defines an argument for the troubleshooting command interpreter.
 *
 * @since 4.5
 * @see TroubleshootingOperationDefinition
 */
@Experimental
public interface ArgumentDefinition extends Serializable {

  /**
   * @return the argument name.
   */
  String getName();

  /**
   * @return the argument description.
   */
  String getDescription();

  /**
   * @return whether the argument is required or not.
   */
  boolean isRequired();
}
