/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.when;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataAttributesResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.introspection.ExtensionModel;
import org.mule.runtime.extension.api.introspection.ImmutableOutputModel;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.introspection.operation.RuntimeOperationModel;
import org.mule.runtime.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MetadataComponentModelValidatorTestCase extends AbstractMuleTestCase {

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private DictionaryType dictionaryType;

  @Mock
  private RuntimeOperationModel operationModel;

  @Mock
  private RuntimeSourceModel sourceModel;

  private MetadataComponentModelValidator validator = new MetadataComponentModelValidator();

  public static class SimpleOutputResolver implements MetadataOutputResolver<String>, MetadataAttributesResolver<String> {

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, String key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }

    @Override
    public MetadataType getAttributesMetadata(MetadataContext context, String key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }
  }

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));

    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(String.class), false, emptySet()));
    when(operationModel.getName()).thenReturn("operation");
    when(operationModel.getMetadataResolverFactory()).thenReturn(new NullMetadataResolverFactory());

    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(String.class), false, emptySet()));
    when(sourceModel.getName()).thenReturn("source");
    when(sourceModel.getMetadataResolverFactory()).thenReturn(new NullMetadataResolverFactory());
  }

  @Test
  public void valid() {
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationReturnsObjectType() {
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(Object.class), false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void operationReturnsDictionaryTypeWithObjectTypeValue() {
    when(dictionaryType.getValueType()).thenReturn(toMetadataType(Object.class));
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test
  public void operationReturnsDictionaryTypeWithPojoValue() {
    when(dictionaryType.getValueType()).thenReturn(toMetadataType(Apple.class));
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void sourceReturnsObjectType() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(Object.class), false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test(expected = IllegalModelDefinitionException.class)
  public void sourceReturnsDictionaryType() {
    when(dictionaryType.getValueType()).thenReturn(toMetadataType(Object.class));
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test
  public void sourceReturnsPojoType() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(Apple.class), false, emptySet()));
    validator.validate(extensionModel);
  }

  @Test
  public void sourceReturnsObjectTypeWithDefinedOutputResolver() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", toMetadataType(Object.class), false, emptySet()));
    when(sourceModel.getMetadataResolverFactory())
        .thenReturn(new DefaultMetadataResolverFactory(NullMetadataResolver.class, NullMetadataResolver.class,
                                                       SimpleOutputResolver.class, SimpleOutputResolver.class));
    validator.validate(extensionModel);
  }

  @Test
  public void sourceReturnsDictionaryTypeWithDefinedOutputResolver() {
    when(dictionaryType.getValueType()).thenReturn(toMetadataType(Object.class));
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    when(sourceModel.getMetadataResolverFactory())
        .thenReturn(new DefaultMetadataResolverFactory(NullMetadataResolver.class, NullMetadataResolver.class,
                                                       SimpleOutputResolver.class, SimpleOutputResolver.class));
    validator.validate(extensionModel);
  }
}
