/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.EXTENSION_MODEL_JSON_FILE_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.loadExtension;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExtensionModelResourceFactoryTestCase extends AbstractGeneratedResourceFactoryTestCase {

  private ExtensionModelResourceFactory resourceFactory = new ExtensionModelResourceFactory();
  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = loadExtension(HeisenbergExtension.class);
  }


  @Override
  protected Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes() {
    return new Class[] {ExtensionModelResourceFactory.class};
  }

  @Test
  public void generate() throws Exception {
    GeneratedResource resource = resourceFactory.generateResource(extensionModel).get();
    assertThat(resource.getPath(), equalTo(EXTENSION_MODEL_JSON_FILE_NAME));

    ExtensionModel deserialized = new ExtensionModelJsonSerializer().deserialize(new String(resource.getContent()));
    assertThat(extensionModel.getName(), equalTo(deserialized.getName()));
  }
}
