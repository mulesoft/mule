/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.transaction.xa;

/**
 * Implementations of this interface hold a reference to a factory of XAResource objects.
 *
 * This class is used in those classes that wrap jdbc XADataSource or jms XAConnectionFactory or any other factory of XA resources
 * and allows to retrieve the actual implementation.
 */
public interface XaResourceFactoryHolder {

  /**
   * @return the {@link javax.transaction.xa.XAResource} provider
   */
  Object getHoldObject();

}
