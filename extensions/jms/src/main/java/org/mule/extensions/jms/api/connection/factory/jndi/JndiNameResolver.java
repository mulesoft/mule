/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.connection.factory.jndi;



import org.mule.runtime.api.lifecycle.Lifecycle;

import javax.naming.NamingException;

/**
 * Defines a strategy for lookup objects by name using JNDI.
 *
 * @since 4.0
 */
public interface JndiNameResolver extends Lifecycle {

  /**
   * Looks up an object by name.
   *
   * @param name the name of the object to search for
   * @return the object if is found
   * @throws NamingException is there is an error during the lookup.
   */
  Object lookup(String name) throws NamingException;
}
