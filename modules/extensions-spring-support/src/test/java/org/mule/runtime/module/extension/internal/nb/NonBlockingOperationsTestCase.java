/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.nb;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import static org.mule.test.marvel.ironman.IronManOperations.FLIGHT_PLAN;
import static org.mule.test.marvel.model.MissileProofVillain.MISSILE_PROOF;
import static org.mule.test.marvel.model.Villain.KABOOM;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.getConfigurationInstanceFromRegistry;
import org.mule.functional.junit4.ExtensionFunctionalTestCase;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.test.marvel.ironman.IronMan;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.marvel.model.MissileProofVillain;
import org.mule.test.marvel.model.Villain;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class NonBlockingOperationsTestCase extends ExtensionFunctionalTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected Class<?>[] getAnnotatedExtensionClasses() {
    return new Class<?>[] {MarvelExtension.class};
  }

  @Override
  protected String getConfigFile() {
    return "iron-man-config.xml";
  }

  @Test
  public void nonBlockingConnectedOperation() throws Exception {
    fireMissileAndAssert("fireMissile");
  }

  @Test
  public void failingNonBlockingConnectedOperation() throws Exception {
    expectedException.expect(instanceOf(MessagingException.class));
    expectedException.expectMessage(MISSILE_PROOF);
    expectedException.expectCause(instanceOf(UnsupportedOperationException.class));

    Villain villain = new MissileProofVillain();
    flowRunner("fireMissile").withPayload(villain).nonBlocking().run();

    assertThat(villain.isAlive(), is(true));
  }

  @Test
  public void nonBlockingOperationReconnection() throws Exception {
    fireMissileAndAssert("warMachineFireMissile");
    IronMan warMachine = getIronMan("warMachine");
    assertThat(warMachine.getMissilesFired(), is(2));
  }

  @Test
  public void voidNonBlockingOperation() throws Exception {
    IronMan ironMan = getIronMan("ironMan");
    flowRunner("computeFlightPlan").run();
    assertThat(ironMan.getFlightPlan(), is(FLIGHT_PLAN));
  }

  private IronMan getIronMan(String name) throws Exception {
    return (IronMan) getConfigurationInstanceFromRegistry(name, getInitialiserEvent(), muleContext).getValue();
  }

  private void fireMissileAndAssert(String flowName) throws Exception {
    Villain villain = new Villain();
    String result = (String) flowRunner(flowName)
        .withPayload(villain)
        .nonBlocking().run().getMessage().getPayload().getValue();

    assertThat(villain.isAlive(), is(false));
    assertThat(result, is(KABOOM));
  }

}
