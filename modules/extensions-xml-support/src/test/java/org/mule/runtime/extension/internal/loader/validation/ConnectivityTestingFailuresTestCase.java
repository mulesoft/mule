/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.validation;


import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Collections.emptySet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.XmlExtensionLoaderDelegate.MODULE_CONNECTION_MARKER_ATTRIBUTE;
import static org.mule.runtime.extension.internal.loader.validator.TestConnectionValidator.TEST_CONNECTION_SELECTED_ELEMENT_INVALID;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.api.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.internal.loader.ExtensionModelFactory;
import org.mule.runtime.extension.internal.loader.validator.TestConnectionValidator;
import org.mule.runtime.internal.dsl.NullDslResolvingContext;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
public class ConnectivityTestingFailuresTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void multipleGlobalElementsWithXmlnsConnectionAttribute() {
    setExpectedMessage("first-config", "second-config");
    getExtensionModelFrom("validation/testconnection/module-multiple-global-element-xmlns-connection-true.xml");
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefined() {
    setExpectedMessage("first-config-not-defined-to-which-one-do-test-connection",
                       "second-config-not-defined-to-which-one-do-test-connection");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection.xml",
                          new HashSet<>(Arrays.asList(getHttpExtension(true))));
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefinedHttpAndFile() {
    setExpectedMessage("file-global-element", "http-global-element");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection-http-file.xml",
                          new HashSet<>(Arrays.asList(getHttpExtension(true), getFileExtension())));
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

  @Test
  public void invalidTestConnectionElement() {
    ExtensionModel loaded = getExtensionModelFrom("validation/testconnection/module-invalid-test-connection.xml",
                                                  new HashSet<>(Arrays.asList(getHttpExtension(false))));
    ProblemsReporter problemsReporter = new ProblemsReporter(loaded);
    new TestConnectionValidator().validate(loaded, problemsReporter);

    assertThat(problemsReporter.getWarnings().size(), is(1));
    assertThat(problemsReporter.getWarnings().get(0).getMessage(), is(format(TEST_CONNECTION_SELECTED_ELEMENT_INVALID,
                                                                             "http-requester-config",
                                                                             MODULE_CONNECTION_MARKER_ATTRIBUTE,
                                                                             "http:request-config")));
    assertThat(problemsReporter.getWarnings().get(0).getComponent(), is(loaded.getConfigurationModels().get(0)));

  }

  private ExtensionModel getFileExtension() {
    return mockedExtension("file", "config", "connection", true);
  }

  private ExtensionModel getHttpExtension(boolean supportsConnectivityTesting) {
    return mockedExtension("http", "request-config", "request-connection", supportsConnectivityTesting);
  }

  private void setExpectedMessage(String... conflictingGlobalElements) {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage(Arrays.stream(conflictingGlobalElements).collect(Collectors.joining(", ")));
  }

  private ExtensionModel mockedExtension(final String name, final String config, final String connectionProvider,
                                         boolean supportsConnectivityTesting) {
    final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    extensionDeclarer.named(name)
        .onVersion("4.0.0")
        .fromVendor("MuleSoft testcase")
        .withCategory(Category.COMMUNITY)
        .withConfig(config)
        .withConnectionProvider(connectionProvider)
        .supportsConnectivityTesting(supportsConnectivityTesting)
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
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    parameters.put("COMPILATION_MODE", true);
    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(extensions), parameters);
  }
}
