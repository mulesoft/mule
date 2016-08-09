/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.manifest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.extension.api.introspection.ExtensionFactory;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.declaration.DescribingContext;
import org.mule.runtime.extension.api.introspection.declaration.spi.Describer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.DefaultExtensionFactory;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.runtime.module.extension.internal.resources.AbstractGeneratedResourceFactoryTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExtensionManifestGeneratorTestCase extends AbstractGeneratedResourceFactoryTestCase {

  private ExtensionManifestGenerator generator = new ExtensionManifestGenerator();

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    Describer describer =
        new AnnotationsBasedDescriber(HeisenbergExtension.class, new StaticVersionResolver(getProductVersion()));
    ExtensionFactory extensionFactory = new DefaultExtensionFactory(new SpiServiceRegistry(), getClass().getClassLoader());
    final DescribingContext context = new DefaultDescribingContext(getClass().getClassLoader());

    extensionModel = extensionFactory.createFrom(describer.describe(context), context);
  }

  @Override
  protected Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes() {
    return new Class[] {ExtensionManifestGenerator.class};
  }

  @Test
  public void generate() throws Exception {
    InputStream in = getClass().getResourceAsStream("/heisenberg-test-manifest.xml");
    assertThat(in, is(notNullValue()));
    String expectedSchema = IOUtils.toString(in);
    Optional<GeneratedResource> resource = generator.generateResource(extensionModel);
    assertThat(resource.isPresent(), is(true));

    compareXML(expectedSchema, new String(resource.get().getContent()));
  }
}
