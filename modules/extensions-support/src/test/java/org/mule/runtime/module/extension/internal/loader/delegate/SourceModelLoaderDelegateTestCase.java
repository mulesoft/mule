/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.delegate;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.test.metadata.extension.resolver.TestInputAndOutputWithAttributesResolverWithKeyResolver.TEST_INPUT_AND_OUTPUT_WITH_ATTRIBUTES_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputOutputSourceResolverWithKeyResolver.TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestInputResolver.INPUT_RESOLVER_NAME;
import static org.mule.test.metadata.extension.resolver.TestMetadataInputCarResolver.TEST_INPUT_CAR_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestMetadataInputHouseResolver.TEST_INPUT_HOUSE_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestMetadataInputPersonResolver.TEST_INPUT_PERSON_RESOLVER;
import static org.mule.test.metadata.extension.resolver.TestOutputAnyTypeResolver.METADATA_EXTENSION_RESOLVER;
import static org.mule.test.module.extension.internal.util.ExtensionDeclarationTestUtils.declarerFor;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceCallbackDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.property.ResolverInformation;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.MetadataExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

public class SourceModelLoaderDelegateTestCase extends AbstractMuleTestCase {

  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    ExtensionDeclarer declarer = declarerFor(MetadataExtension.class, getProductVersion());
    declaration = declarer.getDeclaration();
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
  public void declareDynamicInputTypesInSource() {

    List<SourceDeclaration> messageSources = declaration.getConfigurations().get(0).getMessageSources();
    SourceDeclaration sourceDeclaration = getDeclaration(messageSources, "MetadataSourceWithCallbackParameters");

    SourceCallbackDeclaration successSourceCallback = sourceDeclaration.getSuccessCallback().get();
    assertParamResolverInfo(successSourceCallback, "response", of(TEST_INPUT_PERSON_RESOLVER));
    assertParamResolverInfo(successSourceCallback, "successObject", of(TEST_INPUT_CAR_RESOLVER));

    SourceCallbackDeclaration errorSourceCallback = sourceDeclaration.getErrorCallback().get();

    assertParamResolverInfo(errorSourceCallback, "response", of(TEST_INPUT_HOUSE_RESOLVER));
    assertParamResolverInfo(errorSourceCallback, "errorObject", of(INPUT_RESOLVER_NAME));
  }

  @Test
  public void declaresTypeResolverInformationForDynamicResolver() throws Exception {
    List<SourceDeclaration> messageSources = declaration.getConfigurations().get(0).getMessageSources();
    SourceDeclaration sourceDynamicAttributes = getDeclaration(messageSources, "MetadataSource");
    assertCategoryInfo(sourceDynamicAttributes, METADATA_EXTENSION_RESOLVER);
    assertOutputResolverInfo(sourceDynamicAttributes, of(TEST_INPUT_AND_OUTPUT_WITH_ATTRIBUTES_RESOLVER_WITH_KEY_RESOLVER));
    assertAttributesResolverInfo(sourceDynamicAttributes, empty());
    assertKeysResolverInfo(sourceDynamicAttributes, of(TEST_INPUT_OUTPUT_SOURCE_RESOLVER_WITH_KEY_RESOLVER));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    MatcherAssert.assertThat(output.getType(), equalTo(type));
    MatcherAssert.assertThat(output.hasDynamicType(), is(isDynamic));
  }

  private void assertParameterType(ParameterDeclaration param, MetadataType type) {
    MatcherAssert.assertThat(param.getType(), equalTo(type));
  }

  private void assertParamResolverInfo(BaseDeclaration declaration, String param, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getParameterResolver(param), "ParameterResolver", expectedName);
  }

  private void assertKeysResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getKeysResolver(), "KeysResolver", expectedName);
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
    assertThat("Name miss match for the " + kind,
               resolverName.map(ResolverInformation::getResolverName), is(equalTo(expectedName)));
  }

  private void assertCategoryInfo(BaseDeclaration declaration, String expectedName) {
    TypeResolversInformationModelProperty info = getResolversInfo(declaration);
    assertThat("Name miss match for the resolvers category: ",
               info.getCategoryName(), is(equalTo(expectedName)));
  }

  private void assertOutputResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getOutputResolver(), "OutputResolver", expectedName);
  }

  private void assertAttributesResolverInfo(BaseDeclaration declaration, Optional<String> expectedName) {
    assertResolverInfo(declaration, info -> info.getAttributesResolver(), "AttributesResolver", expectedName);
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

  private static <T extends NamedDeclaration> T getDeclaration(List<T> operationList, String name) {
    return operationList.stream().filter(operation -> operation.getName().equals(name)).collect(toList()).get(0);
  }
}
