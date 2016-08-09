/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.enricher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.core.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.introspection.enricher.EnricherTestUtils.getDeclaration;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;

import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OperationDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.OutputDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.api.introspection.declaration.type.annotation.TypeAliasAnnotation;
import org.mule.runtime.extension.api.introspection.property.MetadataContentModelProperty;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.DefaultDescribingContext;
import org.mule.runtime.module.extension.internal.introspection.describer.AnnotationsBasedDescriber;
import org.mule.runtime.module.extension.internal.introspection.version.StaticVersionResolver;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Shape;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class DynamicMetadataModelEnricherTestCase {

  private static final String CONTENT_METADATA_WITH_KEY_ID = "contentMetadataWithKeyId";
  private ExtensionDeclaration declaration;

  @Before
  public void setUp() {
    final AnnotationsBasedDescriber basedDescriber =
        new AnnotationsBasedDescriber(MetadataExtension.class, new StaticVersionResolver(getProductVersion()));
    ExtensionDeclarer declarer = basedDescriber.describe(new DefaultDescribingContext(getClass().getClassLoader()));
    new DynamicMetadataModelEnricher().enrich(new DefaultDescribingContext(declarer, this.getClass().getClassLoader()));
    declaration = declarer.getDeclaration();
  }

  @Test
  public void parseMetadataAnnotationsOnParameter() {
    final OperationDeclaration operationDeclaration = getDeclaration(declaration.getOperations(), CONTENT_METADATA_WITH_KEY_ID);
    final List<ParameterDeclaration> parameters = operationDeclaration.getParameters();

    assertParameterIsMetadataKeyPart(getDeclaration(parameters, "type"));
    assertParameterIsMetadataContent(getDeclaration(parameters, "content"));
  }

  @Test
  public void declareStaticAndDynamicTypesInOperation() {
    List<ParameterDeclaration> params;
    final List<OperationDeclaration> operations = declaration.getOperations();

    OperationDeclaration dynamicContent = getDeclaration(operations, "contentMetadataWithKeyId");
    assertOutputType(dynamicContent.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicContent.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicContent.getParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class), false);
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class), true);

    OperationDeclaration dynamicOutput = getDeclaration(operations, "outputMetadataWithKeyId");
    assertOutputType(dynamicOutput.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicOutput.getParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class), false);
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class), true);

    OperationDeclaration dynaimcContentAndOutput = getDeclaration(operations, "contentAndOutputMetadataWithKeyId");
    assertOutputType(dynaimcContentAndOutput.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynaimcContentAndOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynaimcContentAndOutput.getParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class), false);
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class), true);

    OperationDeclaration dynamicOutputAndAttributes = getDeclaration(operations, "outputAttributesWithDynamicMetadata");
    assertOutputType(dynamicOutputAndAttributes.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicOutputAndAttributes.getOutputAttributes(), toMetadataType(AbstractOutputAttributes.class), true);
    params = dynamicOutputAndAttributes.getParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class), false);

    OperationDeclaration staticOutputOnly = getDeclaration(operations, "typeWithDeclaredSubtypesMetadata");
    assertOutputType(staticOutputOnly.getOutput(), toMetadataType(boolean.class), false);
    assertOutputType(staticOutputOnly.getOutputAttributes(), toMetadataType(void.class), false);

    OperationDeclaration staticOutputAndAttributes = getDeclaration(operations, "outputAttributesWithDeclaredSubtypesMetadata");
    assertOutputType(staticOutputAndAttributes.getOutput(), toMetadataType(Shape.class), false);
    assertOutputType(staticOutputAndAttributes.getOutputAttributes(), toMetadataType(AbstractOutputAttributes.class), false);
  }

  @Test
  public void declareStaticAndDynamicTypesInSource() {

    final List<SourceDeclaration> messageSources = declaration.getMessageSources();
    SourceDeclaration sourceDynamicAttributes = getDeclaration(messageSources, "MetadataSource");

    assertOutputType(sourceDynamicAttributes.getOutput(), TYPE_BUILDER.dictionaryType().id(Map.class.getName())
        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName())).ofValue(TYPE_BUILDER.objectType().id("java.lang.Object")
            .with(new ClassInformationAnnotation(Object.class, null)).with(new TypeAliasAnnotation(Object.class.getSimpleName())))
        .build(), true);
    assertOutputType(sourceDynamicAttributes.getOutputAttributes(), toMetadataType(StringAttributes.class), true);
    assertParameterType(getDeclaration(sourceDynamicAttributes.getParameters(), "type"), toMetadataType(String.class), false);

    SourceDeclaration sourceStaticAttributes = getDeclaration(messageSources, "MetadataSourceWithMultilevel");

    assertOutputType(sourceStaticAttributes.getOutput(), TYPE_BUILDER.dictionaryType().id(Map.class.getName())
        .ofKey(TYPE_BUILDER.stringType().id(String.class.getName())).ofValue(TYPE_BUILDER.objectType().id("java.lang.Object")
            .with(new ClassInformationAnnotation(Object.class, null)).with(new TypeAliasAnnotation(Object.class.getSimpleName())))
        .build(), true);
    assertOutputType(sourceStaticAttributes.getOutputAttributes(), toMetadataType(StringAttributes.class), false);

    List<ParameterDeclaration> locationKey = sourceStaticAttributes.getParameters();
    assertParameterType(getDeclaration(locationKey, "continent"), toMetadataType(String.class), false);
    assertParameterType(getDeclaration(locationKey, "country"), toMetadataType(String.class), false);
    assertParameterType(getDeclaration(locationKey, "city"), toMetadataType(String.class), false);

  }

  private void assertParameterIsMetadataKeyPart(ParameterDeclaration param) {
    checkIsPresent(param, MetadataKeyPartModelProperty.class);
  }

  private void assertParameterIsMetadataContent(ParameterDeclaration param) {
    checkIsPresent(param, MetadataContentModelProperty.class);
  }

  private void assertParameterType(ParameterDeclaration param, MetadataType type, boolean isDynamic) {
    assertThat(param.getType(), equalTo(type));
    assertThat(param.hasDynamicType(), is(isDynamic));
  }

  private void assertOutputType(OutputDeclaration output, MetadataType type, boolean isDynamic) {
    assertThat(output.getType(), equalTo(type));
    assertThat(output.hasDynamicType(), is(isDynamic));
  }
}
