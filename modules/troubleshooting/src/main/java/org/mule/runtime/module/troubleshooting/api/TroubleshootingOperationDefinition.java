/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.troubleshooting.api;

import org.mule.api.annotation.Experimental;

import java.io.Serializable;
import java.util.List;

/**
 * Defines an operation and its metadata.
 *
 * @since 4.5
 */
@Experimental
public interface TroubleshootingOperationDefinition extends Serializable {

  /**
   * @return the operation name.
   */
  String getName();

  /**
   * @return the operation description.
   */
  String getDescription();

  /**
   * @return the operation argument definitions.
   */
  List<ArgumentDefinition> getArgumentDefinitions();
}
