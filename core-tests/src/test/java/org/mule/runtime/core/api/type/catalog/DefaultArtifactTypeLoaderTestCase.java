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
import static org.mule.runtime.core.internal.type.catalog.SpecialTypesTypeLoader.VOID;
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
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.runtime.core.internal.type.catalog.DefaultArtifactTypeLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(REUSE)
@Story(TYPES_CATALOG)
public class DefaultArtifactTypeLoaderTestCase extends AbstractMuleTestCase {

  private static final String MOCK_EXTENSION_PREFIX = "mock";
  private static final String MOCK_TYPE_ALIAS = "MockType";
  private static final String MOCK_TYPE_ID = "MockTypeId";
  private static final String MOCK_EXTENSION_NAME = "Mock Extension";

  private static final String MOCK_TYPE_IDENTIFIER = MOCK_EXTENSION_PREFIX + ":" + MOCK_TYPE_ID;

  private static final String MOCK_TYPE_IDENTIFIER_FROM_ALIAS = MOCK_EXTENSION_PREFIX + ":" + MOCK_TYPE_ALIAS;

  private DefaultArtifactTypeLoader defaultArtifactTypeLoader;

  @Before
  public void setUp() throws InitialisationException {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class);

    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(MOCK_EXTENSION_PREFIX).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);

    when(mockExtensionModel.getName()).thenReturn(MOCK_EXTENSION_NAME);

    ObjectType mockType = create(JAVA).objectType()
        .id(MOCK_TYPE_ID)
        .with(new TypeAliasAnnotation(MOCK_TYPE_ALIAS))
        .build();
    when(mockExtensionModel.getTypes()).thenReturn(singleton(mockType));

    defaultArtifactTypeLoader = new DefaultArtifactTypeLoader(singleton(mockExtensionModel));
    defaultArtifactTypeLoader.initialise();
  }

  @Test
  public void hasPrimitiveTypeString() {
    assertThat(defaultArtifactTypeLoader.load(STRING).isPresent(), is(true));
  }

  @Test
  public void hasNotIncorrectType() {
    assertThat(defaultArtifactTypeLoader.load("incorrect").isPresent(), is(false));
  }

  @Test
  public void hasVoidType() {
    assertThat(defaultArtifactTypeLoader.load(VOID).isPresent(), is(true));
  }

  @Test
  public void objectTypeFromExtension() {
    assertThat(defaultArtifactTypeLoader.load(MOCK_TYPE_IDENTIFIER).isPresent(), is(true));
  }

  @Test
  public void objectTypeFromExtensionUsingTypeID() {
    assertThat(defaultArtifactTypeLoader.load(MOCK_TYPE_IDENTIFIER_FROM_ALIAS).isPresent(), is(true));
  }
}
