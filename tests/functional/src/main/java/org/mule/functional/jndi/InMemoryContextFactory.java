/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.jndi;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;

/**
 * Simple in-memory JNDI context for unit testing.
 */
public class InMemoryContextFactory implements InitialContextFactory {

  public Context getInitialContext() throws NamingException {
    return getInitialContext(null);
  }

  public Context getInitialContext(Hashtable environment) throws NamingException {
    return new InMemoryContext();
  }
}


