/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.validation;

import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENABLE_POLLING_SOURCE_LIMIT_PARAMETER;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.POLLING;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;

import org.mule.functional.api.extension.TestComponentExtensionLoadingDelegate;
import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.vegan.extension.VeganExtension;
import org.mule.tests.api.TestComponentsExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
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
        .expectMessage("[validation/heisenberg-default-illegal-config.xml:10]: "
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
        .expectMessage("[validation/operation-with-expression-config-ref.xml:8]: "
            + "Element <heisenberg:config> is missing required parameter 'knownAddresses'.");
    loadConfiguration("validation/operation-with-expression-config-ref.xml");
  }

  @Test
  public void sourceWithExpressionConfigReference() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/source-with-expression-config-ref.xml:8]: "
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
    // FIXME W-10831629: there is an inconsistency between the element referred to by this error message and the one where there
    // are conflicting exclusive optionals
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[validation/petstore-exclusive-required-parameter.xml:9]: "
            + "Element <Breeder> requires that one of its optional parameters must be set, but all of them are missing. One of the following must be set: [mammals, birds].");
    loadConfiguration("validation/petstore-exclusive-required-parameter.xml");
  }

  @Test
  public void configLevelOperationNegative() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("[validation/vegan-invalid-config-for-operations.xml:16]");
    expectedException
        .expectMessage("Referenced component 'apple' must be one of stereotypes [VEGAN:BANANA-CONFIG]");
    loadConfiguration("validation/vegan-invalid-config-for-operations.xml");
  }

  @Test
  public void configLevelSourceNegative() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("[validation/vegan-invalid-config-for-sources.xml:16]");
    expectedException
        .expectMessage("Referenced component 'banana' must be one of stereotypes [VEGAN:APPLE_CONFIG].");
    loadConfiguration("validation/vegan-invalid-config-for-sources.xml");
  }

  @Test
  public void configRefPointsToNonExistant() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("[validation/config-ref-points-to-non-existant-config.xml:9]");
    expectedException
        .expectMessage("Referenced component 'nonExistant' must be one of stereotypes [PETSTORE:CONFIG].");
    loadConfiguration("validation/config-ref-points-to-non-existant-config.xml");
  }

  @Test
  public void routerStereotypeValidation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Invalid content was found starting with element");
    expectedException.expectMessage("set-variable");
    loadConfiguration("scopes/heisenberg-stereotype-validation-config.xml");
  }

  @Test
  @Feature(SOURCES)
  @Story(POLLING)
  @Issue("MULE-18631")
  public void negativePollingSourceLimitingValidation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("The 'maxItemsPerPoll' parameter must have a value greater than 1");
    loadConfiguration("source/negative-polling-source-limiting-config.xml");
  }

  @Test
  @Feature(SOURCES)
  @Story(POLLING)
  @Issue("MULE-18631")
  public void zeroPollingSourceLimitingValidation() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("The 'maxItemsPerPoll' parameter must have a value greater than 1");
    loadConfiguration("source/zero-polling-source-limiting-config.xml");
  }

  @Test
  @Issue("MULE-17906")
  public void dynamicStatefulOverride() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("[validation/dynamic-stateful-override-config.xml:11]");
    expectedException
        .expectMessage("Component uses a dynamic configuration and defines configuration override parameter 'optionalWithDefault' which is assigned on initialization. That combination is not supported. Please use a non dynamic configuration or don't set the parameter.");
    loadConfiguration("validation/dynamic-stateful-override-config.xml");
  }

  @Test
  @Issue("MULE-17906")
  public void dynamicStatefulOverrideImplicit() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("[validation/dynamic-stateful-override-implicit-config.xml:9]");
    expectedException
        .expectMessage("Component uses a dynamic configuration and defines configuration override parameter 'optionalWithDefault' which is assigned on initialization. That combination is not supported. Please use a non dynamic configuration or don't set the parameter.");
    loadConfiguration("validation/dynamic-stateful-override-implicit-config.xml");
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel petStore = loadExtension(PetStoreConnector.class, emptySet());
    ExtensionModel heisenberg = loadExtension(HeisenbergExtension.class, emptySet());
    ExtensionModel vegan = loadExtension(VeganExtension.class, emptySet());
    ExtensionModel implicit = loadExtension(ImplicitConfigExtension.class, emptySet());
    ExtensionModel testComponents = loadExtensionWithDelegate(TestComponentExtensionLoadingDelegate.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.addAll(super.getRequiredExtensions());
    extensions.add(petStore);
    extensions.add(heisenberg);
    extensions.add(vegan);
    extensions.add(implicit);
    extensions.add(testComponents);

    return extensions;
  }

  @Override
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return singletonMap(ENABLE_POLLING_SOURCE_LIMIT_PARAMETER, true);
  }
}
