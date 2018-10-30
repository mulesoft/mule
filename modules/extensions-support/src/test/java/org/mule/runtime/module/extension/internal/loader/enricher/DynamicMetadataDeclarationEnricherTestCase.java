/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getDeclaration;
import static org.mule.test.metadata.extension.resolver.TestInputAndOutputResolverWithKeyResolver.TEST_INPUT_AND_OUTPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputAndOutputWithAttributesResolverWithKeyResolver.TEST_INPUT_AND_OUTPUT_WITH_ATTRIBUTES_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputResolverWithKeyResolver.TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputResolverWithoutKeyResolver.TEST_INPUT_RESOLVER_WITHOUT_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.TEST_MULTI_LEVEL_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputAnyTypeResolver.METADATA_EXTENSION_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputAnyTypeResolver.TEST_OUTPUT_ANY_TYPE_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputResolverWithKeyResolver.TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.ComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Shape;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;

public class DynamicMetadataDeclarationEnricherTestCase extends AbstractMuleTestCase {

  private static final String CONTENT_METADATA_WITH_KEY_ID = "contentMetadataWithKeyId";
  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    DefaultJavaModelLoaderDelegate loader = new DefaultJavaModelLoaderDelegate(MetadataExtension.class, getProductVersion());
    ExtensionDeclarer declarer =
        loader.declare(new DefaultExtensionLoadingContext(getClass().getClassLoader(), getDefault(emptySet())));
    new DynamicMetadataDeclarationEnricher()
        .enrich(new DefaultExtensionLoadingContext(declarer, this.getClass().getClassLoader(), getDefault(emptySet())));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void parseMetadataAnnotationsOnParameter() {
    final OperationDeclaration operationDeclaration =
        getDeclaration(declaration.getConfigurations().get(0).getOperations(), CONTENT_METADATA_WITH_KEY_ID);
    final List<ParameterDeclaration> parameters = operationDeclaration.getAllParameters();

    assertParameterIsMetadataKeyPart(getDeclaration(parameters, "type"));
    assertParameterIsMetadataContent(getDeclaration(parameters, "content"));
  }

  @Test
  public void declareStaticAndDynamicTypesInOperation() {
    List<ParameterDeclaration> params;
    List<OperationDeclaration> operations = declaration.getConfigurations().get(0).getOperations();
    MetadataType outputMetadataType = IntrospectionUtils.getReturnType(new TypeWrapper(Object.class, TYPE_LOADER));
    MetadataType objectParameterMetadataType = (new ParameterTypeWrapper(Object.class, TYPE_LOADER)).asMetadataType();

    OperationDeclaration dynamicContent = getDeclaration(operations, "contentMetadataWithKeyId");
    assertOutputType(dynamicContent.getOutput(), outputMetadataType, true);
    assertOutputType(dynamicContent.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicContent.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), objectParameterMetadataType);

    OperationDeclaration dynamicOutput = getDeclaration(operations, "outputMetadataWithKeyId");
    assertOutputType(dynamicOutput.getOutput(), outputMetadataType, true);
    assertOutputType(dynamicOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicOutput.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), objectParameterMetadataType);

    OperationDeclaration dynamicContentAndOutput = getDeclaration(operations, "contentAndOutputMetadataWithKeyId");
    assertOutputType(dynamicContentAndOutput.getOutput(), outputMetadataType, true);
    assertOutputType(dynamicContentAndOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicContentAndOutput.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), objectParameterMetadataType);

    operations = declaration.getOperations();
    OperationDeclaration dynamicOutputAndAttributes = getDeclaration(operations, "outputAttributesWithDynamicMetadata");
    assertOutputType(dynamicOutputAndAttributes.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicOutputAndAttributes.getOutputAttributes(), toMetadataType(AbstractOutputAttributes.class), true);
    params = dynamicOutputAndAttributes.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));

    OperationDeclaration staticOutputOnly = getDeclaration(operations, "typeWithDeclaredSubtypesMetadata");
    assertOutputType(staticOutputOnly.getOutput(), toMetadataType(boolean.class), false);
    assertOutputType(staticOutputOnly.getOutputAttributes(), toMetadataType(void.class), false);

    OperationDeclaration staticOutputAndAttributes = getDeclaration(operations, "outputAttributesWithDeclaredSubtypesMetadata");
    assertOutputType(staticOutputAndAttributes.getOutput(), toMetadataType(Shape.class), false);
    assertOutputType(staticOutputAndAttributes.getOutputAttributes(), toMetadataType(AbstractOutputAttributes.class), false);
  }

  @Test
  public void declareStaticAndDynamicTypesInSource() {

    List<SourceDeclaration> messageSources = declaration.getConfigurations().get(0).getMessageSources();
    SourceDeclaration sourceDynamicAttributes = getDeclaration(messageSources, "MetadataSource");

    assertOutputType(sourceDynamicAttributes.getOutput(), TYPE_BUILDER.objectType()
        .with(new ClassInformationAnnotation(Map.class, asList(String.class, Object.class)))
        .openWith(TYPE_LOADER.load(Object.class)).build(), true);
    assertOutputType(sourceDynamicAttributes.getOutputAttributes(), toMetadataType(StringAttributes.class), false);
    assertParameterType(getDeclaration(sourceDynamicAttributes.getAllParameters(), "type"), toMetadataType(String.class));

    messageSources = declaration.getMessageSources();
    SourceDeclaration sourceStaticAttributes = getDeclaration(messageSources, "MetadataSourceWithMultilevel");

    assertOutputType(sourceStaticAttributes.getOutput(), TYPE_BUILDER.objectType()
        .with(new ClassInformationAnnotation(Map.class, asList(String.class, Object.class)))
        .openWith(TYPE_LOADER.load(Object.class)).build(), true);
    assertOutputType(sourceStaticAttributes.getOutputAttributes(), toMetadataType(StringAttributes.class), false);

    List<ParameterDeclaration> locationKey = sourceStaticAttributes.getAllParameters();
    assertParameterType(getDeclaration(locationKey, "continent"), toMetadataType(String.class));
    assertParameterType(getDeclaration(locationKey, "country"), toMetadataType(String.class));
    assertParameterType(getDeclaration(locationKey, "city"), toMetadataType(String.class));

  }

  @Test
  public void declaresTypeResolverInformationForDynamicResolver() throws Exception {
    List<OperationDeclaration> operations = declaration.getConfigurations().get(0).getOperations();

    OperationDeclaration dynamicContent = getDeclaration(operations, "contentMetadataWithKeyId");
    assertCategoryInfo(dynamicContent, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(dynamicContent, of(TEST_OUTPUT_ANY_TYPE_RESOLVER));
    assertAttributesResolverInfo(dynamicContent, empty());
    assertParamResolverInfo(dynamicContent, "type", empty());
    assertParamResolverInfo(dynamicContent, "content", of(TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER));


    OperationDeclaration dynamicOutput = getDeclaration(operations, "outputMetadataWithKeyId");
    assertCategoryInfo(dynamicOutput, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(dynamicOutput, of(TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER));
    assertAttributesResolverInfo(dynamicOutput, empty());
    assertKeysResolverInfo(dynamicOutput, of(TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER));
    assertParamResolverInfo(dynamicOutput, "type", empty());
    assertParamResolverInfo(dynamicOutput, "content", empty());


    OperationDeclaration outputAndmultipleInput = getDeclaration(operations, "outputAndMultipleInputWithKeyId");
    assertCategoryInfo(outputAndmultipleInput, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(outputAndmultipleInput, of(TEST_INPUT_AND_OUTPUT_RESOLVER_WITH_KEY_RESOLVER));
    assertAttributesResolverInfo(outputAndmultipleInput, empty());
    assertParamResolverInfo(outputAndmultipleInput, "type", empty());
    assertParamResolverInfo(outputAndmultipleInput, "firstPerson", of(TEST_INPUT_AND_OUTPUT_RESOLVER_WITH_KEY_RESOLVER));
    assertParamResolverInfo(outputAndmultipleInput, "otherPerson", of(TEST_INPUT_AND_OUTPUT_RESOLVER_WITH_KEY_RESOLVER));


    OperationDeclaration simpleMultiLevelKeyResolver = getDeclaration(operations, "simpleMultiLevelKeyResolver");
    assertCategoryInfo(simpleMultiLevelKeyResolver, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(simpleMultiLevelKeyResolver, empty());
    assertAttributesResolverInfo(simpleMultiLevelKeyResolver, empty());
    assertKeysResolverInfo(simpleMultiLevelKeyResolver, of(TEST_MULTI_LEVEL_KEY_RESOLVER));
    assertParamResolverInfo(simpleMultiLevelKeyResolver, "content", of(TEST_MULTI_LEVEL_KEY_RESOLVER));
    assertParamResolverInfo(simpleMultiLevelKeyResolver, "continent", empty());
    assertParamResolverInfo(simpleMultiLevelKeyResolver, "country", empty());
    assertParamResolverInfo(simpleMultiLevelKeyResolver, "city", empty());

    OperationDeclaration withoutKeysWithKeyId = getDeclaration(operations, "contentMetadataWithoutKeysWithKeyId");
    assertCategoryInfo(withoutKeysWithKeyId, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(withoutKeysWithKeyId, empty());
    assertAttributesResolverInfo(withoutKeysWithKeyId, empty());
    assertKeysResolverInfo(withoutKeysWithKeyId, empty());
    assertParamResolverInfo(withoutKeysWithKeyId, "content", of(TEST_INPUT_RESOLVER_WITHOUT_KEY_RESOLVER));

    List<SourceDeclaration> messageSources = declaration.getConfigurations().get(0).getMessageSources();
    SourceDeclaration sourceDynamicAttributes = getDeclaration(messageSources, "MetadataSource");
    assertCategoryInfo(sourceDynamicAttributes, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(sourceDynamicAttributes, of(TEST_INPUT_AND_OUTPUT_WITH_ATTRIBUTES_RESOLVER_WITH_KEY_RESOLVER));
    assertAttributesResolverInfo(sourceDynamicAttributes, empty());
    assertKeysResolverInfo(sourceDynamicAttributes, of(TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER));
  }

  @Test
  public void typeResolverInformationSkippedForStaticResolver() throws Exception {
    OperationDeclaration operation = getDeclaration(declaration.getOperations(),
                                                    "outputAttributesWithDeclaredSubtypesMetadata");

    Optional<TypeResolversInformationModelProperty> info = operation
        .getModelProperty(TypeResolversInformationModelProperty.class);

    assertThat("Static resolvers information should not be declared in the model",
               info.isPresent(), is(false));
  }

  @Test
  public void typeResolverInformationSkippedForDsql() throws Exception {
    OperationDeclaration query = getDeclaration(declaration.getOperations(), "doQuery");
    Optional<TypeResolversInformationModelProperty> info = query
        .getModelProperty(TypeResolversInformationModelProperty.class);

    assertThat("Query resolvers information should not be declared in the model",
               info.isPresent(), is(false));
  }

  private void assertParameterIsMetadataKeyPart(ParameterDeclaration param) {
    checkIsPresent(param, MetadataKeyPartModelProperty.class);
  }

  private void assertParameterIsMetadataContent(ParameterDeclaration param) {
    assertThat(param.getRole(), is(CONTENT));
  }

  private void assertParameterType(ParameterDeclaration param, MetadataType type) {
    assertThat(param.getType(), equalTo(type));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    assertThat(output.getType(), equalTo(type));
    assertThat(output.hasDynamicType(), is(isDynamic));
  }

  private void assertParamResolverInfo(ComponentDeclaration declaration, String param, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getParameterResolverName(param), "ParameterResolver", expectedName);
  }

  private void assertOutputResolverInfo(ComponentDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getOutputResolverName(), "OutputResolver", expectedName);
  }

  private void assertAttributesResolverInfo(ComponentDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getAttributesResolverName(), "AttributesResolver", expectedName);
  }

  private void assertKeysResolverInfo(ComponentDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getKeysResolverName(), "KeysResolver", expectedName);
  }

  private void assertCategoryInfo(ComponentDeclaration declaration, String expectedName) {
    TypeResolversInformationModelProperty info = getResolversInfo(declaration);
    assertThat("Name miss match for the resolvers category: ",
               info.getCategoryName(), is(equalTo(expectedName)));
  }

  private TypeResolversInformationModelProperty getResolversInfo(ComponentDeclaration declaration) {
    Optional<TypeResolversInformationModelProperty> info = declaration
        .getModelProperty(TypeResolversInformationModelProperty.class);

    if (!info.isPresent()) {
      fail("Expected to have information of TypeResolvers but found no model property of class: "
          + TypeResolversInformationModelProperty.class.getName());
    }
    return info.get();
  }

  private void assertResolverInfo(ComponentDeclaration declaration,
                                  Function<TypeResolversInformationModelProperty, Optional<String>> resolverSupplier,
                                  String kind, Optional<String> expectedName) {
    TypeResolversInformationModelProperty info = getResolversInfo(declaration);
    Optional<String> resolverName = resolverSupplier.apply(info);
    if (expectedName.isPresent() && !resolverName.isPresent()) {
      fail(format("Expected %s name to be '%s' but it was not declared in the model. "
          + "Information was: %s", kind, expectedName, info.toString()));

    } else if (!expectedName.isPresent() && resolverName.isPresent()) {
      fail(format("Expected %s name to be empty, but a declaration was found in the model. "
          + "Information was: %s", kind, info.toString()));
    }
    assertThat("Name miss match for the " + kind,
               resolverName, is(equalTo(expectedName)));
  }
}
