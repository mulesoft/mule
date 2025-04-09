/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.extension.internal.loader.xml.validation;


import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.dsl.DslResolvingContext.nullDslResolvingContext;
import static org.mule.runtime.core.api.extension.provider.MuleExtensionModelProvider.getExtensionModel;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionLoaderDelegate.MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE;
import static org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader.RESOURCE_XML;
import static org.mule.runtime.extension.internal.loader.xml.validator.TestConnectionValidator.TEST_CONNECTION_SELECTED_ELEMENT_INVALID;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.connection.ConnectionManagementType;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.loader.ProblemsReporter;
import org.mule.runtime.extension.internal.loader.xml.XmlExtensionModelLoader;
import org.mule.runtime.extension.internal.loader.xml.validator.TestConnectionValidator;
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
    getExtensionModelFrom("validation/testconnection/module-multiple-global-element-xmlns-connection-true.xml",
                          new HashSet<>(asList(getPetstoreExtension(true))));
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefined() {
    setExpectedMessage("first-config-not-defined-to-which-one-do-test-connection",
                       "second-config-not-defined-to-which-one-do-test-connection");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection.xml",
                          new HashSet<>(asList(getPetstoreExtension(true))));
  }

  @Test
  public void multipleGlobalElementsWithTestConnectionAndNotEvenOneDefinedHttpAndFile() {
    setExpectedMessage("file-global-element", "http-global-element");
    getExtensionModelFrom("validation/testconnection/module-not-defined-test-connection-http-file.xml",
                          new HashSet<>(asList(getPetstoreExtension(true), getFileExtension())));
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
                                                  new HashSet<>(asList(getPetstoreExtension(false))));
    ProblemsReporter problemsReporter = new ProblemsReporter(loaded);
    new TestConnectionValidator().validate(loaded, problemsReporter);

    assertThat(problemsReporter.getWarnings().size(), is(1));
    assertThat(problemsReporter.getWarnings().get(0).getMessage(), is(format(TEST_CONNECTION_SELECTED_ELEMENT_INVALID,
                                                                             "http-requester-config",
                                                                             MODULE_CONNECTION_MARKER_ANNOTATION_ATTRIBUTE,
                                                                             "petstore:config")));
    assertThat(problemsReporter.getWarnings().get(0).getComponent(), is(loaded.getConfigurationModels().get(0)));

  }

  private ExtensionModel getFileExtension() {
    return mockedExtension("file", "config", "connection", true);
  }

  private ExtensionModel getPetstoreExtension(boolean supportsConnectivityTesting) {
    return mockedExtension("petstore", "config", "connection", supportsConnectivityTesting);
  }

  private void setExpectedMessage(String... conflictingGlobalElements) {
    exception.expect(MuleRuntimeException.class);
    exception.expectMessage(Arrays.stream(conflictingGlobalElements).collect(Collectors.joining(", ")));
  }

  private ExtensionModel mockedExtension(final String name, final String config, final String connectionProvider,
                                         boolean supportsConnectivityTesting) {
    final ClassTypeLoader typeLoader = ExtensionsTypeLoaderFactory.getDefault()
        .createTypeLoader(ConnectivityTestingFailuresTestCase.class.getClassLoader());

    final ExtensionDeclarer extensionDeclarer = new ExtensionDeclarer();
    final ConfigurationDeclarer configDeclarer = extensionDeclarer.named(name)
        .onVersion("4.0.0")
        .fromVendor("MuleSoft testcase")
        .withCategory(Category.COMMUNITY)
        .withConfig(config);
    configDeclarer
        .withConnectionProvider(connectionProvider)
        .supportsConnectivityTesting(supportsConnectivityTesting)
        .withConnectionManagementType(ConnectionManagementType.NONE)
        .onDefaultParameterGroup()
        .withRequiredParameter("name")
        .ofType(typeLoader.load(String.class))
        .asComponentId();

    return new ExtensionModelLoader() {

      @Override
      public String getId() {
        return ConnectivityTestingFailuresTestCase.class.getName();
      }

      @Override
      protected void declareExtension(ExtensionLoadingContext context) {
        // nothing to do
      }
    }.loadExtensionModel(extensionDeclarer, builder(currentThread().getContextClassLoader(),
                                                    nullDslResolvingContext())
        .build());
  }

  private ExtensionModel getExtensionModelFrom(String modulePath) {
    return getExtensionModelFrom(modulePath, emptySet());
  }

  private ExtensionModel getExtensionModelFrom(String modulePath, Set<ExtensionModel> depedencyExtensions) {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(RESOURCE_XML, modulePath);
    // TODO MULE-14517: This workaround should be replaced for a better and more complete mechanism
    parameters.put("COMPILATION_MODE", true);

    Set<ExtensionModel> allExtensions = new HashSet<>(depedencyExtensions);
    allExtensions.add(getExtensionModel());

    return new XmlExtensionModelLoader().loadExtensionModel(getClass().getClassLoader(), getDefault(allExtensions), parameters);
  }
}
