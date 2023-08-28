/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client;

import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.runtime.client.DefaultExtensionsClientTestExtension.DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

import org.mule.AbstractBenchmark;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.manager.DefaultExtensionManagerFactory;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

@Warmup(iterations = 0)
@Measurement(iterations = 1)
@OutputTimeUnit(NANOSECONDS)
public class DefaultExtensionsClientBenchmark extends AbstractBenchmark {

  private MuleContext muleContext;
  private DefaultExtensionsClient extensionsClient;

  @Setup
  public void setup() throws MuleException {
    muleContext = createMuleContextWithServices();
    muleContext.start();

    extensionsClient = new DefaultExtensionsClient();
    muleContext.getInjector().inject(extensionsClient);
    extensionsClient.initialise();
  }

  @TearDown
  public void tearDown() {
    extensionsClient.dispose();
    muleContext.dispose();
  }

  @Benchmark
  public Result<?, ?> basicNoOp() {
    return execute("basicNoOp");
  }

  @Benchmark
  public Result<?, ?> multipleParametersNoOp() {
    return execute("multipleParametersNoOp", parameterizer -> {
      parameterizer.withParameter("stringParameter", "Hey");
      parameterizer.withParameter("intParameter", 9);
      parameterizer.withParameter("strings", asList("a", "b", "c"));
    });
  }

  @Benchmark
  public Result<?, ?> simplePayloadOutput() {
    return execute("simplePayloadOutput");
  }

  @Benchmark
  public Result<?, ?> simpleResultOutput() {
    return execute("simpleResultOutput");
  }

  @Benchmark
  public Result<?, ?> simpleIdentity() {
    return execute("simpleIdentity", parameterizer -> {
      parameterizer.withParameter("content", new ByteArrayInputStream("Some payload".getBytes()));
    });
  }

  @Override
  protected List<ConfigurationBuilder> getAdditionalConfigurationBuilders() {
    return singletonList(new ExtensionManagerConfigurationBuilder());
  }

  private Result<?, ?> execute(String operationName) {
    return execute(operationName, operationParameterizer -> {
    });
  }

  private Result<?, ?> execute(String operationName, Consumer<OperationParameterizer> parameterizerConsumer) {
    try {
      return extensionsClient.execute(DEFAULT_EXTENSIONS_CLIENT_TEST_EXTENSION_NAME, operationName, parameterizerConsumer).get();
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  private static class ExtensionManagerConfigurationBuilder extends AbstractConfigurationBuilder {

    @Override
    protected void doConfigure(MuleContext muleContext) throws RegistrationException {
      ExtensionManager extensionManager = new DefaultExtensionManagerFactory().create(muleContext);
      extensionManager.registerExtension(loadTestExtensionModel());

      MuleRegistry registry = ((MuleContextWithRegistry) muleContext).getRegistry();
      registry.registerObject(OBJECT_EXTENSION_MANAGER, extensionManager);
    }
  }

  private static ExtensionModel loadTestExtensionModel() {
    ExtensionModelLoadingRequest loadingRequest = builder(currentThread().getContextClassLoader(), getDefault(emptySet()))
        .addParameter(TYPE_PROPERTY_NAME, DefaultExtensionsClientTestExtension.class.getName())
        .addParameter(VERSION, "1.0.0-SNAPSHOT")
        .build();
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(loadingRequest);
  }
}
