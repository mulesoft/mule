/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources.test;

import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.loadExtension;

import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.extension.api.resources.GeneratedResource;
import org.mule.runtime.module.extension.internal.resources.documentation.ExtensionDocumentationResourceGenerator;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;

import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ExtensionModelResourceFactoryTestCase {

  private static final String RESOURCE_NAME = "heisenberg-extension-descriptions.xml";
  private ExtensionDocumentationResourceGenerator resourceFactory = new ExtensionDocumentationResourceGenerator();
  private ExtensionModel extensionModel;

  @Before
  public void before() {
    extensionModel = loadExtension(HeisenbergExtension.class);
  }

  @Test
  public void generate() throws Exception {
    GeneratedResource resource = resourceFactory.generateResource(extensionModel).get();
    assertThat(resource.getPath(), equalTo("META-INF/" + RESOURCE_NAME));
    XMLUnit.setIgnoreWhitespace(true);
    String expected =
        IOUtils.toString(currentThread().getContextClassLoader().getResourceAsStream(RESOURCE_NAME), UTF_8);
    XMLUnit.compareXML(expected, new String(resource.getContent()));
  }
}
