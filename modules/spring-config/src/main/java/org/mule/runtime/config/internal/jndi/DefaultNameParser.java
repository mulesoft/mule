/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.jndi;

import javax.naming.CompositeName;
import javax.naming.Name;
import javax.naming.NameParser;
import javax.naming.NamingException;

/**
 * A default implementation of {@link NameParser}
 *
 */
public class DefaultNameParser implements NameParser {

  public Name parse(String name) throws NamingException {
    return new CompositeName(name);
  }
}
