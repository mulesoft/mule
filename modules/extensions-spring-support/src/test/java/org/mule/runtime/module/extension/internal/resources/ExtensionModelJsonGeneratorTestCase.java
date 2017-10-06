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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.resources.ExtensionModelJsonGeneratorTestCase.ExtensionJsonGeneratorTestUnit.newTestUnit;

import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.extension.api.persistence.ExtensionModelJsonSerializer;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.soap.api.loader.SoapExtensionModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.test.function.extension.WeaveFunctionExtension;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.ram.RickAndMortyExtension;
import org.mule.test.substitutiongroup.extension.SubstitutionGroupExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.skyscreamer.jsonassert.JSONAssert;

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

    List<ExtensionJsonGeneratorTestUnit> extensions;
    extensions = asList(
                        newTestUnit(javaLoader, VeganExtension.class, "vegan.json"),
                        newTestUnit(javaLoader, PetStoreConnector.class, "petstore.json"),
                        newTestUnit(javaLoader, MetadataExtension.class, "metadata.json"),
                        newTestUnit(javaLoader, HeisenbergExtension.class, "heisenberg.json"),
                        newTestUnit(javaLoader, SubstitutionGroupExtension.class, "substitutiongroup.json"),
                        newTestUnit(javaLoader, TransactionalExtension.class, "tx-ext.json"),
                        newTestUnit(javaLoader, SubTypesMappingConnector.class, "subtypes.json"),
                        newTestUnit(javaLoader, MarvelExtension.class, "marvel.json"),
                        newTestUnit(soapLoader, RickAndMortyExtension.class, "ram.json"),
                        newTestUnit(javaLoader, TypedValueExtension.class, "typed-value.json"),
                        newTestUnit(javaLoader, TestOAuthExtension.class, "test-oauth.json"),
                        newTestUnit(javaLoader, WeaveFunctionExtension.class, "test-fn.json"),
                        newTestUnit(javaLoader, ValuesExtension.class, "values.json"));

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

  /**
   * Utility to batch fix input files when severe model changes are introduced.
   * Use carefully, not a mechanism to get away with anything.
   * First check why the generated json is different and make sure you're not introducing any bugs.
   * This should NEVER be committed as true
   *
   * @return whether or not the "expected" test files should be updated when comparison fails
   */
  private boolean shouldUpdateExpectedFilesOnError() {
    return false;
  }

  @Before
  public void setup() throws IOException {
    generator = new ExtensionModelJsonSerializer(true);
    expectedJson = getResourceAsString("models/" + expectedSource, getClass()).trim();
  }

  @Test
  public void generate() throws Exception {
    final String json = generator.serialize(extensionUnderTest).trim();
    try {
      JSONAssert.assertEquals(expectedJson, json, true);
    } catch (AssertionError e) {

      if (shouldUpdateExpectedFilesOnError()) {
        updateExpectedJson(json);
      } else {
        System.out.println("Expected: \n " + expectedJson);
        System.out.println("\n\nBut Got: \n " + json);

        throw e;
      }
    }
  }

  @Test
  public void load() throws Exception {
    ExtensionModel result = generator.deserialize(expectedJson);
    assertThat(result, is(extensionUnderTest));
  }

  private void updateExpectedJson(String json) throws URISyntaxException, IOException {
    File root = new File(getResourceAsUrl("models/" + expectedSource, getClass()).toURI()).getParentFile()
        .getParentFile().getParentFile().getParentFile();
    File testDir = new File(root, "src/test/resources/models");
    File target = new File(testDir, expectedSource);
    stringToFile(target.getAbsolutePath(), json);

    System.out.println(expectedSource + " fixed");
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

    private ExtensionJsonGeneratorTestUnit(ExtensionModelLoader loader, Class<?> extensionClass, String fileName) {
      this.loader = loader;
      this.extensionClass = extensionClass;
      this.fileName = fileName;
    }

    static ExtensionJsonGeneratorTestUnit newTestUnit(ExtensionModelLoader modelLoader, Class<?> extensionClass,
                                                      String fileName) {
      return new ExtensionJsonGeneratorTestUnit(modelLoader, extensionClass, fileName);
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
