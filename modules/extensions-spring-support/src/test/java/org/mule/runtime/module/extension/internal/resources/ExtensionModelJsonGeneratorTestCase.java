/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.resources;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalInnerPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.ListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.MapConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.StringListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.TestConnector;
import org.mule.runtime.module.extension.soap.internal.loader.SoapExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.ram.RickAndMortyExtension;
import org.mule.test.soap.extension.FootballSoapExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class ExtensionModelJsonGeneratorTestCase extends AbstractMuleTestCase {

  static final Map<String, ExtensionModel> extensionModels = new HashMap<>();

  private static ExtensionModelLoader javaLoader = new DefaultJavaExtensionModelLoader();
  private static ExtensionModelLoader soapLoader = new SoapExtensionModelLoader();

  @Parameterized.Parameter(0)
  public ExtensionModel extensionUnderTest;

  @Parameterized.Parameter(1)
  public String expectedSource;

  private ExtensionModelJsonSerializer generator;
  private String expectedJson;


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    final ClassLoader classLoader = ExtensionModelJsonGeneratorTestCase.class.getClassLoader();
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(DeclarationEnricher.class, classLoader))
        .thenReturn(asList(new JavaXmlDeclarationEnricher()));

    final List<ExtensionJsonGeneratorTestUnit> extensions = Arrays.asList(
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             MapConnector.class,
                                                                                                             "map.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             ListConnector.class,
                                                                                                             "list.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             TestConnector.class,
                                                                                                             "basic.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             StringListConnector.class,
                                                                                                             "string-list.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             GlobalPojoConnector.class,
                                                                                                             "global-pojo.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             GlobalInnerPojoConnector.class,
                                                                                                             "global-inner-pojo.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             VeganExtension.class,
                                                                                                             "vegan.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             PetStoreConnector.class,
                                                                                                             "petstore.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             MetadataExtension.class,
                                                                                                             "metadata.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             HeisenbergExtension.class,
                                                                                                             "heisenberg.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             TransactionalExtension.class,
                                                                                                             "tx-ext.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             SubTypesMappingConnector.class,
                                                                                                             "subtypes.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             MarvelExtension.class,
                                                                                                             "marvel.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(soapLoader,
                                                                                                             FootballSoapExtension.class,
                                                                                                             "soap.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(soapLoader,
                                                                                                             RickAndMortyExtension.class,
                                                                                                             "ram.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             TypedValueExtension.class,
                                                                                                             "typed-value.json"),
                                                                          new ExtensionJsonGeneratorTestUnit(javaLoader,
                                                                                                             TestOAuthExtension.class,
                                                                                                             "test-oauth.json"));

    BiFunction<Class<?>, ExtensionModelLoader, ExtensionModel> createExtensionModel = (extension, loader) -> {
      ExtensionModel model = loadExtension(extension, loader);

      if (extensionModels.put(model.getName(), model) != null) {
        throw new IllegalArgumentException(format("Extension names must be unique. Name [%s] for extension [%s] was already used",
                                                  model.getName(), extension.getName()));
      }

      return model;
    };

    return extensions.stream()
        .map(e -> new Object[] {createExtensionModel.apply(e.getExtensionClass(), e.getLoader()), e.getFileName()})
        .collect(toList());
  }

  @Before
  public void setup() throws IOException {
    generator = new ExtensionModelJsonSerializer(true);
    expectedJson = getResourceAsString("models/" + expectedSource, getClass()).trim();
  }

  @Test
  public void generate() throws Exception {
    String json = generator.serialize(extensionUnderTest).trim();
    if (!json.equals(expectedJson)) {
      System.out.println(json);
    }
    assertThat(json, is(equalTo(expectedJson)));
  }

  public static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader) {
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, "4.0.0-SNAPSHOT");
    // TODO MULE-11797: as this utils is consumed from
    // org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel),
    // this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(emptySet());
    return loader.loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  static class ExtensionJsonGeneratorTestUnit {

    final ExtensionModelLoader loader;
    final Class<?> extensionClass;
    final String fileName;

    ExtensionJsonGeneratorTestUnit(ExtensionModelLoader loader, Class<?> extensionClass, String fileName) {
      this.loader = loader;
      this.extensionClass = extensionClass;
      this.fileName = fileName;
    }

    ExtensionModelLoader getLoader() {
      return loader;
    }

    Class<?> getExtensionClass() {
      return extensionClass;
    }

    String getFileName() {
      return fileName;
    }
  }
}
