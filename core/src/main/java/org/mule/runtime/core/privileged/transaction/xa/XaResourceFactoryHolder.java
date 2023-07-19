/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.transaction.xa;

import org.mule.api.annotation.NoImplement;

/**
 * Implementations of this interface hold a reference to a factory of XAResource objects.
 *
 * This class is used in those classes that wrap jdbc XADataSource or jms XAConnectionFactory or any other factory of XA resources
 * and allows to retrieve the actual implementation.
 */
@NoImplement
public interface XaResourceFactoryHolder {

  /**
   * @return the {@link javax.transaction.xa.XAResource} provider
   */
  Object getHoldObject();

}
