/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;


import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests if the current module contains global elements poorly defined in which the current smart connector cannot determine to
 * which one should delegate the test connection feature.
 *
 * @since 4.0
 */
@SmallTest
public class TestConnectionFailuresTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Before
  public void setUp() {
    exception.expect(MuleRuntimeException.class);
  }

  @Test
  public void multipleGlobalElementsWithXmlnsConnectionAttribute() {
    setExpectedMessage("Can't resolve http://www.mulesoft.org/schema/mule/http/current/mule-http.xsd");
    getExtensionModelFrom("validation/testconnection/module-multiple-global-element-xmlns-connection-true.xml");
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefined() {
    setExpectedMessage("first-config-not-defined-to-which-one-do-test-connection",
                       "second-config-not-defined-to-which-one-do-test-connection");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection.xml",
                          new HashSet<>(Arrays.asList(getHttpExtension())));
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefinedHttpAndFile() {
    setExpectedMessage("file-global-element", "http-global-element");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection-http-file.xml",
                          new HashSet<>(Arrays.asList(getHttpExtension(), getFileExtension())));
  }

  @Test
  public void repeatedPropertiesConfigurationConnection() {
    setExpectedMessage("repeated properties are: [someUserConfig, somePassConfig]");
    getExtensionModelFrom("validation/testconnection/module-repeated-properties-configuration-connection.xml");
  }

  @Test
  public void multipleConnectionProperties() {
    setExpectedMessage("There cannot be more than 1 child [connection] element per [module], found [2]");
    getExtensionModelFrom("validation/testconnection/module-multiple-connection.xml");
  }

  private ExtensionModel getFileExtension() {
    return mockedExtension("file", "config", "connection");
  }

  private ExtensionModel getHttpExtension() {
    return mockedExtension("http", "request-config", "request-connection");
  }

  private void setExpectedMessage(String... conflictingGlobalElements) {
    exception.expectMessage(Arrays.stream(conflictingGlobalElements).collect(Collectors.joining(", ")));
  }

  private ExtensionModel mockedExtension(final String name, final String config, final String connectionProvider) {
    final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    extensionDeclarer.named(name)
        .onVersion("4.0.0")
        .fromVendor("MuleSoft testcase")
        .withCategory(Category.COMMUNITY)
        .withConfig(config)
        .withConnectionProvider(connectionProvider)
        .withConnectionManagementType(ConnectionManagementType.NONE);

    return new ExtensionModelFactory()
        .create(new DefaultExtensionLoadingContext(extensionDeclarer, currentThread().getContextClassLoader(),
                                                   new NullDslResolvingContext()));
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    return getExtensionModelFrom(modulePath, emptySet());
  }

  private ExtensionModel getExtensionModelFrom(String modulePath, Set<ExtensionModel> extensions) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(extensions), parameters);
  }
}
