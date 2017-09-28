/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.processor;

/**
 * A configuration line provider allows to have a parent child relationship between {@code ConfigLine} while keeping the object
 * immutable.
 *
 * @since 4.0
 */
@FunctionalInterface
public interface ConfigLineProvider {

  /**
   * @return a {@code ConfigLine}.
   */
  ConfigLine getConfigLine();

}
