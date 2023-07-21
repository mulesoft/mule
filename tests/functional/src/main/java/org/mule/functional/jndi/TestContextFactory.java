/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
