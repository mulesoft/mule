/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.config;

import static java.util.Arrays.asList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Kiwi;
import org.mule.test.module.extension.AbstractExtensionFunctionalTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class ConfigLevelOperationTestCase extends AbstractExtensionFunctionalTestCase {

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{Apple.class}, {Banana.class}, {Kiwi.class}});
  }

  private final Class<? extends Fruit> fruitType;

  public ConfigLevelOperationTestCase(Class<? extends Fruit> fruitType) {
    this.fruitType = fruitType;
  }

  @Override
  protected String getConfigFile() {
    return "operations/vegan-config.xml";
  }

  @Test
  public void execute() throws Exception {
    Fruit fruit = (Fruit) flowRunner(fruitType.getSimpleName().toLowerCase() + "Ok").run().getMessage().getPayload().getValue();
    assertThat(fruit.getClass(), equalTo(fruitType));
    assertThat(fruit.isBitten(), is(true));
  }
}
