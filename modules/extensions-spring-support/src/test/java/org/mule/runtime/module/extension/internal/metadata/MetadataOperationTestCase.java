/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.metadata.PartAwareMetadataKeyBuilder.newKey;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.TIRES;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.EUROPE;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.AGE_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.BRAND_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.NAME_VALUE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;
import org.mule.functional.listener.Callback;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.DefaultMetadataCache;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.tck.junit4.matcher.MetadataKeyMatcher;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.animals.Animal;
import org.mule.test.metadata.extension.model.animals.AnimalClade;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.animals.SwordFish;
import org.mule.test.metadata.extension.model.attribute.AbstractOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Shape;
import org.mule.test.metadata.extension.resolver.TestThreadContextClassLoaderResolver;
import org.mule.test.module.extension.internal.util.ExtensionsTestUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

public class MetadataOperationTestCase extends MetadataExtensionFunctionalTestCase {

  private static final String MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA = "messageAttributesPersonTypeMetadata";
  private static final String MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA = "messageAttributesVoidTypeMetadata";
  private static final String PAGED_OPERATION_METADATA = "pagedOperationMetadata";
  private static final String CONFIG = "config";
  private static final String ALTERNATIVE_CONFIG = "alternative-config";

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getMetadataKeysWithKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(componentId);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));
  }

  @Test
  public void getMetadataKeysWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);
    final MetadataResult<MetadataKeysContainer> metadataKeys = metadataService.getMetadataKeys(componentId);
    assertSuccessResult(metadataKeys);
    final Set<MetadataKey> keys = getKeysFromContainer(metadataKeys.get());
    assertThat(keys.size(), is(1));
    assertThat(keys.iterator().next(), instanceOf(NullMetadataKey.class));
  }

  @Test
  public void getMultilevelKeys() throws Exception {
    componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(componentId);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasSize(2));

    assertThat(metadataKeys, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(metadataKeys, hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT)));
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInMetadataResolver() throws Exception {
    componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
    MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataResult =
        metadataService.getOperationMetadata(componentId, key);
    assertSuccessResult(metadataResult);
  }

  @Test
  public void dynamicOperationMetadata() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputAndMultipleInputWithKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "firstPerson"), personType, true);
    assertExpectedType(getParameter(typedModel, "otherPerson"), personType, true);
  }


  @Test
  public void dynamicOutputWithoutContentParam() throws Exception {
    // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyId
    componentId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  @Test
  public void dynamicContentWithoutOutput() throws Exception {
    // Resolver for content and output type, no return type, resolves only @Content, with key and KeyId
    componentId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, void.class, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void operationOutputWithoutKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);

  }

  @Test
  public void contentAndOutputMetadataWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void contentMetadataWithoutKeysWithKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, void.class, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputMetadataWithoutKeysWithKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  //
  @Test
  public void messageAttributesVoidTypeMetadata() throws Exception {
    componentId = new ProcessorId(MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, ExtensionsTestUtils.TYPE_BUILDER.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, TARGET_PARAMETER_NAME), String.class);
  }

  @Test
  public void messageAttributesStringTypeMetadata() throws Exception {
    componentId = new ProcessorId(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, StringAttributes.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  @Test
  public void attributesDynamicPersonTypeMetadata() throws Exception {
    componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    MetadataType type = typedModel.getOutputAttributes().getType();
    assertThat(type, is(instanceOf(DictionaryType.class)));
    DictionaryType dictionary = (DictionaryType) type;
    assertThat(dictionary.getKeyType(), is(instanceOf(DateType.class)));
    assertThat(dictionary.getValueType(), is(instanceOf(StringType.class)));
  }

  @Test
  public void attributesUnionTypeMetadata() throws Exception {
    componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, Shape.class, AbstractOutputAttributes.class);

  }

  @Test
  public void getContentMetadataWithKey() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void getOutputMetadataWithKey() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);
  }

  @Test
  public void dynamicContentWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void dynamicOutputWithoutKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);
  }

  @Test
  public void dynamicOutputAndContentWithCache() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    MetadataType outputType = typedModel.getOutput().getType();
    MetadataType contentType = getParameter(typedModel, "content").getType();
    assertThat(contentType, is(equalTo(outputType)));
  }

  @Test
  public void resolverContentWithContextClassLoader() throws Exception {
    resolveTestWithContextClassLoader(RESOLVER_CONTENT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void resolverOutputWithContextClassLoader() throws Exception {
    resolveTestWithContextClassLoader(RESOLVER_OUTPUT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void shouldInheritOperationResolvers() throws Exception {
    componentId = new ProcessorId(SHOULD_INHERIT_OPERATION_RESOLVERS, FIRST_PROCESSOR_INDEX);
    assertInheritedResolvers();
  }

  @Test
  public void shouldInheritExtensionResolvers() throws Exception {
    componentId = new ProcessorId(SHOULD_INHERIT_EXTENSION_RESOLVERS, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertInheritedResolvers(metadataDescriptor.getModel());
  }

  @Test
  public void shouldInheritOperationParentResolvers() throws Exception {
    componentId = new ProcessorId(SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertInheritedResolvers(metadataDescriptor.getModel());
  }

  @Test
  public void multipleCaches() throws Exception {
    // using config
    componentId = new ProcessorId(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
    getSuccessComponentDynamicMetadata();
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
    getSuccessComponentDynamicMetadata();

    // using alternative-config
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG, FIRST_PROCESSOR_INDEX);
    getSuccessComponentDynamicMetadata();

    MuleMetadataService metadataManager = (MuleMetadataService) muleContext.getRegistry().lookupObject(MetadataService.class);
    Map<String, ? extends MetadataCache> caches = metadataManager.getMetadataCaches();

    assertThat(caches.keySet(), hasSize(2));
    assertThat(caches.keySet(), hasItems(CONFIG, ALTERNATIVE_CONFIG));
  }

  @Test
  public void pagedOperationMetadataTestCase() throws Exception {
    componentId = new ProcessorId(PAGED_OPERATION_METADATA, FIRST_PROCESSOR_INDEX);
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "animal"), Animal.class);
  }

  @Test
  public void elementsAreStoredInCaches() throws Exception {
    // using config
    componentId = new ProcessorId(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
    metadataService.getMetadataKeys(componentId);
    getSuccessComponentDynamicMetadata();

    // using alternative-config
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG, FIRST_PROCESSOR_INDEX);
    getSuccessComponentDynamicMetadata();

    MuleMetadataService metadataManager = (MuleMetadataService) muleContext.getRegistry().lookupObject(MetadataService.class);
    DefaultMetadataCache configCache = (DefaultMetadataCache) metadataManager.getMetadataCaches().get(CONFIG);

    assertThat(configCache.asMap().keySet(), containsInAnyOrder(AGE, NAME, BRAND));
    assertThat(configCache.get(AGE).get(), is(AGE_VALUE));
    assertThat(configCache.get(NAME).get(), is(NAME_VALUE));
    assertThat(configCache.get(BRAND).get(), is(BRAND_VALUE));

    DefaultMetadataCache alternativeConfigCache =
        (DefaultMetadataCache) metadataManager.getMetadataCaches().get(ALTERNATIVE_CONFIG);
    assertThat(alternativeConfigCache.asMap().keySet(), hasItems(BRAND));
    assertThat(alternativeConfigCache.get(BRAND).get(), is(BRAND_VALUE));
  }

  @Test
  public void componentWithStaticInputs() throws IOException {
    componentId = new ProcessorId(TYPE_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "plainShape"), Shape.class);
    assertExpectedType(getParameter(typedModel, "animal"), Animal.class);
    assertExpectedType(getParameter(typedModel, "rectangleSubtype"), Rectangle.class);
  }

  @Test
  public void retrieveKeysFromBooleanMetadataKey() {
    componentId = new ProcessorId(BOOLEAN_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(componentId);
    assertSuccessResult(result);
    String booleanMetadataResolver = "BooleanMetadataResolver";
    assertThat(result.get().getCategories(), contains(booleanMetadataResolver));
    Set<MetadataKey> metadataKeys = result.get().getKeys(booleanMetadataResolver).get();
    assertThat(metadataKeys, hasItems(metadataKeyWithId("FALSE"), metadataKeyWithId("TRUE")));
  }

  @Test
  public void booleanMetadataKey() throws IOException {
    componentId = new ProcessorId(BOOLEAN_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result =
        metadataService.getOperationMetadata(componentId, newKey("true").build());
    assertSuccessResult(result);
    assertExpectedType(getParameter(result.get().getModel(), "content"), TYPE_LOADER.load(SwordFish.class), true);
  }

  @Test
  public void retrieveKeysFromEnumMetadataKey() {
    componentId = new ProcessorId(ENUM_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(componentId);
    assertSuccessResult(result);
    String enumMetadataResolver = "EnumMetadataResolver";
    assertThat(result.get().getCategories(), contains(enumMetadataResolver));

    Set<MetadataKey> metadataKeys = result.get().getKeys(enumMetadataResolver).get();
    MetadataKeyMatcher[] metadataKeyMatchers = Stream.of(AnimalClade.values())
        .map(Object::toString)
        .map(key -> metadataKeyWithId(key))
        .toArray(MetadataKeyMatcher[]::new);

    assertThat(metadataKeys, hasItems(metadataKeyMatchers));
  }

  @Test
  public void enumMetadataKey() throws IOException {
    componentId = new ProcessorId(ENUM_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(newKey("MAMMAL").build());
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "content"), TYPE_LOADER.load(Bear.class), true);
  }

  @Test
  public void metadataKeyDefaultValue() throws Exception {
    componentId = new ProcessorId(METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(componentId);
    assertSuccessResult(result);
    MetadataType type = result.get().getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(2));
    objectType.getFields().forEach(f -> assertThat(f.getKey().getName().getLocalPart(), isOneOf(TIRES, BRAND)));
  }

  @Test
  public void defaultValueMultilevelMetadataKey() throws Exception {
    componentId = new ProcessorId(MULTILEVEL_METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataDescriptor =
        metadataService.getOperationMetadata(componentId);
    MetadataType type = getParameter(metadataDescriptor.get().getModel(), "content").getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) type).getFields().size(), is(3));
    List<String> expectedKeys = Arrays.asList("CONTINENT", "COUNTRY", "CITY");
    Optional<ObjectFieldType> missingKey = ((ObjectType) type).getFields().stream()
        .filter(f -> !expectedKeys.contains(f.getKey().getName().getLocalPart()))
        .findFirst();
    assertThat(missingKey.isPresent(), is(false));
  }

  @Test
  public void defaultValueMetadataKey() throws Exception {
    componentId = new ProcessorId(METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(componentId);
    assertSuccessResult(result);
    ComponentMetadataDescriptor descriptor = result.get();
    MetadataType type = descriptor.getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) type).getFields(), hasSize(2));
  }


  @Test
  public void operationWhichReturnsListOfMessages() throws Exception {
    componentId = new ProcessorId("listOfMessages", FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(componentId);
    assertSuccessResult(result);
    ComponentMetadataDescriptor descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) param).getType(), TYPE_LOADER.load(String.class),
                      TYPE_LOADER.load(StringAttributes.class));
  }

  @Test
  public void operationWhichReturnsDynamicListOfMessages() throws Exception {
    componentId = new ProcessorId("dynamicListOfMessages", FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(componentId);
    assertSuccessResult(result);
    ComponentMetadataDescriptor descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) param).getType(), personType, TYPE_BUILDER.voidType().build());
  }

  /**
   * Test template that sets an "invalid" classloader in TCCL different from the one that was used to register the extension and
   * asserts that, it sets back the original classloader to TCCL. Done in this way due to it is not possible to change extension
   * model classloader property once it is registered.
   */
  private void resolveTestWithContextClassLoader(String flowName, Callback<MetadataOperationTestCase> doAction)
      throws Exception {
    componentId = new ProcessorId(flowName, FIRST_PROCESSOR_INDEX);
    TestThreadContextClassLoaderResolver.reset();
    final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
    withContextClassLoader(mock(ClassLoader.class), () -> {
      doAction.execute(MetadataOperationTestCase.this);
      assertThat(TestThreadContextClassLoaderResolver.getCurrentState(), is(sameInstance(originalClassLoader)));
    });
  }

  private void assertInheritedResolvers() throws IOException {
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertInheritedResolvers(typedModel);
  }

  private void assertInheritedResolvers(ComponentModel typedModel) throws IOException {
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);
  }


  private final ParameterModel getParameter(ComponentModel model, String parameterName) {
    return model.getAllParameterModels().stream()
        .filter(p -> p.getName().equals(parameterName)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Parameter not found"));
  }
}
