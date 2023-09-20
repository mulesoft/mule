/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import org.mule.runtime.core.api.event.CoreEvent;

import java.util.Collection;

import org.junit.Test;

public class ModuleUsingListTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "flows/flow-with-module-using-list.xml";
  }

  @Override
  protected String[] getModulePaths() {
    return new String[] {"modules/module-using-list.xml"};
  }

  @Test
  public void testPetstoreWithList() throws Exception {
    CoreEvent event = flowRunner("testPetstoreWithList").run();
    Collection<String> pets = (Collection<String>) event.getMessage().getPayload().getValue();
    assertThat(pets, containsInAnyOrder("Dog", "Cat"));
  }
}
