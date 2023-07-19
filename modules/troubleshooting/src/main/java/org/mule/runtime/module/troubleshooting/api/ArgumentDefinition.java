/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
