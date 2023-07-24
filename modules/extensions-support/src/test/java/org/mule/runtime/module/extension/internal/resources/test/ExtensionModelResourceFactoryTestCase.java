/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.resources.test;

import static java.lang.Thread.currentThread;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.extension.api.resources.spi.GeneratedResourceFactory;
import org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExtensionModelResourceFactoryTestCase extends AbstractGeneratedResourceFactoryTestCase {

  private static final String RESOURCE_NAME = "heisenberg-extension-descriptions.xml";
  private ExtensionDocumentationResourceGenerator resourceFactory = new ExtensionDocumentationResourceGenerator();
  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = loadExtension(HeisenbergExtension.class);
  }

  @Override
  protected Class<? extends GeneratedResourceFactory>[] getResourceFactoryTypes() {
    return new Class[] {ExtensionDocumentationResourceGenerator.class};
  }

  @Test
  public void generate() throws Exception {
    GeneratedResource resource = resourceFactory.generateResource(extensionModel).get();
    assertThat(resource.getPath(), equalTo(RESOURCE_NAME));
    XMLUnit.setIgnoreWhitespace(true);
    String expected = IOUtils.toString(currentThread().getContextClassLoader().getResource(RESOURCE_NAME).openStream());
    XMLUnit.compareXML(expected, new String(resource.getContent()));
  }
}
