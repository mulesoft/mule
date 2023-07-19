/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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


