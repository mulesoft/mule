/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.dsl.api.xml;

/**
 * Mule DSL constants.
 */
public interface DslConstants {

  /**
   * This is the namespace prefix for core elements in the configuration.
   * 
   * The namespace is optional. All {@link org.mule.runtime.dsl.api.component.ComponentIdentifier}s created for core elements will
   * have this namespace.
   */
  String CORE_NAMESPACE = "mule";

}
