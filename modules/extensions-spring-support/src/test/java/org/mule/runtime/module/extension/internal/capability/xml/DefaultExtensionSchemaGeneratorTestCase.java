/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml;

import static com.google.common.collect.ImmutableSet.copyOf;
import static java.lang.Boolean.getBoolean;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.mule.runtime.api.util.MuleSystemProperties.SYSTEM_PROPERTY_PREFIX;
import static org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase.ResourceExtensionUnitTest.newUnitTest;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.compareXML;

import org.mule.extension.test.extension.reconnection.ReconnectionExtension;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.type.TypeCatalog;
import org.mule.runtime.extension.api.dsl.syntax.resources.spi.ExtensionSchemaGenerator;
import org.mule.runtime.module.extension.internal.FileGenerationParameterizedExtensionModelTestCase;
import org.mule.runtime.module.extension.internal.capability.xml.schema.DefaultExtensionSchemaGenerator;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalInnerPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.GlobalPojoConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.ListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.MapConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.StringListConnector;
import org.mule.runtime.module.extension.internal.runtime.connectivity.basic.TestConnector;
import org.mule.tck.size.SmallTest;
import org.mule.test.function.extension.WeaveFunctionExtension;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.implicit.config.extension.extension.api.ImplicitConfigExtension;
import org.mule.test.marvel.MarvelExtension;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.nonimplicit.config.extension.extension.api.NonImplicitConfigExtension;
import org.mule.test.oauth.TestOAuthExtension;
import org.mule.test.petstore.extension.PetStoreConnector;
import org.mule.test.ram.RickAndMortyExtension;
import org.mule.test.semantic.extension.SemanticTermsExtension;
import org.mule.test.soap.extension.FootballSoapExtension;
import org.mule.test.substitutiongroup.extension.SubstitutionGroupExtension;
import org.mule.test.subtypes.extension.SubTypesMappingConnector;
import org.mule.test.transactional.TransactionalExtension;
import org.mule.test.typed.value.extension.extension.TypedValueExtension;
import org.mule.test.values.extension.ValuesExtension;
import org.mule.test.vegan.extension.VeganExtension;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@SmallTest
@RunWith(Parameterized.class)
public class DefaultExtensionSchemaGeneratorTestCase extends FileGenerationParameterizedExtensionModelTestCase {

  private static final boolean UPDATE_EXPECTED_FILES_ON_ERROR =
          getBoolean(SYSTEM_PROPERTY_PREFIX + "extensionSchemas.updateExpectedFilesOnError");

  private final ExtensionSchemaGenerator generator = new DefaultExtensionSchemaGenerator();


  @Parameterized.Parameters(name = "{1}")
  public static Collection<Object[]> data() {

    final List<ResourceExtensionUnitTest> extensions;
    extensions = asList(newUnitTest(JAVA_LOADER, MapConnector.class, "map.xsd"),
            newUnitTest(JAVA_LOADER, ListConnector.class, "list.xsd"),
            newUnitTest(JAVA_LOADER, TestConnector.class, "basic.xsd"),
            newUnitTest(JAVA_LOADER, StringListConnector.class, "string-list.xsd"),
            newUnitTest(JAVA_LOADER, GlobalPojoConnector.class, "global-pojo.xsd"),
            newUnitTest(JAVA_LOADER, GlobalInnerPojoConnector.class, "global-inner-pojo.xsd"),
            newUnitTest(JAVA_LOADER, VeganExtension.class, "vegan.xsd"),
            newUnitTest(JAVA_LOADER, PetStoreConnector.class, "petstore.xsd"),
            newUnitTest(JAVA_LOADER, MetadataExtension.class, "metadata.xsd"),
            newUnitTest(JAVA_LOADER, HeisenbergExtension.class, "heisenberg.xsd"),
            newUnitTest(JAVA_LOADER, SubstitutionGroupExtension.class, "substitutiongroup.xsd"),
            newUnitTest(JAVA_LOADER, TransactionalExtension.class, "tx-ext.xsd"),
            newUnitTest(JAVA_LOADER, SubTypesMappingConnector.class, "subtypes.xsd"),
            newUnitTest(JAVA_LOADER, MarvelExtension.class, "marvel.xsd"),
            newUnitTest(SOAP_LOADER, FootballSoapExtension.class, "soap.xsd"),
            newUnitTest(SOAP_LOADER, RickAndMortyExtension.class, "ram.xsd"),
            newUnitTest(JAVA_LOADER, TypedValueExtension.class, "typed-value.xsd"),
            newUnitTest(JAVA_LOADER, TestOAuthExtension.class, "test-oauth.xsd"),
            newUnitTest(JAVA_LOADER, WeaveFunctionExtension.class, "test-fn.xsd"),
            newUnitTest(JAVA_LOADER, ValuesExtension.class, "values.xsd"),
            newUnitTest(JAVA_LOADER, ImplicitConfigExtension.class, "implicit-config.xsd"),
            newUnitTest(JAVA_LOADER, NonImplicitConfigExtension.class, "non-implicit-config.xsd"),
            newUnitTest(JAVA_LOADER, SemanticTermsExtension.class, "semantic-terms-extension.xsd"),
            newUnitTest(JAVA_LOADER, ReconnectionExtension.class, "reconnection-extension.xsd"));

    return createExtensionModels(extensions);
  }

  @Override
  protected boolean shouldUpdateExpectedFilesOnError() {
    return UPDATE_EXPECTED_FILES_ON_ERROR;
  }

  @Override
  protected String getExpectedFilesDir() {
    return "schemas/";
  }

  @Override
  protected String doGenerate(ExtensionModel extensionUnderTest) throws Exception {
    return generator.generate(extensionUnderTest, new SchemaTestDslContext());
  }

  @Override
  protected void assertEquals(String expectedContent, String actualContent) throws Exception {
    compareXML(expectedContent, actualContent);
  }

  private static class SchemaTestDslContext implements DslResolvingContext {

    @Override
    public Optional<ExtensionModel> getExtension(String name) {
      return ofNullable(EXTENSION_MODELS.get(name));
    }

    @Override
    public Optional<ExtensionModel> getExtensionForType(String typeId) {
      return getTypeCatalog().getDeclaringExtension(typeId).flatMap(this::getExtension);
    }

    @Override
    public Set<ExtensionModel> getExtensions() {
      return copyOf(EXTENSION_MODELS.values());
    }

    @Override
    public TypeCatalog getTypeCatalog() {
      return TypeCatalog.getDefault(copyOf(EXTENSION_MODELS.values()));
    }
  }

}
