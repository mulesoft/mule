/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.functional;

import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.petstore.extension.PetCage;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import org.junit.Test;

@Feature(XML_SDK)
@Issue("W-13681772")
public class ModuleUsingMapTestCase extends AbstractCeXmlExtensionMuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "flows/flow-with-module-using-map.xml";
  }

  @Override
  protected String[] getModulePaths() {
    return new String[] {"modules/module-using-map.xml"};
  }

  @Test
  public void testPetstoreWithMap() throws Exception {
    CoreEvent event = flowRunner("testPetstoreWithMap").run();
    PetCage cage = (PetCage) event.getMessage().getPayload().getValue();
    assertThat(cage.getBirds().get("Parrot"), equalTo(10));
    assertThat(cage.getBirds().get("Parakeet"), equalTo(15));
  }
}
