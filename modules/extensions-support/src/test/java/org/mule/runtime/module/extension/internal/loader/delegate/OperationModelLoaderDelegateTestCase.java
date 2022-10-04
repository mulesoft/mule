/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.test.metadata.extension.resolver.SdkTestInputResolverWithKeyResolver.SDK_TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.SdkTestOutputAnyTypeResolver.TEST_OUTPUT_ANY_TYPE_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputAndOutputResolverWithKeyResolver.TEST_INPUT_AND_OUTPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputAndOutputWithAttributesResolverWithKeyResolver.TEST_INPUT_AND_OUTPUT_WITH_ATTRIBUTES_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputResolverWithoutKeyResolver.TEST_INPUT_RESOLVER_WITHOUT_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.TEST_MULTI_LEVEL_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputAnyTypeResolver.METADATA_EXTENSION_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputResolverWithKeyResolver.TEST_OUTPUT_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterTypeWrapper;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.TypeWrapper;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Shape;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

public class OperationModelLoaderDelegateTestCase extends AbstractMuleTestCase {

  private static final String SDK_CONTENT_METADATA_WITH_KEY_ID = "sdkContentMetadataWithKeyId";
  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = declarerFor(MetadataExtension.class, getProductVersion());
    declaration = declarer.getDeclaration();
  }

  @Test
  public void parseMetadataAnnotationsOnParameter() {
    final OperationDeclaration operationDeclaration =
        getDeclaration(declaration.getConfigurations().get(0).getOperations(), SDK_CONTENT_METADATA_WITH_KEY_ID);
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

    OperationDeclaration dynamicContent = getDeclaration(operations, SDK_CONTENT_METADATA_WITH_KEY_ID);
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
    OperationDeclaration dynamicOutputAndAttributes = getDeclaration(operations, "sdkOutputAttributesWithDynamicMetadata");
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
  public void typeResolverInformationSkippedForStaticResolver() throws Exception {
    OperationDeclaration operation = getDeclaration(declaration.getOperations(),
                                                    "outputAttributesWithDeclaredSubtypesMetadata");

    Optional<TypeResolversInformationModelProperty> info = operation
        .getModelProperty(TypeResolversInformationModelProperty.class);

    MatcherAssert.assertThat("Static resolvers information should not be declared in the model",
                             info.isPresent(), is(false));
  }

  @Test
  public void declaresTypeResolverInformationForDynamicResolver() throws Exception {
    List<OperationDeclaration> operations = declaration.getConfigurations().get(0).getOperations();

    OperationDeclaration dynamicContent = getDeclaration(operations, SDK_CONTENT_METADATA_WITH_KEY_ID);
    assertCategoryInfo(dynamicContent, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(dynamicContent, of(TEST_OUTPUT_ANY_TYPE_RESOLVER));
    assertAttributesResolverInfo(dynamicContent, empty());
    assertParamResolverInfo(dynamicContent, "type", empty());
    assertParamResolverInfo(dynamicContent, "content", of(SDK_TEST_INPUT_RESOLVER_WITH_KEY_RESOLVER));


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

  private void assertParameterIsMetadataKeyPart(ParameterDeclaration param) {
    checkIsPresent(param, MetadataKeyPartModelProperty.class);
  }

  private void assertParameterIsMetadataContent(ParameterDeclaration param) {
    MatcherAssert.assertThat(param.getRole(), is(CONTENT));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    MatcherAssert.assertThat(output.getType(), equalTo(type));
    MatcherAssert.assertThat(output.hasDynamicType(), is(isDynamic));
  }

  private void assertOutputResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getOutputResolver(), "OutputResolver", expectedName);
  }

  private void assertParameterType(ParameterDeclaration param, MetadataType type) {
    MatcherAssert.assertThat(param.getType(), equalTo(type));
  }

  private void assertCategoryInfo(BaseDeclaration declaration, String expectedName) {
    TypeResolversInformationModelProperty info = getResolversInfo(declaration);
    MatcherAssert.assertThat("Name miss match for the resolvers category: ",
                             info.getCategoryName(), is(equalTo(expectedName)));
  }

  private void assertResolverInfo(BaseDeclaration declaration,
                                  Function<TypeResolversInformationModelProperty, Optional<ResolverInformation>> resolverSupplier,
                                  String kind, Optional<String> expectedName) {
    TypeResolversInformationModelProperty info = getResolversInfo(declaration);
    Optional<ResolverInformation> resolverName = resolverSupplier.apply(info);
    if (expectedName.isPresent() && !resolverName.isPresent()) {
      fail(format("Expected %s name to be '%s' but it was not declared in the model. "
          + "Information was: %s", kind, expectedName, info.toString()));

    } else if (!expectedName.isPresent() && resolverName.isPresent()) {
      fail(format("Expected %s name to be empty, but a declaration was found in the model. "
          + "Information was: %s", kind, info.toString()));
    }
    MatcherAssert.assertThat("Name miss match for the " + kind,
                             resolverName.map(ResolverInformation::getResolverName), is(equalTo(expectedName)));
  }

  private void assertAttributesResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getAttributesResolver(), "AttributesResolver", expectedName);
  }

  private void assertParamResolverInfo(BaseDeclaration declaration, String param, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getParameterResolver(param), "ParameterResolver", expectedName);
  }

  private void assertKeysResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getKeysResolver(), "KeysResolver", expectedName);
  }

  private static <T extends NamedDeclaration> T getDeclaration(List<T> operationList, String name) {
    return operationList.stream().filter(operation -> operation.getName().equals(name)).collect(toList()).get(0);
  }

  private TypeResolversInformationModelProperty getResolversInfo(BaseDeclaration declaration) {
    Optional<TypeResolversInformationModelProperty> info = declaration
        .getModelProperty(TypeResolversInformationModelProperty.class);

    if (!info.isPresent()) {
      fail("Expected to have information of TypeResolvers but found no model property of class: "
          + TypeResolversInformationModelProperty.class.getName());
    }
    return info.get();
  }

  private <T extends ModelProperty> T checkIsPresent(BaseDeclaration declaration, Class<T> modelProperty) {
    final Optional<T> property = declaration.getModelProperty(modelProperty);
    assertThat(property.isPresent(), is(true));
    return property.get();
  }
}
