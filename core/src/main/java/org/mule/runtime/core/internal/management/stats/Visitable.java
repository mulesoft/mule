/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.management.stats;

import org.mule.runtime.core.api.management.stats.PayloadStatistics;

/**
 * Visitable interface to decorate an instance in order to capture {@link PayloadStatistics} from it.
 * 
 * @since 4.4, 4.3.1
 */
public interface Visitable<T> {

  /**
   * accepts the visitor and returns a transformation.
   * 
   * @param visitor visitor which also transforms the visitee.
   * @return the transformation.
   */
  T accept(Visitor visitor);

  /**
   * @return delegate
   */
  T getDelegate();

}
