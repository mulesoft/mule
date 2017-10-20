/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.core.api.util.FileUtils.stringToFile;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsString;
import static org.mule.runtime.core.api.util.IOUtils.getResourceAsUrl;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.api.loader.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.runtime.module.extension.internal.capability.xml.DefaultExtensionSchemaGeneratorTestCase.SchemaGeneratorTestUnit.newTestUnit;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionModelLoader;
import org.mule.runtime.module.extension.api.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.runtime.module.extension.internal.capability.xml.schema.DefaultExtensionSchemaGenerator;
import org.mule.runtime.module.extension.internal.loader.enricher.JavaXmlDeclarationEnricher;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalInnerPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.ListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.MapConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.StringListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.TestConnector;
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
import org.mule.test.soap.extension.FootballSoapExtension;
import org.mule.test.substitutiongroup.extension.SubstitutionGroupExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class DefaultExtensionSchemaGeneratorTestCase extends AbstractMuleTestCase {

  static final Map<String, ExtensionModel> extensionModels = new HashMap<>();

  private static ExtensionModelLoader javaLoader = new DefaultJavaExtensionModelLoader();
  private static ExtensionModelLoader soapLoader = new SoapExtensionModelLoader();

  @Parameterized.Parameter(0)
  public ExtensionModel extensionUnderTest;

  @Parameterized.Parameter(1)
  public String expectedXSD;

  private ExtensionSchemaGenerator generator = new DefaultExtensionSchemaGenerator();
  private String expectedSchema;


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {
    final ClassLoader classLoader = DefaultExtensionSchemaGeneratorTestCase.class.getClassLoader();
    final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
    when(serviceRegistry.lookupProviders(DeclarationEnricher.class, classLoader))
        .thenReturn(asList(new JavaXmlDeclarationEnricher()));

    final List<SchemaGeneratorTestUnit> extensions;
    extensions = asList(newTestUnit(javaLoader, MapConnector.class, "map.xsd"),
                        newTestUnit(javaLoader, ListConnector.class, "list.xsd"),
                        newTestUnit(javaLoader, TestConnector.class, "basic.xsd"),
                        newTestUnit(javaLoader, StringListConnector.class, "string-list.xsd"),
                        newTestUnit(javaLoader, GlobalPojoConnector.class, "global-pojo.xsd"),
                        newTestUnit(javaLoader, GlobalInnerPojoConnector.class, "global-inner-pojo.xsd"),
                        newTestUnit(javaLoader, VeganExtension.class, "vegan.xsd"),
                        newTestUnit(javaLoader, PetStoreConnector.class, "petstore.xsd"),
                        newTestUnit(javaLoader, MetadataExtension.class, "metadata.xsd"),
                        newTestUnit(javaLoader, HeisenbergExtension.class, "heisenberg.xsd"),
                        newTestUnit(javaLoader, SubstitutionGroupExtension.class, "substitutiongroup.xsd"),
                        newTestUnit(javaLoader, TransactionalExtension.class, "tx-ext.xsd"),
                        newTestUnit(javaLoader, SubTypesMappingConnector.class, "subtypes.xsd"),
                        newTestUnit(javaLoader, MarvelExtension.class, "marvel.xsd"),
                        newTestUnit(soapLoader, FootballSoapExtension.class, "soap.xsd"),
                        newTestUnit(soapLoader, RickAndMortyExtension.class, "ram.xsd"),
                        newTestUnit(javaLoader, TypedValueExtension.class, "typed-value.xsd"),
                        newTestUnit(javaLoader, TestOAuthExtension.class, "test-oauth.xsd"),
                        newTestUnit(javaLoader, WeaveFunctionExtension.class, "test-fn.xsd"),
                        newTestUnit(javaLoader, ValuesExtension.class, "values.xsd"));

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
    expectedSchema = getResourceAsString("schemas/" + expectedXSD, getClass());
  }

  @Test
  public void generate() throws Exception {
    String schema = generator.generate(extensionUnderTest, new SchemaTestDslContext());
    try {
      compareXML(expectedSchema, schema);
    } catch (Throwable t) {
      if (shouldUpdateExpectedFilesOnError()) {
        File root = new File(getResourceAsUrl("schemas/" + expectedXSD, getClass()).toURI()).getParentFile()
            .getParentFile().getParentFile().getParentFile();
        File testDir = new File(root, "src/test/resources/schemas");
        File target = new File(testDir, expectedXSD);
        stringToFile(target.getAbsolutePath(), schema);

        System.out.println(expectedXSD + " fixed");
      }
      throw t;
    }
  }

  private static class SchemaTestDslContext implements DslResolvingContext {

    @Override
    public Optional<ExtensionModel> getExtension(String name) {
      return ofNullable(extensionModels.get(name));
    }

    @Override
    public Optional<ExtensionModel> getExtensionForType(String typeId) {
      return getTypeCatalog().getDeclaringExtension(typeId).flatMap(this::getExtension);
    }

    @Override
    public Set<ExtensionModel> getExtensions() {
      return copyOf(extensionModels.values());
    }

    @Override
    public TypeCatalog getTypeCatalog() {
      return TypeCatalog.getDefault(copyOf(extensionModels.values()));
    }
  }

  public static ExtensionModel loadExtension(Class<?> clazz, ExtensionModelLoader loader) {
    Map<String, Object> params = new HashMap<>();
    params.put(TYPE_PROPERTY_NAME, clazz.getName());
    params.put(VERSION, getProductVersion());
    //TODO MULE-11797: as this utils is consumed from org.mule.runtime.module.extension.internal.capability.xml.schema.AbstractXmlResourceFactory.generateResource(org.mule.runtime.api.meta.model.ExtensionModel), this util should get dropped once the ticket gets implemented.
    final DslResolvingContext dslResolvingContext = getDefault(new LinkedHashSet<>(extensionModels.values()));
    return loader.loadExtensionModel(clazz.getClassLoader(), dslResolvingContext, params);
  }

  static class SchemaGeneratorTestUnit {

    final ExtensionModelLoader loader;
    final Class<?> extensionClass;
    final String fileName;

    SchemaGeneratorTestUnit(ExtensionModelLoader loader, Class<?> extensionClass, String fileName) {
      this.loader = loader;
      this.extensionClass = extensionClass;
      this.fileName = fileName;
    }

    static SchemaGeneratorTestUnit newTestUnit(ExtensionModelLoader loader, Class<?> extensionClass, String fileName) {
      return new SchemaGeneratorTestUnit(loader, extensionClass, fileName);
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
