/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.test.vegan.extension.VeganExtension.APPLE;
import static org.mule.test.vegan.extension.VeganExtension.BANANA;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.source.SourceModel;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.vegan.extension.PaulMcCartneySource;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class DefaultExtensionFactoryTestCase extends AbstractMuleTestCase {

  private final ExtensionFactory extensionFactory =
      new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
  private ExtensionModel extensionModel;

  @Before
  public void before() {
    Describer describer = new AnnotationsBasedDescriber(VeganExtension.class, new StaticVersionResolver(getProductVersion()));
    final DescribingContext context = new DefaultDescribingContext(getClass().getClassLoader());
    extensionModel = extensionFactory.createFrom(describer.describe(context), context);
  }

  @Test
  public void flyweight() {
    final ConfigurationModel appleConfiguration = aggresiveGet(extensionModel.getConfigurationModel(APPLE));
    final ConfigurationModel bananaConfiguration = aggresiveGet(extensionModel.getConfigurationModel(BANANA));

    final String sourceName = PaulMcCartneySource.class.getSimpleName();
    SourceModel appleSource = aggresiveGet(appleConfiguration.getSourceModel(sourceName));
    SourceModel bananaSource = aggresiveGet(bananaConfiguration.getSourceModel(sourceName));

    assertThat(appleSource, is(sameInstance(appleSource)));
    assertThat(bananaSource, is(sameInstance(bananaSource)));

    final String operationName = "spreadTheWord";
    OperationModel appleOperation = aggresiveGet(appleConfiguration.getOperationModel(operationName));
    OperationModel bananaOperation = aggresiveGet(bananaConfiguration.getOperationModel(operationName));

    assertThat(appleOperation, is(sameInstance(bananaOperation)));
  }

  private <T> T aggresiveGet(Optional<T> optional) {
    return optional.orElseThrow(() -> new NoSuchElementException());
  }
}
