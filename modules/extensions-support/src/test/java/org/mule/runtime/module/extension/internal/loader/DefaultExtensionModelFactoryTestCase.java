/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static org.mule.metadata.api.utils.MetadataTypeUtils.getTypeId;
import static org.mule.runtime.api.meta.Category.SELECT;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.extension.api.ExtensionConstants.BACK_PRESSURE_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.DROP;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.FAIL;
import static org.mule.runtime.extension.api.runtime.source.BackPressureMode.WAIT;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_17;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_21;
import static org.mule.sdk.api.meta.JavaVersion.JAVA_8;
import static org.mule.sdk.api.stereotype.MuleStereotypes.OBJECT_STORE;
import static org.mule.test.allure.AllureConstants.Sdk.SDK;
import static org.mule.test.allure.AllureConstants.Sdk.SupportedJavaVersions.JAVA_VERSIONS_IN_EXTENSION_MODEL;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_FILE_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_NAME;
import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import static org.mule.test.vegan.extension.VeganExtension.KIWI;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.StringContains.containsString;

import static org.junit.Assert.assertThrows;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.annotation.EnumAnnotation;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.HasExternalLibraries;
import org.mule.runtime.api.meta.model.ImportedTypeModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Export;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.OnException;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.Streaming;
import org.mule.runtime.extension.api.annotation.SubTypeMapping;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.source.BackPressure;
import org.mule.runtime.extension.api.annotation.source.EmitsResponse;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.sdk.api.annotation.JavaVersionSupport;
import org.mule.sdk.api.annotation.error.ErrorTypes;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.HeisenbergErrors;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.HeisenbergOperations;
import org.mule.test.heisenberg.extension.HeisenbergRouters;
import org.mule.test.heisenberg.extension.HeisenbergScopes;
import org.mule.test.heisenberg.extension.HeisenbergSource;
import org.mule.test.heisenberg.extension.KillingOperations;
import org.mule.test.heisenberg.extension.MoneyLaunderingOperation;
import org.mule.test.heisenberg.extension.SecureHeisenbergConnectionProvider;
import org.mule.test.heisenberg.extension.exception.HeisenbergConnectionExceptionEnricher;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.CarDealer;
import org.mule.test.heisenberg.extension.model.CarWash;
import org.mule.test.heisenberg.extension.model.Investment;
import org.mule.test.heisenberg.extension.model.Ricin;
import org.mule.test.heisenberg.extension.model.Weapon;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.sdk.java.versions.NewJavaVersionsExtension;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.hamcrest.Matcher;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@SmallTest
@Feature(SDK)
public class DefaultExtensionModelFactoryTestCase extends AbstractMuleTestCase {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  private final ExtensionModel heisenbergExtension = loadExtension(HeisenbergExtension.class);
  private final ExtensionModel veganExtension = loadExtension(VeganExtension.class);

  @Test
  public void flyweight() {
    final ConfigurationModel appleConfiguration = aggressiveGet(veganExtension.getConfigurationModel(APPLE));
    final ConfigurationModel bananaConfiguration = aggressiveGet(veganExtension.getConfigurationModel(BANANA));

    final String sourceName = PaulMcCartneySource.class.getSimpleName();
    SourceModel appleSource = aggressiveGet(appleConfiguration.getSourceModel(sourceName));
    SourceModel bananaSource = aggressiveGet(bananaConfiguration.getSourceModel(sourceName));

    assertThat(appleSource, is(sameInstance(appleSource)));
    assertThat(bananaSource, is(sameInstance(bananaSource)));

    final String operationName = "spreadTheWord";
    OperationModel appleOperation = aggressiveGet(appleConfiguration.getOperationModel(operationName));
    OperationModel bananaOperation = aggressiveGet(bananaConfiguration.getOperationModel(operationName));

    assertThat(appleOperation, is(sameInstance(bananaOperation)));
  }

  @Test
  public void blockingExecutionTypes() {
    final List<String> nonBlockingOperations = Arrays.asList("killMany", "executeAnything", "alwaysFailsWrapper", "getChain",
                                                             "exceptionOnCallbacks", "neverFailsWrapper", "payloadModifier",
                                                             "blockingNonBlocking", "nonBlocking", "callGusFringNonBlocking",
                                                             "tapPhones", "sdkExecuteForeingOrders", "concurrentRouteExecutor",
                                                             "simpleRouter", "spy", "stereotypedRoutes", "twoRoutesRouter",
                                                             "sdkVoidRouter", "voidRouter");

    Reference<Boolean> cpuIntensive = new Reference<>(false);
    Reference<Boolean> blocking = new Reference<>(false);
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operation) {
        String operationName = operation.getName();

        Matcher<Iterable<? super String>> nonBlockingMatcher = hasItem(operationName);
        assertThat(nonBlockingOperations, operation.isBlocking() ? not(nonBlockingMatcher) : nonBlockingMatcher);

        if (operationName.equals("approve")) {
          assertThat(operation.getExecutionType(), is(CPU_INTENSIVE));
          cpuIntensive.set(true);
        } else if (operation.requiresConnection()) {
          assertThat(operation.getExecutionType(), is(BLOCKING));
          blocking.set(true);
        } else {
          assertThat(operation.getExecutionType(), is(CPU_LITE));
        }
      }
    }.walk(heisenbergExtension);

    assertThat(cpuIntensive.get(), is(true));
    assertThat(blocking.get(), is(true));
  }

  @Test
  public void nonBlockingExecutionType() {
    ExtensionModel extensionModel = loadExtension(MarvelExtension.class);
    OperationModel operation =
        extensionModel.getConfigurationModel(CONFIG_NAME).get().getOperationModel("fireMissile").get();
    assertThat(operation.isBlocking(), is(false));
    assertThat(operation.getExecutionType(), is(CPU_LITE));
    assertThat(operation.getOutput().getType(), instanceOf(StringType.class));
    assertThat(operation.getOutputAttributes().getType(), equalTo(typeLoader.load(void.class)));
  }

  @Test
  public void contentParameter() {
    assertSinglePrimaryContentParameter(veganExtension, "getAllApples", PAYLOAD);
  }

  @Test
  public void contentParameterWithCustomDefault() {
    assertSinglePrimaryContentParameter(veganExtension, "tryToEatThisListOfMaps",
                                        null);
  }

  @Test
  public void exportedLibraries() {
    assertExternalLibraries(heisenbergExtension);
    new IdempotentExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        assertExternalLibraries(model);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        assertExternalLibraries(model);
      }
    }.walk(heisenbergExtension);
  }

  @Test
  public void streamingHintOnOperation() throws Exception {
    OperationModel operationModel = heisenbergExtension.getConfigurationModels().get(0).getOperationModel("sayMyName").get();
    ParameterModel streamingParameter = operationModel.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME))
        .findFirst()
        .get();

    assertStreamingStrategy(streamingParameter);
  }

  @Test
  public void streamingHintOnSource() throws Exception {
    SourceModel sourceModel = heisenbergExtension.getConfigurationModels().get(0).getSourceModel("ListenPayments").get();
    ParameterModel streamingParameter = sourceModel.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME))
        .findFirst()
        .get();

    assertStreamingStrategy(streamingParameter);
  }

  @Test
  public void untestableConnectionProvider() throws Exception {
    ConnectionProviderModel connectionProviderModel = veganExtension.getConfigurationModel(APPLE)
        .map(c -> c.getConnectionProviders().get(0))
        .get();

    assertThat(connectionProviderModel.supportsConnectivityTesting(), is(false));
  }

  @Test
  public void untestableSdkConnectionProvider() throws Exception {
    ConnectionProviderModel connectionProviderModel = veganExtension.getConfigurationModel(KIWI)
        .map(c -> c.getConnectionProviders().get(0))
        .get();

    assertThat(connectionProviderModel.supportsConnectivityTesting(), is(false));
  }

  @Test
  public void testableConnectionProvider() throws Exception {
    ConnectionProviderModel connectionProviderModel = veganExtension.getConfigurationModel(BANANA)
        .map(c -> c.getConnectionProviders().get(0))
        .get();

    assertThat(connectionProviderModel.supportsConnectivityTesting(), is(true));
  }

  @Test
  public void sourceWithReducedBackPressureStrategies() {
    SourceModel source = heisenbergExtension.getConfigurationModels().get(0).getSourceModel("ListenPayments").get();

    ParameterModel parameter = source.getAllParameterModels().stream()
        .filter(p -> BACK_PRESSURE_STRATEGY_PARAMETER_NAME.equals(p.getName()))
        .findAny().orElseThrow(() -> new IllegalStateException("No backPressureStrategy parameter"));

    assertThat(parameter.getType(), is(instanceOf(StringType.class)));
    assertThat(parameter.getDefaultValue(), is(FAIL));

    EnumAnnotation enumAnnotation = parameter.getType().getAnnotation(EnumAnnotation.class)
        .orElseThrow(() -> new IllegalStateException("No enum annotation"));

    assertThat(asList(enumAnnotation.getValues()), containsInAnyOrder(FAIL.name(), DROP.name()));
  }

  @Test
  public void sourceWithFixedBackPressureStrategy() {
    SourceModel source = heisenbergExtension.getSourceModels().get(0);

    Optional<ParameterModel> parameter = source.getAllParameterModels().stream()
        .filter(p -> BACK_PRESSURE_STRATEGY_PARAMETER_NAME.equals(p.getName()))
        .findAny();

    assertThat(parameter.isPresent(), is(false));
  }

  @Test
  public void sourceWithInheritedBackPressureStrategies() {
    SourceModel source = heisenbergExtension.getConfigurationModels().get(0).getSourceModel("ReconnectableListenPayments").get();

    BackPressureStrategyModelProperty backPressureStrategyModelProperty =
        source.getModelProperty(BackPressureStrategyModelProperty.class).get();

    assertThat(backPressureStrategyModelProperty.getDefaultMode(), is(FAIL));
    assertThat(backPressureStrategyModelProperty.getSupportedModes(), hasItems(FAIL, DROP));
  }

  @Test
  public void sourceWithInvalidDefaultBackPressureStrategies() {
    final var thrown =
        assertThrows(IllegalModelDefinitionException.class, () -> loadExtension(BadBackPressureHeisenbergExtension.class));

    assertThat(thrown.getMessage(),
               containsString("backPressureStrategy parameter has a default value which is not listed as an available option"));
  }

  @Test
  public void sourceWithInvalidBackPressureStrategies() {
    assertThrows(IllegalModelDefinitionException.class, () -> loadExtension(IllegalBackPressureHeisenbergExtension.class));
  }

  @Test
  public void objectStoreParameters() {
    OperationModel operationModel = heisenbergExtension.getOperationModel("storeMoney").get();
    ParameterModel parameter =
        operationModel.getAllParameterModels().stream().filter(p -> "objectStore".equals(p.getName())).findFirst().get();

    StereotypeModel stereotype = parameter.getAllowedStereotypes().stream()
        .filter(s -> s.getType().equals(OBJECT_STORE.getType()))
        .findFirst()
        .get();

    assertThat(stereotype.getNamespace(), equalTo(OBJECT_STORE.getNamespace()));

    Optional<ImportedTypeModel> typeImport = heisenbergExtension.getImportedTypes().stream()
        .filter(i -> getTypeId(i.getImportedType()).map(id -> ObjectStore.class.getName().equals(id)).orElse(false))
        .findFirst();

    assertThat(typeImport.isPresent(), is(true));
  }

  @Test
  @Story(JAVA_VERSIONS_IN_EXTENSION_MODEL)
  public void defaultJavaVersionSupport() {
    assertThat(heisenbergExtension.getSupportedJavaVersions(), equalTo(Set.of(JAVA_21.version(), JAVA_17.version())));
  }

  @Test
  @Story(JAVA_VERSIONS_IN_EXTENSION_MODEL)
  public void customJavaVersionSupport() {
    ExtensionModel model = loadExtension(TestJavaSupportExtension.class);
    assertThat(model.getSupportedJavaVersions(), contains(JAVA_8.version(), JAVA_17.version()));
  }

  @Test
  @Story(JAVA_VERSIONS_IN_EXTENSION_MODEL)
  @Issue("W-18586120")
  public void customJavaVersionSupportWithJavaVersionMismatch() {
    ExtensionModel model = loadExtension(NewJavaVersionsExtension.class);
    assertThat(model.getSupportedJavaVersions(), contains(JAVA_17.version(), JAVA_21.version()));
  }

  private void assertStreamingStrategy(ParameterModel streamingParameter) {
    assertThat(streamingParameter.getType(), equalTo(new StreamingStrategyTypeBuilder().getByteStreamingStrategyType()));
    assertThat(streamingParameter.isRequired(), is(false));
    assertThat(streamingParameter.getDefaultValue(), is(nullValue()));
    assertThat(streamingParameter.getExpressionSupport(), is(NOT_SUPPORTED));
  }

  private void assertExternalLibraries(HasExternalLibraries model) {
    assertThat(model.getExternalLibraryModels(), is(notNullValue()));
    assertThat(model.getExternalLibraryModels(), hasSize(1));
    ExternalLibraryModel library = model.getExternalLibraryModels().iterator().next();

    assertThat(library.getName(), is(HEISENBERG_LIB_NAME));
    assertThat(library.getDescription(), is(HEISENBERG_LIB_DESCRIPTION));
    assertThat(library.getRegexMatcher().get(), is(HEISENBERG_LIB_FILE_NAME));
    assertThat(library.getRequiredClassName().get(), is(HEISENBERG_LIB_CLASS_NAME));
  }

  private void assertSinglePrimaryContentParameter(ExtensionModel extensionModel, String operationName, String defaultValue) {
    OperationModel appleOperation = aggressiveGet(extensionModel.getOperationModel(operationName));
    List<ParameterModel> contentParameters = appleOperation.getAllParameterModels().stream()
        .filter(ExtensionModelUtils::isContent)
        .collect(toList());

    assertThat(contentParameters, hasSize(1));
    ParameterModel contentParameter = contentParameters.get(0);
    assertThat(contentParameter.isRequired(), is(false));
    assertThat(contentParameter.getDefaultValue(), is(defaultValue));
    assertThat(contentParameter.getRole(), is(PRIMARY_CONTENT));
  }

  private <T> T aggressiveGet(Optional<T> optional) {
    return optional.orElseThrow(NoSuchElementException::new);
  }

  @Extension(name = HeisenbergExtension.HEISENBERG, category = SELECT)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class,
      KillingOperations.class, HeisenbergScopes.class, HeisenbergRouters.class})
  @OnException(HeisenbergConnectionExceptionEnricher.class)
  @ConnectionProviders({HeisenbergConnectionProvider.class, SecureHeisenbergConnectionProvider.class})
  @Sources(BadBackPressureSource.class)
  @Export(classes = {HeisenbergExtension.class, HeisenbergException.class}, resources = "methRecipe.json")
  @SubTypeMapping(baseType = Weapon.class, subTypes = {Ricin.class})
  @SubTypeMapping(baseType = Investment.class, subTypes = {CarWash.class, CarDealer.class})
  @ErrorTypes(HeisenbergErrors.class)
  public static class BadBackPressureHeisenbergExtension extends HeisenbergExtension {

  }

  @Alias("ListenPayments")
  @EmitsResponse
  @Streaming
  @MediaType(TEXT_PLAIN)
  @BackPressure(defaultMode = FAIL, supportedModes = {DROP, WAIT})
  public static class BadBackPressureSource extends HeisenbergSource {

  }

  @Extension(name = HeisenbergExtension.HEISENBERG, category = SELECT)
  @Operations({HeisenbergOperations.class, MoneyLaunderingOperation.class,
      KillingOperations.class, HeisenbergScopes.class, HeisenbergRouters.class})
  @OnException(HeisenbergConnectionExceptionEnricher.class)
  @ConnectionProviders({HeisenbergConnectionProvider.class, SecureHeisenbergConnectionProvider.class})
  @Sources(IllegalBackPressureSource.class)
  @Export(classes = {HeisenbergExtension.class, HeisenbergException.class}, resources = "methRecipe.json")
  @SubTypeMapping(baseType = Weapon.class, subTypes = {Ricin.class})
  @SubTypeMapping(baseType = Investment.class, subTypes = {CarWash.class, CarDealer.class})
  @org.mule.sdk.api.annotation.error.ErrorTypes(HeisenbergErrors.class)
  public static class IllegalBackPressureHeisenbergExtension extends HeisenbergExtension {

  }

  @Alias("ListenPayments")
  @EmitsResponse
  @Streaming
  @MediaType(TEXT_PLAIN)
  @BackPressure(defaultMode = FAIL, supportedModes = {DROP, WAIT})
  public static class IllegalBackPressureSource extends HeisenbergSource {

  }

  @Extension(name = "DefaultJavaSupport")
  @JavaVersionSupport({JAVA_8, JAVA_17})
  public static class TestJavaSupportExtension {

  }
}
