/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.meta.model.ExecutionType.BLOCKING;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_INTENSIVE;
import static org.mule.runtime.api.meta.model.ExecutionType.CPU_LITE;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.message.NullAttributes;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.meta.model.util.IdempotentExtensionWalker;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.core.util.ValueHolder;
import org.mule.runtime.extension.api.declaration.DescribingContext;
import org.mule.runtime.extension.api.declaration.spi.Describer;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.runtime.ExtensionFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Test;

@SmallTest
public class DefaultExtensionFactoryTestCase extends AbstractMuleTestCase {

  private final ExtensionFactory extensionFactory =
      new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());

  private final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();

  private ExtensionModel createExtension(Class<?> annotatedClass) {
    Describer describer = new AnnotationsBasedDescriber(annotatedClass, new StaticVersionResolver(getProductVersion()));
    final DescribingContext context = new DefaultDescribingContext(getClass().getClassLoader());
    return extensionFactory.createFrom(describer.describe(context), context);
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
    ExtensionModel extensionModel = createExtension(HeisenbergExtension.class);

    ValueHolder<Boolean> cpuIntensive = new ValueHolder<>(false);
    ValueHolder<Boolean> blocking = new ValueHolder<>(false);
    new IdempotentExtensionWalker() {

      @Override
      protected void onOperation(OperationModel operation) {
        assertThat(operation.isBlocking(), is(true));
        String operationName = operation.getName();

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
    OperationModel operation = extensionModel.getConfigurationModel("iron-man").get().getOperationModel("fireMissile").get();
    assertThat(operation.isBlocking(), is(false));
    assertThat(operation.getExecutionType(), is(CPU_LITE));
    assertThat(operation.getOutput().getType(), equalTo(typeLoader.load(String.class)));
    assertThat(operation.getOutputAttributes().getType(), equalTo(typeLoader.load(NullAttributes.class)));
  }

  private <T> T aggressiveGet(Optional<T> optional) {
    return optional.orElseThrow(NoSuchElementException::new);
  }
}
