/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api;

import org.mule.runtime.api.exception.MuleException;

/**
 * Marking interface to identity resources that need to be closed in order to release resources.
 */
@Deprecated
public interface Closeable {

  /**
   * Closes the resource. Calling this method is mandatory for any component using this instance once it finishes using it. This
   * method should not throw exception is invoked on an instance that has already been closed
   * 
   * @throws MuleException if an exception occurs closing the resource
   */
  void close() throws MuleException;

}
