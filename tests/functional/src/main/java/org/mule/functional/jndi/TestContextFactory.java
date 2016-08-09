/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.jndi;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Creates an in-memory context and populates it with test data.
 */
public class TestContextFactory extends InMemoryContextFactory {

  public Context getInitialContext() throws NamingException {
    Context context = super.getInitialContext();
    populateTestData(context);
    return context;
  }

  public Context getInitialContext(Hashtable environment) throws NamingException {
    Context context = super.getInitialContext(environment);
    populateTestData(context);
    return context;
  }

  protected void populateTestData(Context context) throws NamingException {
    context.bind("fruit/apple", new Apple());
    context.bind("fruit/banana", new Banana());
    context.bind("fruit/orange", new Orange(new Integer(8), new Double(10), "Florida Sunny"));
  }
}
