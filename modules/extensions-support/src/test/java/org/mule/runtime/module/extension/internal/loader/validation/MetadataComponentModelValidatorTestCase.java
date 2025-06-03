/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.validation;

import static org.mule.metadata.api.builder.BaseTypeBuilder.create;
import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockMetadataResolverFactory;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.mockParameters;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.validate;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.visitableMock;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.quality.Strictness.LENIENT;

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
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyPart;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.model.ImmutableOutputModel;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.metadata.internal.DefaultMetadataResolverFactory;
import org.mule.runtime.metadata.internal.NullMetadataResolverSupplier;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.loader.java.property.CompileTimeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionOperationDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.validation.MetadataComponentModelValidator;
import org.mule.runtime.module.extension.internal.metadata.ResolverSupplier;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

@SmallTest
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = LENIENT)
public class MetadataComponentModelValidatorTestCase extends AbstractMuleTestCase {

  public static final ClassTypeLoader loader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
  public static final String EMPTY = "";
  private static final Supplier<MockResolver> MOCK_RESOLVER_SUPPLIER =
      ResolverSupplier.of(MockResolver.class);
  private static final Supplier<SimpleOutputResolver> SIMPLE_OUTPUT_RESOLVER =
      ResolverSupplier.of(SimpleOutputResolver.class);
  public static final String PARAMETER_NAME = "parameterName";

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ExtensionModel extensionModel;

  @Mock
  private OperationModel operationModel;

  @Mock
  private SourceModel sourceModel;

  @Mock
  private SourceCallbackModel sourceCallbackModel;

  private final BaseTypeBuilder typeBuilder = BaseTypeBuilder.create(JAVA);
  private ObjectType dictionaryType;
  private ArrayType arrayType;

  private final MetadataComponentModelValidator validator = new MetadataComponentModelValidator();


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

  @BeforeEach
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
  void valid() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(MOCK_RESOLVER_SUPPLIER, emptyMap(), MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    ParameterModel param = getMockKeyPartParam("default", 1);
    setValidKeyId(param.getName());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  @Test
  void operationWithAttributeResolverButNoAttributes() {
    when(extensionModel.getSourceModels()).thenReturn(emptyList());
    mockMetadataResolverFactory(operationModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   SIMPLE_OUTPUT_RESOLVER));

    when(operationModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    when(operationModel.getOutputAttributes())
        .thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(void.class), false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("has an AttributesTypeResolver defined but it doesn't declare any attributes"));
  }

  @Test
  void operationReturnsObjectType() {
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("declares 'class java.lang.Object' as its return type. Components that return a type such"));
  }

  @Test
  void operationReturnsVoidType() {
    mockMetadataResolverFactory(operationModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Void.class), true, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("A Metadata OutputResolver named 'MockResolver' in category 'MockResolver' was defined for the Void Operation 'operation'. "
                   + "Output resolvers cannot be used on Void Operations, since they produce no output."));
  }

  @Test
  void operationReturnsDictionaryTypeWithObjectTypeValue() {
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("declares a map of 'class java.lang.Object' as its return type."));
  }

  @Test
  void operationReturnsDictionaryTypeWithPojoValue() {
    dictionaryType = typeBuilder.objectType()
        .openWith(toMetadataType(Apple.class)).build();
    when(operationModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    validate(extensionModel, validator);
  }

  @Test
  void sourceReturnsObjectType() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Components that return a type such as Object or Map (or a collection of any of those)"));
  }

  @Test
  void sourceReturnsDictionaryType() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Source 'source' declares a map of 'class java.lang.Object' as its return type. "));
  }

  @Test
  void sourceReturnsPojoType() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Apple.class), false, emptySet()));
    validate(extensionModel, validator);
  }

  @Test
  void sourceReturnsObjectTypeWithDefinedOutputResolver() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel(EMPTY, toMetadataType(Object.class), false, emptySet()));
    mockMetadataResolverFactory(sourceModel, new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                                SIMPLE_OUTPUT_RESOLVER,
                                                                                NullMetadataResolverSupplier.INSTANCE));
    validate(extensionModel, validator);
  }

  @Test
  void sourceReturnsDictionaryTypeWithDefinedOutputResolver() {
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", dictionaryType, false, emptySet()));
    mockMetadataResolverFactory(sourceModel, new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                                SIMPLE_OUTPUT_RESOLVER,
                                                                                NullMetadataResolverSupplier.INSTANCE));
    validate(extensionModel, validator);
  }

  @Test
  void sourceReturnsArrayTypeOfObjectWithoutDefinedOutputResolver() {
    arrayType = typeBuilder.arrayType().of(toMetadataType(Object.class)).build();
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", arrayType, false, emptySet()));
    assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
  }

  @Test
  void sourceReturnsArrayTypeOfDictionaryWithObjectValue() {
    arrayType = typeBuilder.arrayType().of(dictionaryType).build();
    when(sourceModel.getOutput()).thenReturn(new ImmutableOutputModel("", arrayType, false, emptySet()));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("declares a collection of "));
  }

  @Test
  void metadataResolverWithDifferentCategories() {
    setValidKeyId(EMPTY);
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(ResolverSupplier.of(DifferentCategoryResolver.class),
                                                                   emptyMap(),
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NullMetadataResolverSupplier.INSTANCE));

    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("specifies metadata resolvers that doesn't belong to the same category"));
  }

  @Test
  void metadataResolverWithEmptyCategoryName() {
    setValidKeyId(EMPTY);
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(ResolverSupplier.of(EmptyCategoryName.class),
                                                                   emptyMap(),
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("specifies a metadata resolver 'EmptyCategoryName' which has an empty category name"));
  }

  @Test
  void metadataResolverWithEmptyResolverName() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE,
                                                                   emptyMap(),
                                                                   ResolverSupplier.of(EmptyResolverName.class),
                                                                   NullMetadataResolverSupplier.INSTANCE));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("specifies a metadata resolver 'EmptyResolverName' which has an empty resolver name"));
  }

  @Test
  void metadataResolverWithRepeatedResolverName() {
    Map<String, Supplier<? extends InputTypeResolver>> inputResolvers = new HashMap<>();
    ParameterModel parameterModel = mock(ParameterModel.class);
    when(parameterModel.getName()).thenReturn(PARAMETER_NAME);
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    mockParameters(sourceModel, parameterModel);
    inputResolvers.put(PARAMETER_NAME, ResolverSupplier.of(SimpleInputResolver.class));

    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE,
                                                                   inputResolvers,
                                                                   SIMPLE_OUTPUT_RESOLVER,
                                                                   NullMetadataResolverSupplier.INSTANCE));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Resolver names should be unique for a given category"));
  }

  @Test
  void metadataKeyMissingDefaultValues() {
    setInvalidKeyId();
    ParameterModel param1 = getMockKeyPartParam(null, 1);
    ParameterModel param2 = getMockKeyPartParam("SomeValue", 2);
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("defines '1' MetadataKeyPart with default values, but the type contains '2'"));
  }

  @Test
  void metadataKeyWithValidDefaultValues() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));

    ParameterModel param1 = getMockKeyPartParam("Value", 1);
    ParameterModel param2 = getMockKeyPartParam("SomeValue", 2);
    setInvalidKeyId();
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    validate(extensionModel, validator);
  }

  @Test
  void metadataKeyWithoutDefaultValues() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));

    ParameterModel param1 = getMockKeyPartParam(null, 1);
    ParameterModel param2 = getMockKeyPartParam(null, 2);
    setInvalidKeyId();
    when(sourceModel.getAllParameterModels()).thenReturn(asList(param1, param2));
    validate(extensionModel, validator);
  }

  @Test
  void metadataKeyIdWithoutTypeResolver() {
    ParameterModel param = getMockKeyPartParam("Value", 1);
    setValidKeyId(param.getName());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    var thrown = assertThrows(IllegalModelDefinitionException.class, () -> validate(extensionModel, validator));
    assertThat(thrown.getMessage(),
               containsString("Source 'source' defines a MetadataKeyId parameter but neither an Output nor Type resolver that makes use of it was defined"));
  }

  @Test
  void metadataKeyIdWithoutTypeResolverOnRuntimeShouldNotFail() {
    setCompileTime(false);
    ParameterModel param = getMockKeyPartParam("Value", 1);
    setValidKeyId(param.getName());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  @Test
  void noMetadataKey() {
    ParameterModel param = mock(ParameterModel.class);
    when(sourceModel.getModelProperty(MetadataKeyIdModelProperty.class)).thenReturn(empty());
    when(sourceModel.getAllParameterModels()).thenReturn(singletonList(param));
    validate(extensionModel, validator);
  }

  @Test
  void stringMetadataKeyWithDefaultValue() {
    mockMetadataResolverFactory(sourceModel,
                                new DefaultMetadataResolverFactory(NullMetadataResolverSupplier.INSTANCE, emptyMap(),
                                                                   MOCK_RESOLVER_SUPPLIER,
                                                                   NullMetadataResolverSupplier.INSTANCE));

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

  private void setCompileTime(boolean compileTime) {
    when(extensionModel.getModelProperty(CompileTimeModelProperty.class))
        .thenReturn(ofNullable(compileTime ? new CompileTimeModelProperty() : null));
  }
}
