/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source.legacy;

import org.mule.runtime.extension.api.runtime.source.Source;

/**
 * Interface that is meant to signal that the given instances delegate its responsabilities into a legacy {@link Source}
 *
 * @since 4.4.0
 */
public interface LegacySourceWrapper {

  /**
   * This method provide the instance of the legacy {@link Source} in with this implementation is delegating behavior.
   *
   * @return the delegate {@link Source} instance.
   */
  Source getDelegate();

}
