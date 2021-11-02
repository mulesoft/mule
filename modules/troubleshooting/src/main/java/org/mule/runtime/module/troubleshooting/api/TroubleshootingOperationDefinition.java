/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
