/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.validation;

import static java.util.Collections.emptySet;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * An {@link AbstractConfigurationFailuresTestCase} which is expected to point to a somewhat invalid config. The test fails if the
 * config is parsed correctly.
 *
 * @since 4.0
 */
@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class InvalidExtensionConfigTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void heisenbergMissingTls() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/heisenberg-missing-tls-connection-config.xml:17]: "
            + "Element <heisenberg:secure-connection> is missing required parameter 'tlsContext'.");
    loadConfiguration("validation/heisenberg-missing-tls-connection-config.xml");
  }

  @Test
  public void heisenbergDefaultConfigNegative() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/heisenberg-default-illegal-config.xml:21]: "
            + "Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/heisenberg-default-illegal-config.xml");
  }

  @Test
  public void petStoreMissingRequiredParameterInsidePojo() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-missing-required-parameter.xml:17]: "
            + "Element <petstore:phone-number> is missing required parameter 'areaCodes'.");
    loadConfiguration("validation/petstore-missing-required-parameter.xml");
  }

  @Test
  public void operationWithExpressionConfigReference() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/operation-with-expression-config-ref.xml:19]: "
            + "Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/operation-with-expression-config-ref.xml");
  }

  @Test
  public void sourceWithExpressionConfigReference() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/source-with-expression-config-ref.xml:20]: "
            + "Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/source-with-expression-config-ref.xml");
  }

  @Test
  public void petStoreExclusiveGroupPojo() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-group-pojo-config.xml:8]: "
            + "Element <config>, the following parameters cannot be set at the same time: [cash, debt].");
    loadConfiguration("validation/petstore-exclusive-group-pojo-config.xml");
  }

  @Test
  public void petStoreExclusiveGroupInsidePojo() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-group-inside-pojo-config.xml:13]: "
            + "Element <Aquarium>, the following parameters cannot be set at the same time: [frogName, fishName].");
    loadConfiguration("validation/petstore-exclusive-group-inside-pojo-config.xml");
  }

  @Test
  public void petStoreExclusiveParameterOperation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-parameters-operation.xml:16]: "
            + "Element <getBreeder>, the following parameters cannot be set at the same time: [mammals, birds].");
    loadConfiguration("validation/petstore-exclusive-parameters-operation.xml");
  }

  @Test
  public void petStoreExclusiveParameterWithAlias() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-parameter-with-alias.xml:8]: "
            + "Element <config>, the following parameters cannot be set at the same time: [cash, debt].");
    loadConfiguration("validation/petstore-exclusive-parameter-with-alias.xml");
  }

  @Test
  public void petStoreExclusiveGroupInsidePojoOneRequired() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-group-inside-pojo-one-required-config.xml:13]: "
            + "Element <Aquarium> requires that one of its optional parameters must be set, but all of them are missing. One of the following must be set: [frogName, fishName].");
    loadConfiguration("validation/petstore-exclusive-group-inside-pojo-one-required-config.xml");
  }

  @Test
  public void petStoreExclusiveParameterRequired() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-required-parameter.xml:9]: "
            + "Element <Breeder> requires that one of its optional parameters must be set, but all of them are missing. One of the following must be set: [mammals, birds].");
    loadConfiguration("validation/petstore-exclusive-required-parameter.xml");
  }

  // TODO MULE-19350 migrate and adapt this test
  @Test
  public void configLevelOperationNegative() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException
        .expectMessage("Root component 'appleEatsBanana' defines an usage of operation 'eatBanana' which points to configuration 'apple'. The selected config does not support that operation.");
    loadConfiguration("validation/vegan-invalid-config-for-operations.xml");
  }

  // TODO MULE-19350 migrate and adapt this test
  @Test
  public void configLevelSourceNegative() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException
        .expectMessage("Root component 'harvest-apples' defines an usage of operation 'harvest-apples' which points to configuration 'banana'. The selected config does not support that operation.");
    loadConfiguration("validation/vegan-invalid-config-for-sources.xml");
  }

  @Test
  public void routerStereotypeValidation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Invalid content was found starting with element");
    expectedException.expectMessage("set-variable");
    loadConfiguration("scopes/heisenberg-stereotype-validation-config.xml");
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel petStore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel heisenberg = loadExtension(HeisenbergExtension.class, emptySet());
    ExtensionModel vegan = loadExtension(VeganExtension.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(petStore);
    extensions.add(heisenberg);
    extensions.add(vegan);

    return extensions;
  }
}
