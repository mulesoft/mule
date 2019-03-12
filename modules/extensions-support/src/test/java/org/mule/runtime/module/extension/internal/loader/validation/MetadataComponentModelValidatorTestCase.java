/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.util.ExtensionModelTestUtils.visitableMock;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.AttributesTypeResolver;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.core.internal.metadata.DefaultMetadataResolverFactory;
import org.mule.runtime.core.internal.metadata.NullMetadataResolverSupplier;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.metadata.NullMetadataResolver;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.metadata.ResolverSupplier;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class MetadataComponentModelValidatorTestCase extends AbstractMuleTestCase {

  public static final ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  public static final String EMPTY = "";
  private static final Supplier<NullMetadataResolver> NULL_RESOLVER_SUPPLIER =
      new NullMetadataResolverSupplier();
  private static final Supplier<MockResolver> MOCK_RESOLVER_SUPPLIER =
      ResolverSupplier.of(MockResolver.class);
  private static final Supplier<SimpleOutputResolver> SIMPLE_OUTPUT_RESOLVER =
      ResolverSupplier.of(SimpleOutputResolver.class);
  public static final String PARAMETER_NAME = "parameterName";

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private SourceCallbackModel sourceCallbackModel;

  private BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  private ObjectType dictionaryType;
  private ArrayType arrayType;

  private MetadataComponentModelValidator validator = new MetadataComponentModelValidator();


  public static class SimpleOutputResolver implements OutputTypeResolver<String>, AttributesTypeResolver<String> {

    @Override
    public String getResolverName() {
      return "SimpleOutputResolver";
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, String key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }

    @Override
    public MetadataType getAttributesType(MetadataContext context, String key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }

    @Override
    public String getCategoryName() {
      return "SimpleOutputResolver";
    }
  }

  public static class SimpleInputResolver implements InputTypeResolver<String> {

    @Override
    public MetadataType getInputMetadata(MetadataContext context, String key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }

    @Override
    public String getCategoryName() {
      return "SimpleOutputResolver";
    }

    @Override
    public String getResolverName() {
      return "SimpleOutputResolver";
    }
  }


  public static class DifferentCategoryResolver implements TypeKeysResolver {

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
      return emptySet();
    }

    @Override
    public String getCategoryName() {
      return "NotSimpleOutputResolver";
    }

    @Override
    public String getResolverName() {
      return "DifferentCategoryResolver";
    }
  }


  public static class EmptyCategoryName implements TypeKeysResolver {

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
      return emptySet();
    }

    @Override
    public String getCategoryName() {
      return null;
    }

    @Override
    public String getResolverName() {
      return "EmptyCategoryName";
    }
  }

  public static class EmptyResolverName implements TypeKeysResolver, OutputTypeResolver {

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
      return emptySet();
    }

    @Override
    public String getCategoryName() {
      return "SimpleOutputResolver";
    }

    @Override
    public String getResolverName() {
      return null;
    }

    @Override
    public MetadataType getOutputType(MetadataContext context, Object key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }
  }

  @Before
  public void before() {
    when(extensionModel.getOperationModels()).thenReturn(asList(operationModel));
    when(extensionModel.getSourceModels()).thenReturn(asList(sourceModel));
    ExtensionTypeDescriptorModelProperty descriptorModelProperty = mock(ExtensionTypeDescriptorModelProperty.class);
    when(extensionModel.getModelProperty(ExtensionTypeDescriptorModelProperty.class)).thenReturn(of(descriptorModelProperty));
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class)).thenReturn(of(new CompileTimeModelProperty()));
    Type extensionType = mock(Type.class);
    when(descriptorModelProperty.getType()).thenReturn(extensionType);
    when(extensionType.getDeclaringClass()).thenReturn(of(this.getClass()));

    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(String.class), false, emptySet()));
    when(operationModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel(StringUtils.EMPTY, create(JAVA).voidType().build(), false, emptySet()));
    when(operationModel.getName()).thenReturn("operation");
    mockMetadataResolverFactory(operationModel, null);

    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(String.class), false, emptySet()));
    when(sourceModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel(StringUtils.EMPTY, create(JAVA).voidType().build(), false, emptySet()));

    when(sourceModel.getName()).thenReturn("source");
    when(sourceModel.getSuccessCallback()).thenReturn(of(sourceCallbackModel));
    when(sourceModel.getErrorCallback()).thenReturn(of(sourceCallbackModel));
    mockMetadataResolverFactory(sourceModel, null);

    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(sourceModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)).thenReturn(empty());
    when(operationModel.getModelProperty(ExtensionOperationDescriptorModelProperty.class)).thenReturn(empty());

    visitableMock(operationModel, sourceModel);

    dictionaryType = typeBuilder.objectType()
        .openWith(toMetadataType(Object.class))
        .build();
  }

  @Test
  public void valid() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(MOCK_RESOLVER_SUPPLIER, emptyMap(), MOCK_RESOLVER_SUPPLIER,
                                                                   NULL_RESOLVER_SUPPLIER));
    ParameterModel param = getMockKeyPartParam("default", 1);
    setValidKeyId(param.getName());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  @Test
  public void operationWithAttributeResolverButNoAttributes() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("has an AttributesTypeResolver defined but it doesn't declare any attributes");

    when(extensionModel.getSourceModels()).thenReturn(emptyList());
    mockMetadataResolverFactory(operationModel, new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(),
                                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                                   SIMPLE_OUTPUT_RESOLVER));

    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    when(operationModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(void.class), false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void operationReturnsObjectType() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("declares 'class java.lang.Object' as its return type. Components that return a type such");
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void operationReturnsVoidType() {
    exception.expect(IllegalModelDefinitionException.class);
    exception
        .expectMessage("A Metadata OutputResolver named 'MockResolver' in category 'MockResolver' was defined for the Void Operation 'operation'. "
            + "Output resolvers cannot be used on Void Operations, since they produce no output.");

    mockMetadataResolverFactory(operationModel,
                                new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(), MOCK_RESOLVER_SUPPLIER,
                                                                   NULL_RESOLVER_SUPPLIER));
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Void.class), true, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void operationReturnsDictionaryTypeWithObjectTypeValue() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("declares a map of 'class java.lang.Object' as its return type.");

    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void operationReturnsDictionaryTypeWithPojoValue() {
    dictionaryType = typeBuilder.objectType()
        .openWith(toMetadataType(Apple.class)).build();
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(), MOCK_RESOLVER_SUPPLIER,
                                                                   NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsObjectType() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Components that return a type such as Object or Map (or a collection of any of those)");
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsDictionaryType() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Source 'source' declares a map of 'class java.lang.Object' as its return type. "
        + "Components that return a type such as Object or Map ");
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsPojoType() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(), MOCK_RESOLVER_SUPPLIER,
                                                                   NULL_RESOLVER_SUPPLIER));
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Apple.class), false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsObjectTypeWithDefinedOutputResolver() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    mockMetadataResolverFactory(sourceModel, new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(),
                                                                                SIMPLE_OUTPUT_RESOLVER,
                                                                                NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsDictionaryTypeWithDefinedOutputResolver() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    mockMetadataResolverFactory(sourceModel, new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER, emptyMap(),
                                                                                SIMPLE_OUTPUT_RESOLVER,
                                                                                NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsArrayTypeOfObjectWithoutDefinedOutputResolver() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("");
    arrayType = typeBuilder.arrayType().of(toMetadataType(Object.class)).build();
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", arrayType, false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void sourceReturnsArrayTypeOfDictionaryWithObjectValue() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("declares a collection of ");

    arrayType = typeBuilder.arrayType().of(dictionaryType).build();
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", arrayType, false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataResolverWithDifferentCategories() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("specifies metadata resolvers that doesn't belong to the same category");
    setValidKeyId(EMPTY);
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(ResolverSupplier.of(DifferentCategoryResolver.class),
                                                                   emptyMap(),
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataResolverWithEmptyCategoryName() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("specifies a metadata resolver 'EmptyCategoryName' which has an empty category name");
    setValidKeyId(EMPTY);
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(ResolverSupplier.of(EmptyCategoryName.class),
                                                                   emptyMap(),
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataResolverWithEmptyResolverName() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("specifies a metadata resolver 'EmptyResolverName' which has an empty resolver name");

    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER,
                                                                   emptyMap(),
                                                                   ResolverSupplier.of(EmptyResolverName.class),
                                                                   NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataResolverWithRepeatedResolverName() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("Resolver names should be unique for a given category");
    Map<String, Supplier<? extends InputTypeResolver>> inputResolvers = new HashedMap();
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getName()).thenReturn(PARAMETER_NAME);
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    mockParameters(sourceModel, parameterModel);
    inputResolvers.put(PARAMETER_NAME, ResolverSupplier.of(SimpleInputResolver.class));

    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NULL_RESOLVER_SUPPLIER,
                                                                   inputResolvers,
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NULL_RESOLVER_SUPPLIER));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataKeyMissingDefaultValues() {
    exception.expect(IllegalModelDefinitionException.class);
    exception.expectMessage("defines '1' MetadataKeyPart with default values, but the type contains '2'");

    setInvalidKeyId();
    ParameterModel param1 = getMockKeyPartParam(null, 1);
    ParameterModel param2 = getMockKeyPartParam("SomeValue", 2);
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataKeyWithValidDefaultValues() {
    ParameterModel param1 = getMockKeyPartParam("Value", 1);
    ParameterModel param2 = getMockKeyPartParam("SomeValue", 2);
    setInvalidKeyId();
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    validate(extensionModel, validator);
  }

  @Test
  public void metadataKeyWithoutDefaultValues() {
    ParameterModel param1 = getMockKeyPartParam(null, 1);
    ParameterModel param2 = getMockKeyPartParam(null, 2);
    setInvalidKeyId();
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    validate(extensionModel, validator);
  }

  @Test
  public void noMetadataKey() {
    ParameterModel param = mock(ParameterModel.class);
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  @Test
  public void stringMetadataKeyWithDefaultValue() {
    ParameterModel param = getMockKeyPartParam("default", 1);
    setValidKeyId(param.getName());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  public class InvalidMetadataKeyIdPojo {

    @Optional(defaultValue = "SomeValue")
    @MetadataKeyPart(order = 1)
    @Parameter
    private String partOne;

    @MetadataKeyPart(order = 2)
    @Parameter
    private String partTwo;
  }


  public class ValidMetadataKeyIdPojo {

    @Optional(defaultValue = "SomeValue")
    @MetadataKeyPart(order = 1)
    @Parameter
    private String partOne;

    @Optional(defaultValue = "AnotherValue")
    @MetadataKeyPart(order = 2)
    @Parameter
    private String partTwo;
  }

  private ParameterModel getMockKeyPartParam(Object defaultValue, int order) {
    ParameterModel param = mock(ParameterModel.class);
    when(param.getName()).thenReturn("mockParam");
    when(param.getDefaultValue()).thenReturn(defaultValue);
    when(param.getModelProperty(MetadataKeyPartModelProperty.class)).thenReturn(of(new MetadataKeyPartModelProperty(order)));
    return param;
  }

  public static class MockResolver implements TypeKeysResolver, OutputTypeResolver {

    @Override
    public MetadataType getOutputType(MetadataContext context, Object key)
        throws MetadataResolvingException, ConnectionException {
      return null;
    }

    @Override
    public String getCategoryName() {
      return "MockResolver";
    }

    @Override
    public String getResolverName() {
      return "MockResolver";
    }

    @Override
    public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
      return null;
    }
  }

  private void setValidKeyId(String paramName) {
    MetadataKeyIdModelProperty keyIdModelProperty = new MetadataKeyIdModelProperty(loader.load(String.class), paramName);
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(of(keyIdModelProperty));
  }

  private void setInvalidKeyId() {
    MetadataKeyIdModelProperty keyIdModelProperty =
        new MetadataKeyIdModelProperty(loader.load(InvalidMetadataKeyIdPojo.class), "groupKeyParam");
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(of(keyIdModelProperty));
  }
}
