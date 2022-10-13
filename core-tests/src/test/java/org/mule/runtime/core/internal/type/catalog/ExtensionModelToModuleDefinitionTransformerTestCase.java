/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.type.catalog;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.test.allure.AllureConstants.ReuseFeature.REUSE;
import static org.mule.test.allure.AllureConstants.ReuseFeature.ReuseStory.TYPES_CATALOG;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.metadata.api.annotation.TypeAliasAnnotation;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.message.api.el.ModuleDefinition;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.XmlDslModel;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Feature(REUSE)
@Story(TYPES_CATALOG)
@Issue("W-11706194")
public class ExtensionModelToModuleDefinitionTransformerTestCase extends AbstractMuleTestCase {

  private static final String MOCK_EXTENSION_PREFIX = "mock";
  private static final String MOCK_TYPE_ALIAS = "MockType";
  private static final String MOCK_TYPE_ID = "MockTypeId";
  private static final String MOCK_EXTENSION_NAME = "Mock Extension";

  private ObjectType mockType;
  private ExtensionModel extensionModel;
  private ExtensionModelToModuleDefinitionTransformer extensionModelToModuleDefinitionTransformer;

  @Before
  public void setUp() {
    mockType = create(JAVA).objectType()
        .id(MOCK_TYPE_ID)
        .with(new TypeAliasAnnotation(MOCK_TYPE_ALIAS))
        .build();
    extensionModel = extensionWithTypes(asSet(mockType));
    extensionModelToModuleDefinitionTransformer = new ExtensionModelToModuleDefinitionTransformer();
  }

  private static ExtensionModel extensionWithTypes(Set<ObjectType> metadataTypes) {
    ExtensionModel mockExtensionModel = mock(ExtensionModel.class);
    XmlDslModel dslModel = XmlDslModel.builder().setPrefix(MOCK_EXTENSION_PREFIX).build();
    when(mockExtensionModel.getXmlDslModel()).thenReturn(dslModel);
    when(mockExtensionModel.getName()).thenReturn(MOCK_EXTENSION_NAME);
    when(mockExtensionModel.getTypes()).thenReturn(metadataTypes);
    return mockExtensionModel;
  }

  @Test
  public void resultingModuleDefinitionHasExpectedType() {
    ModuleDefinition moduleDefinition = extensionModelToModuleDefinitionTransformer.apply(extensionModel);
    assertThat(moduleDefinition.declaredTypes(), contains(mockType));
  }

  private static <T> Set<T> asSet(T... a) {
    return stream(a).collect(toSet());
  }
}
