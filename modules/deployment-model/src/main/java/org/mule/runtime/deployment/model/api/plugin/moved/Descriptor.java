/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved;

import java.util.Map;

/**
 * Holds the information of the class that needs to be read when looking for the classes
 *
 * @since 4.0
 */
public interface Descriptor {

  /**
   * @return the name to look for within all the handlers for. Non null.
   */
  String getId();

  /**
   * @return the set of attributes to feed the handler when found. Non null.
   */
  Map<String, Object> getAttributes();
}
