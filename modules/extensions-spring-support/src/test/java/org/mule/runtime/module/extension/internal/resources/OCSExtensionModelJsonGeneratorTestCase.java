/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ENABLED;
import static org.mule.runtime.module.extension.internal.resources.ExtensionModelJsonGeneratorTestCase.ExtensionJsonGeneratorTestUnit.newTestUnit;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.test.oauth.TestOAuthExtension;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.AfterClass;
import org.junit.runners.Parameterized;

public class OCSExtensionModelJsonGeneratorTestCase extends ExtensionModelJsonGeneratorTestCase {

  static final Map<String, ExtensionModel> extensionModels = new HashMap<>();

  private static ExtensionModelLoader javaLoader = new DefaultJavaExtensionModelLoader();

  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    System.setProperty(OCS_ENABLED, "true");
    final ClassLoader classLoader = ExtensionModelJsonGeneratorTestCase.class.getClassLoader();
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(DeclarationEnricher.class, classLoader))
        .thenReturn(asList(new JavaXmlDeclarationEnricher()));

    List<ExtensionJsonGeneratorTestUnit> extensions;
    extensions = asList(newTestUnit(javaLoader, TestOAuthExtension.class, "test-oauth-ocs.json"));

    BiFunction<Class<?>, ExtensionModelLoader, ExtensionModel> createExtensionModel = (extension, loader) -> {
      ExtensionModel model = loadExtension(extension, loader);

      if (extensionModels.put(model.getName(), model) != null) {
        throw new IllegalArgumentException(format("Extension names must be unique. Name [%s] for extension [%s] was already used",
                                                  model.getName(), extension.getName()));
      }

      return model;
    };

    return extensions.stream()
        .map(e -> e.toTestParams(createExtensionModel))
        .collect(toList());
  }

  @AfterClass
  public static void afterClass() {
    System.clearProperty(OCS_ENABLED);
  }

}
