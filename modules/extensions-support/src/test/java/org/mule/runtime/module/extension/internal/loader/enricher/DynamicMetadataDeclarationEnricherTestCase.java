/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.api.meta.model.parameter.ParameterRole.CONTENT;
import static org.mule.runtime.core.api.config.MuleManifest.getProductVersion;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.checkIsPresent;
import static org.mule.runtime.module.extension.internal.loader.enricher.EnricherTestUtils.getDeclaration;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_LOADER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.api.annotation.ClassInformationAnnotation;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OutputDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.extension.internal.loader.DefaultExtensionLoadingContext;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaModelLoaderDelegate;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.MetadataExtension;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Shape;

import java.util.List;
import java.util.Map;

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

    OperationDeclaration dynamicContent = getDeclaration(operations, "contentMetadataWithKeyId");
    assertOutputType(dynamicContent.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicContent.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicContent.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class));

    OperationDeclaration dynamicOutput = getDeclaration(operations, "outputMetadataWithKeyId");
    assertOutputType(dynamicOutput.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynamicOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynamicOutput.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class));

    OperationDeclaration dynaimcContentAndOutput = getDeclaration(operations, "contentAndOutputMetadataWithKeyId");
    assertOutputType(dynaimcContentAndOutput.getOutput(), toMetadataType(Object.class), true);
    assertOutputType(dynaimcContentAndOutput.getOutputAttributes(), toMetadataType(void.class), false);
    params = dynaimcContentAndOutput.getAllParameters();
    assertParameterType(getDeclaration(params, "type"), toMetadataType(String.class));
    assertParameterType(getDeclaration(params, "content"), toMetadataType(Object.class));

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
}
