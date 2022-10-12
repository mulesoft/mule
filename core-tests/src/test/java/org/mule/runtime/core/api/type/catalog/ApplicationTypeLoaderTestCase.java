/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.type.catalog;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.metadata.catalog.api.PrimitiveTypesTypeLoader.STRING;
import static org.mule.runtime.core.api.type.catalog.SpecialTypesTypeLoader.VOID;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.TYPES_CATALOG;

import static java.util.Collections.singleton;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(REUSE)
@Story(TYPES_CATALOG)
public class ApplicationTypeLoaderTestCase extends AbstractMuleTestCase {

  private static final String MOCK_EXTENSION_PREFIX = "mock";
  private static final String MOCK_TYPE_ALIAS = "MockType";
  private static final String MOCK_TYPE_ID = "MockTypeId";
  private static final String MOCK_EXTENSION_NAME = "Mock Extension";

  private ApplicationTypeLoader applicationTypeLoader;

  @Before
  public void setUp() {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class);

    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(MOCK_EXTENSION_PREFIX).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);

    when(mockExtensionModel.getName()).thenReturn(MOCK_EXTENSION_NAME);

    ObjectType mockType = create(JAVA).objectType()
        .id(MOCK_TYPE_ID)
        .with(new TypeAliasAnnotation(MOCK_TYPE_ALIAS))
        .build();
    when(mockExtensionModel.getTypes()).thenReturn(singleton(mockType));

    ExpressionLanguageMetadataService mockMetadataService = mock(ExpressionLanguageMetadataService.class);
    when(mockMetadataService.evaluateTypeExpression(eq(MOCK_TYPE_ID), any())).thenReturn(mockType);
    applicationTypeLoader = new ApplicationTypeLoader(singleton(mockExtensionModel), mockMetadataService);
  }

  @Test
  public void hasPrimitiveTypeString() {
    assertThat(applicationTypeLoader.load(STRING).isPresent(), is(true));
  }

  @Test
  public void hasNotIncorrectType() {
    assertThat(applicationTypeLoader.load("incorrect").isPresent(), is(false));
  }

  @Test
  public void hasVoidType() {
    assertThat(applicationTypeLoader.load(VOID).isPresent(), is(true));
  }

  @Test
  @Issue("W-11706194")
  @Description("The type expressions are resolved by the ExpressionLanguageMetadataService")
  public void typeResolvedByExpressionLanguageMetadataService() {
    assertThat(applicationTypeLoader.load(MOCK_TYPE_ID).isPresent(), is(true));
  }
}
