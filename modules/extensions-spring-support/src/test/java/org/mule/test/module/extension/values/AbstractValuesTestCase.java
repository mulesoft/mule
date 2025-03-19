/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.values;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.CONNECTION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.ast.api.ArtifactType.APPLICATION;
import static org.mule.runtime.ast.api.util.MuleAstUtils.createComponentParameterizationFromComponentAst;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.sdk.api.stereotype.MuleStereotypes.CONFIG;
import static org.mule.tck.junit4.matcher.value.ValueResultSuccessMatcher.isSuccess;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.SDK_TOOLING_SUPPORT;
import static org.mule.test.allure.AllureConstants.SdkToolingSupport.ValueProvidersStory.VALUE_PROVIDERS_SERVICE;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsIterableContaining.hasItems;

import org.mule.functional.junit4.CachingAstXmlParser;
import org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.api.value.ValueResult;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.module.extension.api.runtime.config.ConfigurationProviderFactory;
import org.mule.runtime.module.extension.api.runtime.config.ExtensionDesignTimeResolversFactory;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultConfigurationProviderFactory;
import org.mule.runtime.module.extension.internal.runtime.config.DefaultExtensionDesignTimeResolversFactory;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.junit4.matcher.ValueMatcher;
import org.mule.test.module.extension.data.sample.SampleDataExecutor;
import org.mule.test.values.extension.ValuesExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;

import org.hamcrest.Matcher;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SDK_TOOLING_SUPPORT)
@Story(VALUE_PROVIDERS_SERVICE)
public abstract class AbstractValuesTestCase extends AbstractMuleContextTestCase {

  private static final CachingAstXmlParser AST_PARSER =
      new CachingAstXmlParser(true, false, emptyMap(), APPLICATION, emptyArtifact());

  private ExtensionModel valuesExtension;

  private ExtensionDesignTimeResolversFactory extensionDesignTimeResolversFactory;
  private ConfigurationProviderFactory configurationProviderFactory;

  private ArtifactAst appAst;

  @Before
  public void loadAst() {
    appAst = AST_PARSER.parse(this.getClass().getName(),
                              getRequiredExtensions(),
                              this.getClass().getClassLoader(),
                              empty(),
                              null,
                              new String[] {getConfigFile()});
  }

  @Before
  public void createExtensionDesignTimeResolversFactory() throws InitialisationException {
    extensionDesignTimeResolversFactory = new DefaultExtensionDesignTimeResolversFactory();
    initialiseIfNeeded(extensionDesignTimeResolversFactory, true, muleContext);
    configurationProviderFactory = new DefaultConfigurationProviderFactory();
    initialiseIfNeeded(configurationProviderFactory, true, muleContext);
  }

  protected abstract String getConfigFile();

  protected Set<ExtensionModel> getRequiredExtensions() {
    final var extensions = new HashSet<ExtensionModel>();
    extensions.add(getExtensionModel());
    valuesExtension = loadExtension(ValuesExtension.class, emptySet());
    extensions.add(valuesExtension);
    return extensions;
  }

  protected ExtensionModel loadExtension(Class extension, Set<ExtensionModel> deps) {
    DefaultJavaExtensionModelLoader loader = new DefaultJavaExtensionModelLoader();
    return loadExtensionWithLoader(extension, deps, loader);
  }

  protected ExtensionModel loadExtensionWithLoader(Class extension, Set<ExtensionModel> deps,
                                                   ExtensionModelLoader extensionModelLoader) {
    Map<String, Object> ctx = new HashMap<>();
    ctx.put(TYPE_PROPERTY_NAME, extension.getName());
    ctx.put(VERSION, "4.10.0");
    ctx.putAll(getExtensionLoaderContextAdditionalParameters());
    return extensionModelLoader.loadExtensionModel(currentThread().getContextClassLoader(), DslResolvingContext.getDefault(deps),
                                                   ctx);
  }

  /**
   * Subclasses can override this method so that extension models are generated with an extension loading context that contains
   * the parameters returned by this method.
   *
   * @return a map with parameters to be added to the extension loader context.
   */
  protected Map<String, Object> getExtensionLoaderContextAdditionalParameters() {
    return emptyMap();
  }

  protected Matcher<Iterable<Value>> hasValues(String... values) {
    Set<ValueMatcher> options = stream(values)
        .map(ValueMatcher::valueWithId)
        .collect(toSet());
    return hasValues(options.toArray(new ValueMatcher[] {}));
  }

  protected Matcher<Iterable<Value>> hasValues(ValueMatcher... valuesMatchers) {
    return hasItems(valuesMatchers);
  }

  protected Set<Value> getValuesFromSource(String flowName, String parameterName) throws Exception {
    final var sourceAst = getFlowComponent(flowName, SOURCE);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(sourceAst)
        .map(this::createConfigurationProvider);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                sourceAst.getModel(ParameterizedModel.class).orElseThrow());

    final var sourceParameterization = createComponentParameterizationFromComponentAst(sourceAst);
    ValueResult valueResult =
        valueProviderExecutor.resolveValues(valuesExtension, parameterName, sourceParameterization, configurationProvider, null);

    assertThat(valueResult, isSuccess());
    return valueResult.getValues();
  }

  protected Set<Value> getValuesFromSource(String flowName, String parameterName, String targetSelector) throws Exception {
    final var sourceAst = getFlowComponent(flowName, SOURCE);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(sourceAst)
        .map(this::createConfigurationProvider);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                sourceAst.getModel(ParameterizedModel.class).orElseThrow());

    final var sourceParameterization = createComponentParameterizationFromComponentAst(sourceAst);
    ValueResult valueResult =
        valueProviderExecutor.resolveValues(valuesExtension, parameterName, sourceParameterization, configurationProvider,
                                            targetSelector);

    assertThat(valueResult, isSuccess());
    return valueResult.getValues();
  }

  protected Set<Value> getValues(String flowName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResult(flowName, parameterName));
  }

  protected ValueResult getValueResult(String flowName, String parameterName) throws Exception {
    final var operationAst = getFlowComponent(flowName, OPERATION);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(operationAst)
        .map(this::createConfigurationProvider);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                operationAst.getModel(ParameterizedModel.class).orElseThrow());

    final var operationParameterization = createComponentParameterizationFromComponentAst(operationAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, operationParameterization, configurationProvider,
                                               null);
  }

  protected Set<Value> getValues(String flowName, String parameterName, String targetSelector) throws Exception {
    return checkResultAndRetrieveValues(getValueResult(flowName, parameterName, targetSelector));
  }

  protected ValueResult getValueResult(String flowName, String parameterName, String targetSelector) throws Exception {
    final var operationAst = getFlowComponent(flowName, OPERATION);
    Optional<ConfigurationProvider> configurationProvider = configNameFromComponent(operationAst)
        .map(this::createConfigurationProvider);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                operationAst.getModel(ParameterizedModel.class).orElseThrow());

    final var operationParameterization = createComponentParameterizationFromComponentAst(operationAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, operationParameterization, configurationProvider,
                                               targetSelector);
  }

  protected Set<Value> getValuesFromConfig(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResultFromConfig(configName, parameterName));
  }

  public ValueResult getValueResultFromConfig(String configName, String parameterName) {
    final var configAst = getTopLevelComponent(configName);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                configAst.getModel(ParameterizedModel.class).orElseThrow());

    final var configParameterization = createComponentParameterizationFromComponentAst(configAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, configParameterization, empty(), null);
  }

  protected Set<Value> getFieldValuesFromConfig(String configName, String parameterName, String targetSelector) throws Exception {
    return checkResultAndRetrieveValues(getFieldValuesResultFromConfig(configName, parameterName, targetSelector));
  }

  public ValueResult getFieldValuesResultFromConfig(String configName, String parameterName, String targetSelector) {
    final var configAst = getTopLevelComponent(configName);
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                configAst.getModel(ParameterizedModel.class).orElseThrow());

    final var configParameterization = createComponentParameterizationFromComponentAst(configAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, configParameterization, empty(), targetSelector);
  }

  protected Set<Value> getValuesFromConnection(String configName, String parameterName) throws Exception {
    return checkResultAndRetrieveValues(getValueResultFromConnection(configName, parameterName));
  }

  public ValueResult getValueResultFromConnection(String configName, String parameterName) {
    final var connectionAst = getConnectionProvider(getTopLevelComponent(configName));
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                connectionAst.getModel(ParameterizedModel.class).orElseThrow());

    final var conectionParameterization = createComponentParameterizationFromComponentAst(connectionAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, conectionParameterization, empty(), null);
  }

  protected Set<Value> getFieldValuesFromConnection(String configName, String parameterName, String targetSelector)
      throws Exception {
    return checkResultAndRetrieveValues(getFieldValueResultFromConnection(configName, parameterName, targetSelector));
  }

  public ValueResult getFieldValueResultFromConnection(String configName, String parameterName, String targetSelector) {
    final var connectionAst = getConnectionProvider(getTopLevelComponent(configName));
    final var valueProviderExecutor = new ValueProviderExecutor(extensionDesignTimeResolversFactory,
                                                                connectionAst.getModel(ParameterizedModel.class).orElseThrow());

    final var conectionParameterization = createComponentParameterizationFromComponentAst(connectionAst);
    return valueProviderExecutor.resolveValues(valuesExtension, parameterName, conectionParameterization, empty(),
                                               targetSelector);
  }

  private Set<Value> checkResultAndRetrieveValues(ValueResult values) throws ValueResolvingException {
    assertThat(values, isSuccess());
    return values.getValues();
  }

  private ComponentAst getFlowComponent(String flowName, ComponentType componentType) {
    return appAst.topLevelComponentsStream()
        .filter(f -> componentIdEquals(f, flowName))
        .flatMap(flowAst -> flowAst.directChildrenStream()
            .filter(comp -> componentType.equals(comp.getComponentType())))
        .findFirst().orElseThrow();
  }

  private Optional<String> configNameFromComponent(ComponentAst comp) {
    return comp.getParameters()
        .stream()
        .filter(p -> p.getModel().getAllowedStereotypes()
            .stream()
            .anyMatch(as -> as.isAssignableTo(CONFIG)))
        .map(p -> (String) p.getValue().getRight())
        .findAny();
  }

  private ConfigurationProvider createConfigurationProvider(String configName) {
    final var configAst = getTopLevelComponent(configName);

    final var configParameterization = createComponentParameterizationFromComponentAst(configAst);

    return extensionDesignTimeResolversFactory.createConfigurationProvider(valuesExtension,
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
                                                                               .getClassLoader(valuesExtension));
  }

  private boolean componentIdEquals(ComponentAst component, String componentId) {
    return component.getComponentId()
        .map(componentId::equals)
        .orElse(false);
  }

  private ComponentAst getTopLevelComponent(String configName) {
    return appAst.topLevelComponentsStream()
        .filter(f -> componentIdEquals(f, configName))
        .findFirst().orElseThrow();
  }

  private ComponentAst getConnectionProvider(ComponentAst configAst) {
    return configAst.directChildrenStream()
        .filter(c -> CONNECTION.equals(c.getComponentType()))
        .findAny()
        .orElseThrow();
  }

}
