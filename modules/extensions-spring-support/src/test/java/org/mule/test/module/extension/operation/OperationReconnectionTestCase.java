/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.operation;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;

import org.mule.test.marvel.ironman.IronMan;
import org.mule.test.marvel.model.Villain;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;

import org.junit.Test;

public class OperationReconnectionTestCase extends AbstractExtensionFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "operation-reconnection-config.xml";
  }

  @Test
  public void operationReconnection() throws Exception {
    final Villain villain = new Villain();
    flowRunner("operationReconnection").withPayload(villain).run();
    assertThat(villain.isAlive(), is(false));
    IronMan stark = getIronMan();
    assertThat(stark.getMissilesFired(), is(2));
  }

  @Test
  public void defaultReconnection() throws Exception {
    final Villain villain = new Villain();

    flowRunner("defaultReconnection").withPayload(villain).runExpectingException();

    assertThat(villain.isAlive(), is(true));
    IronMan stark = getIronMan();
    assertThat(stark.getMissilesFired(), is(1));
  }

  private IronMan getIronMan() throws Exception {
    return (IronMan) getConfigurationInstanceFromRegistry("ironMan", testEvent(), muleContext).getValue();
  }
}
