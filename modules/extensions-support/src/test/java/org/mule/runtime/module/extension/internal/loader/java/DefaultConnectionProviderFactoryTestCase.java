/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.runtime.connectivity.ConnectionProviderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.petstore.extension.SimplePetStoreConnectionProvider;

import org.junit.Test;

@SmallTest
public class DefaultConnectionProviderFactoryTestCase extends AbstractMuleTestCase {

  private ConnectionProviderFactory factory =
      new DefaultConnectionProviderFactory<>(SimplePetStoreConnectionProvider.class,
                                             SimplePetStoreConnectionProvider.class.getClassLoader());

  @Test
  public void getObjectType() {
    assertThat(factory.getObjectType(), equalTo(SimplePetStoreConnectionProvider.class));
  }

  @Test
  public void newInstance() throws Exception {
    assertThat(factory.newInstance(), is(instanceOf(SimplePetStoreConnectionProvider.class)));
  }

  @Test
  public void returnsDifferentInstances() throws Exception {
    assertThat(factory.newInstance(), is(not(sameInstance(factory.newInstance()))));
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void notProviderClass() {
    new DefaultConnectionProviderFactory<>(Object.class, getClass().getClassLoader());
  }

  @Test(expected = IllegalArgumentException.class)
  public void notInstantiable() {
    new DefaultConnectionProviderFactory<>(ConnectionProvider.class, ConnectionProvider.class.getClassLoader());
  }
}
