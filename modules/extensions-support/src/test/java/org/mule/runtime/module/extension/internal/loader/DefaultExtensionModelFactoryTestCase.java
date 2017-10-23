/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.meta.ExpressionSupport.NOT_SUPPORTED;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.operation.ExecutionType.CPU_LITE;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.PRIMARY_CONTENT;
import static org.mule.runtime.extension.api.ExtensionConstants.STREAMING_STRATEGY_PARAMETER_NAME;
import static org.mule.runtime.extension.api.annotation.param.Optional.PAYLOAD;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.PROCESSOR_DEFINITION;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.VALIDATOR_DEFINITION;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_CLASS_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_DESCRIPTION;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_FILE_NAME;
import static org.mule.test.heisenberg.extension.HeisenbergExtension.HEISENBERG_LIB_NAME;
import static org.mule.test.marvel.ironman.IronMan.CONFIG_NAME;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.HasExternalLibraries;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.declaration.type.StreamingStrategyTypeBuilder;
import org.mule.runtime.extension.api.util.ExtensionModelUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.stereotypes.EmpireStereotype;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;

@SmallTest
public class DefaultExtensionModelFactoryTestCase extends AbstractMuleTestCase {

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private ExtensionModel createExtension(Class<?> annotatedClass) {
    return loadExtension(annotatedClass);
  }

  @Test
  public void flyweight() {
    ExtensionModel extensionModel = createExtension(VeganExtension.class);
    final ConfigurationModel appleConfiguration = aggressiveGet(extensionModel.getConfigurationModel(APPLE));
    final ConfigurationModel bananaConfiguration = aggressiveGet(extensionModel.getConfigurationModel(BANANA));

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
                                                             "exceptionOnCallbacks", "neverFailsWrapper", "payloadModifier");

    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    Reference<Boolean> cpuIntensive = new Reference<>(false);
    Reference<Boolean> blocking = new Reference<>(false);
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operation) {
        String operationName = operation.getName();

        assertThat(operation.isBlocking(), is(!nonBlockingOperations.contains(operationName)));

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
    }.walk(extensionModel);

    assertThat(cpuIntensive.get(), is(true));
    assertThat(blocking.get(), is(true));
  }

  @Test
  public void nonBlockingExecutionType() {
    ExtensionModel extensionModel = createExtension(MarvelExtension.class);
    OperationModel operation =
        extensionModel.getConfigurationModel(CONFIG_NAME).get().getOperationModel("fireMissile").get();
    assertThat(operation.isBlocking(), is(false));
    assertThat(operation.getExecutionType(), is(CPU_LITE));
    assertThat(operation.getOutput().getType(), instanceOf(StringType.class));
    assertThat(operation.getOutputAttributes().getType(), equalTo(typeLoader.load(void.class)));
  }

  @Test
  public void contentParameter() {
    assertSinglePrimaryContentParameter(createExtension(VeganExtension.class), "getAllApples", PAYLOAD);
  }

  @Test
  public void contentParameterWithCustomDefault() {
    assertSinglePrimaryContentParameter(createExtension(VeganExtension.class), "tryToEatThisListOfMaps",
                                        null);
  }

  @Test
  public void exportedLibraries() {
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    assertExternalLibraries(extensionModel);
    new IdempotentExtensionWalker() {

      @Override
      protected void onConfiguration(ConfigurationModel model) {
        assertExternalLibraries(model);
      }

      @Override
      protected void onConnectionProvider(ConnectionProviderModel model) {
        assertExternalLibraries(model);
      }
    }.walk(extensionModel);
  }

  @Test
  public void streamingHintOnOperation() throws Exception {
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    OperationModel operationModel = extensionModel.getConfigurationModels().get(0).getOperationModel("sayMyName").get();
    ParameterModel streamingParameter = operationModel.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME))
        .findFirst()
        .get();

    assertStreamingStrategy(streamingParameter);
  }

  @Test
  public void streamingHintOnSource() throws Exception {
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    SourceModel sourceModel = extensionModel.getConfigurationModels().get(0).getSourceModel("ListenPayments").get();
    ParameterModel streamingParameter = sourceModel.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(STREAMING_STRATEGY_PARAMETER_NAME))
        .findFirst()
        .get();

    assertStreamingStrategy(streamingParameter);
  }

  @Test
  public void customStereotype() {
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    OperationModel operation = extensionModel.getConfigurationModels().get(0).getOperationModel("callSaul").get();

    StereotypeModel stereotypeModel = operation.getStereotype();
    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));

    assertThat(stereotypeModel.getType(), is(new EmpireStereotype().getName().toUpperCase()));
    assertThat(stereotypeModel.getNamespace(), is(HEISENBERG.toUpperCase()));
    assertThat(stereotypeModel.getParent().get(), is(PROCESSOR));
  }

  @Test
  public void validatorStereotype() {
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);
    OperationModel operation = extensionModel.getOperationModel("validateMoney").get();

    StereotypeModel stereotypeModel = operation.getStereotype();
    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));
    assertThat(stereotypeModel.isAssignableTo(VALIDATOR), is(true));

    assertThat(stereotypeModel.getType(), is(VALIDATOR_DEFINITION.getName()));
    assertThat(stereotypeModel.getNamespace(), is(HEISENBERG.toUpperCase()));
    assertThat(stereotypeModel.getParent().get(), is(VALIDATOR));
  }

  @Test
  public void defaultStereotype() {
    ExtensionModel extensionModel = createExtension(VeganExtension.class);
    OperationModel operation = extensionModel.getConfigurationModel(APPLE).get().getOperationModel("eatApple").get();

    StereotypeModel stereotypeModel = operation.getStereotype();
    assertThat(stereotypeModel.isAssignableTo(PROCESSOR), is(true));

    assertThat(stereotypeModel.getType(), is(PROCESSOR_DEFINITION.getName()));
    assertThat(stereotypeModel.getNamespace(), is("MULE"));
    assertThat(stereotypeModel.getParent().isPresent(), is(false));
  }

  @Test
  public void untesteableConnetionProvider() throws Exception {
    ExtensionModel extensionModel = createExtension(VeganExtension.class);
    ConnectionProviderModel connectionProviderModel = extensionModel.getConfigurationModel(APPLE)
        .map(c -> c.getConnectionProviders().get(0))
        .get();

    assertThat(connectionProviderModel.supportsConnectivityTesting(), is(false));
  }

  @Test
  public void testeableConnectionProvider() throws Exception {
    ExtensionModel extensionModel = createExtension(VeganExtension.class);
    ConnectionProviderModel connectionProviderModel = extensionModel.getConfigurationModel(BANANA)
        .map(c -> c.getConnectionProviders().get(0))
        .get();

    assertThat(connectionProviderModel.supportsConnectivityTesting(), is(true));
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
}
