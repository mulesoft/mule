/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.data.sample;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JSON;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_XML;
import static org.mule.runtime.ast.api.util.MuleAstUtils.createComponentParameterizationFromComponentAst;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.lang.System.lineSeparator;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.fail;

import org.mule.functional.junit4.AbstractArtifactAstTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultExtensionDesignTimeResolversFactory;
import org.mule.sdk.api.data.sample.SampleDataException;
import org.mule.test.data.sample.extension.SampleDataExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;

public abstract class AbstractSampleDataTestCase extends AbstractArtifactAstTestCase {

  protected static final String EXPECTED_PAYLOAD = "my payload";
  protected static final String EXPECTED_ATTRIBUTES = "my attributes";
  protected static final String CONF_PREFIX = "from-conf-";
  protected static final String NULL_VALUE = "<<null>>";

  private ExtensionModel sampleDataExtension;

  private ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory;
  private ConfigurationProviderFactory configurationProviderFactory;
  private SampleDataExecutor sampleDataExecutor;

  @Before
  public void createExtensionDesignTimeResolversFactory() throws InitialisationException {
    extensionDesignTimeResolversFactory = new DefaultExtensionDesignTimeResolversFactory();
    initialiseIfNeeded(extensionDesignTimeResolversFactory, true, muleContext);
    configurationProviderFactory = new DefaultConfigurationProviderFactory();
    initialiseIfNeeded(configurationProviderFactory, true, muleContext);

    sampleDataExecutor = new SampleDataExecutor(extensionDesignTimeResolversFactory);
  }

  @Override
  protected Set<ExtensionModel> getRequiredExtensions() {
    final var extensions = new HashSet<ExtensionModel>();
    extensions.add(getExtensionModel());
    sampleDataExtension = loadExtension(SampleDataExtension.class, emptySet());
    extensions.add(sampleDataExtension);
    return extensions;
  }

  protected void assertMessage(SampleDataResult result, String payload, String attributes) {
    if (result.getSampleData().isPresent()) {
      assertMessage(result.getSampleData().orElseThrow(), payload, attributes);
    } else {
      fail(result.getFailure().orElseThrow().getReason());
    }
  }

  protected void assertMessage(Message message, String payload, String attributes) {
    assertThat(message.getPayload().getValue(), equalTo(payload));
    assertThat(message.getPayload().getDataType().getMediaType().matches(APPLICATION_JSON), is(true));
    assertThat(message.getAttributes().getValue(), equalTo(attributes));
    assertThat(message.getAttributes().getDataType().getMediaType().matches(APPLICATION_XML), is(true));
  }

  protected void assertError(SampleDataResult result, String failureCode, String errorMessage) {
    final var failure = result.getFailure().orElseThrow();
    assertThat(failure.getFailureCode(), equalTo(failureCode));
    assertThat(failure.getMessage(), equalTo(errorMessage));
  }

  protected void assertError(SampleDataResult result, String failureCode, String errorMessage,
                             Class<? extends Exception> expectedCause) {
    final var failure = result.getFailure().orElseThrow();
    assertThat(failure.getFailureCode(), equalTo(failureCode));
    assertThat(failure.getMessage(), equalTo(errorMessage));
    assertThat(failure.getReason(),
               containsString(lineSeparator() + "Caused by: " + expectedCause.getName() + ": "));
  }

  protected SampleDataResult getOperationSampleByLocation(String flowName) throws SampleDataException {
    final var operationAst = getFlowComponent(flowName, OPERATION);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(operationAst)
        .map(this::createConfigurationProvider);

    return sampleDataExecutor
        .getSampleData(operationAst.getExtensionModel(),
                       createComponentParameterizationFromComponentAst(operationAst),
                       configurationProvider);
  }

  protected SampleDataResult getSourceSampleByLocation(String flowName) throws SampleDataException {
    final var sourceAst = getFlowComponent(flowName, SOURCE);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(sourceAst)
        .map(this::createConfigurationProvider);

    return sampleDataExecutor
        .getSampleData(sourceAst.getExtensionModel(),
                       createComponentParameterizationFromComponentAst(sourceAst),
                       configurationProvider);
  }

  protected SampleDataResult getSampleByComponentName(String componentName,
                                                      Map<String, Object> parameters,
                                                      String configName)
      throws SampleDataException {
    final var componentModel = sampleDataExtension.findComponentModel(componentName).orElseThrow();

    ComponentParameterization.Builder builder = ComponentParameterization.builder(componentModel);
    parameters.entrySet().stream()
        .forEach(e -> builder.withParameter(e.getKey(), e.getValue()));

    Optional<ConfigurationProvider> configurationProvider = empty();
    if (configName != null) {
      configurationProvider = of(createConfigurationProvider(configName));
    }

    return sampleDataExecutor.getSampleData(sampleDataExtension,
                                            builder.build(),
                                            configurationProvider);
  }

  private ConfigurationProvider createConfigurationProvider(String configName) {
    final var configAst = getTopLevelComponent(configName);

    final var configParameterization = createComponentParameterizationFromComponentAst(configAst);

    return extensionDesignTimeResolversFactory.createConfigurationProvider(sampleDataExtension,
                                                                           (ConfigurationModel) configParameterization
                                                                               .getModel(),
                                                                           configName,
                                                                           configParameterization
                                                                               .getParameters()
                                                                               .entrySet()
                                                                               .stream()
                                                                               .collect(toMap(e -> e.getKey()
                                                                                   .getSecond()
                                                                                   .getName(),
                                                                                              Entry::getValue)),
                                                                           empty(),
                                                                           empty(),
                                                                           configurationProviderFactory,
                                                                           null,
                                                                           null,
                                                                           SampleDataExecutor
                                                                               .getClassLoader(sampleDataExtension));
  }

  protected Map<String, Object> getDefaultParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("payload", "my payload");
    params.put("attributes", "my attributes");

    return params;
  }

  protected Map<String, Object> getGroupParameters() {
    Map<String, Object> params = new HashMap<>();
    params.put("groupParameter", "my payload");
    params.put("optionalParameter", "my attributes");

    return params;
  }
}
