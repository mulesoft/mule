/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import java.io.InputStream;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

@SmallTest
public class MulePluginDescriptorGeneratorTestCase extends AbstractGeneratedResourceFactoryTestCase {

  private MulePluginDescriptorGenerator generator = new MulePluginDescriptorGenerator();

  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = loadExtension(HeisenbergExtension.class);
  }

  @Override
  protected Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes() {
    return new Class[] {MulePluginDescriptorGenerator.class};
  }

  @Test
  public void generate() throws Exception {
    InputStream in = getClass().getResourceAsStream("/heisenberg-test-mule-artifact.json");
    assertThat(in, is(notNullValue()));
    String expectedDescriptor = IOUtils.toString(in);
    Optional<GeneratedResource> resource = generator.generateResource(extensionModel);
    assertThat(resource.isPresent(), is(true));

    String actualDescriptor = new String(resource.get().getContent());
    JSONAssert.assertEquals(expectedDescriptor, actualDescriptor, true);
  }
}
