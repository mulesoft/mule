/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.metadata;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.extension.api.ExtensionConstants.TARGET_PARAMETER_NAME;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.getId;
import static org.mule.tck.junit4.matcher.MetadataKeyMatcher.metadataKeyWithId;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.TIRES;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.EUROPE;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.TYPE_BUILDER;
import static org.mule.test.module.extension.internal.util.ExtensionsTestUtils.assertMessageType;

import org.mule.functional.listener.Callback;
import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.NullType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
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
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Test;

public class MetadataOperationTestCase extends AbstractMetadataOperationTestCase {

  private static final String MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA = "messageAttributesPersonTypeMetadata";
  private static final String MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA = "messageAttributesVoidTypeMetadata";
  private static final String PAGED_OPERATION_METADATA = "pagedOperationMetadata";
  private static final String PAGED_OPERATION_METADATA_RESULT = "pagedOperationMetadataResult";
  private static final String PAGED_OPERATION_METADATA_RESULT_WITH_ATTRIBUTES =
      "pagedOperationMetadataResultWithAttributesResolver";

  public MetadataOperationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
  }

  @Override
  protected String getConfigFile() {
    return METADATA_TEST;
  }

  @Test
  public void getMetadataKeysWithKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));
  }

  @Test
  public void getMetadataKeysWithoutKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeys = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeys);
    final Set<MetadataKey> keys = getKeysFromContainer(metadataKeys.get());
    assertThat(keys.size(), is(1));
    assertThat(keys.iterator().next(), instanceOf(NullMetadataKey.class));
  }

  @Test
  public void getMultilevelKeys() throws Exception {
    location = Location.builder().globalName(SIMPLE_MULTILEVEL_KEY_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(location);
    assertSuccessResult(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasSize(2));

    assertThat(metadataKeys, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(metadataKeys, hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT)));
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInMetadataResolver() throws Exception {
    location = Location.builder().globalName(SIMPLE_MULTILEVEL_KEY_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    MetadataKey key = LOCATION_MULTILEVEL_KEY;
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataResult =
        metadataService.getOperationMetadata(location, key);
    assertSuccessResult(metadataResult);
    assertResolvedKey(metadataResult, LOCATION_MULTILEVEL_KEY);
  }

  @Test
  public void dynamicOperationMetadata() throws Exception {
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputAndMultipleInputWithKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "firstPerson"), personType, true);
    assertExpectedType(getParameter(typedModel, "otherPerson"), personType, true);
  }


  @Test
  public void dynamicOutputWithoutContentParam() throws Exception {
    // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyId
    location = Location.builder().globalName(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  @Test
  public void dynamicContentWithoutOutput() throws Exception {
    // Resolver for content and output type, no return type, resolves only @Content, with key and KeyId
    location = Location.builder().globalName(CONTENT_ONLY_IGNORES_OUTPUT).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, void.class, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void operationOutputWithoutKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEY_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);

  }

  @Test
  public void contentAndOutputMetadataWithoutKeyId() throws Exception {
    location =
        Location.builder().globalName(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void contentMetadataWithoutKeysWithKeyId() throws Exception {
    location =
        Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, void.class, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void outputMetadataWithoutKeysWithKeyId() throws Exception {
    location =
        Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  @Test
  public void messageAttributesVoidTypeMetadata() throws Exception {
    location = Location.builder().globalName(MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, ExtensionsTestUtils.TYPE_BUILDER.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, TARGET_PARAMETER_NAME), String.class);
  }

  @Test
  public void messageAttributesStringTypeMetadata() throws Exception {
    location = Location.builder().globalName(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, StringAttributes.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);

  }

  @Test
  public void attributesDynamicPersonTypeMetadata() throws Exception {
    location = Location.builder().globalName(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType type = typedModel.getOutputAttributes().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType dictionary = (ObjectType) type;
    assertThat(dictionary.getOpenRestriction().get(), is(instanceOf(StringType.class)));
  }

  @Test
  public void attributesUnionTypeMetadata() throws Exception {
    location = Location.builder().globalName(OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA).addProcessorsPart()
        .addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, Shape.class, AbstractOutputAttributes.class);

  }

  @Test
  public void getContentMetadataWithKey() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void getContentMetadataWithoutRequiredKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void getOutputMetadataWithKey() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "type"), String.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);
  }

  @Test
  public void dynamicContentWithoutKeyId() throws Exception {
    location = Location.builder().globalName(CONTENT_METADATA_WITHOUT_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, typeBuilder.anyType().build(), void.class);
    assertExpectedType(getParameter(typedModel, "content"), personType, true);
  }

  @Test
  public void dynamicOutputWithoutKeyId() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITHOUT_KEY_PARAM).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    assertExpectedOutput(typedModel, personType, void.class);
    assertExpectedType(getParameter(typedModel, "content"), Object.class);
  }

  @Test
  public void dynamicOutputAndContentWithCache() throws Exception {
    location = Location.builder().globalName(CONTENT_AND_OUTPUT_CACHE_RESOLVER).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor =
        getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    final OperationModel typedModel = metadataDescriptor.getModel();
    MetadataType outputType = typedModel.getOutput().getType();
    MetadataType contentType = getParameter(typedModel, "content").getType();
    assertThat(contentType, is(equalTo(outputType)));
  }

  @Test
  public void resolverContentWithContextClassLoader() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    resolveTestWithContextClassLoader(RESOLVER_CONTENT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void resolverOutputWithContextClassLoader() throws Exception {
    location = Location.builder().globalName(OUTPUT_METADATA_WITH_KEY_ID).addProcessorsPart().addIndexPart(0).build();
    resolveTestWithContextClassLoader(RESOLVER_OUTPUT_WITH_CONTEXT_CLASSLOADER,
                                      MetadataExtensionFunctionalTestCase::getSuccessComponentDynamicMetadata);
  }

  @Test
  public void shouldInheritOperationResolvers() throws Exception {
    location = Location.builder().globalName(SHOULD_INHERIT_OPERATION_RESOLVERS).addProcessorsPart().addIndexPart(0).build();
    assertInheritedResolvers(PERSON_METADATA_KEY);
  }

  @Test
  public void shouldInheritExtensionResolvers() throws Exception {
    location = Location.builder().globalName(SHOULD_INHERIT_EXTENSION_RESOLVERS).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadataWithKey(PERSON_METADATA_KEY);
    assertInheritedResolvers(metadataDescriptor.getModel());
  }

  @Test
  public void shouldInheritOperationParentResolvers() throws Exception {
    location =
        Location.builder().globalName(SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS).addProcessorsPart().addIndexPart(0).build();
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    assertInheritedResolvers(metadataDescriptor.getModel());
  }

  @Test
  public void pagedOperationMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "animal"), Animal.class);
  }

  @Test
  public void pagedOperationResultMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA_RESULT).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    MetadataType param = metadataDescriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(Iterator.class.getName()));
    assertMessageType(((ArrayType) param).getType(), personType, TYPE_LOADER.load(Animal.class));
  }

  @Test
  public void pagedOperationResultWithAttributeResolverMetadataTestCase() throws Exception {
    location = Location.builder().globalName(PAGED_OPERATION_METADATA_RESULT_WITH_ATTRIBUTES).addProcessorsPart().addIndexPart(0)
        .build();
    ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);
    MetadataType param = metadataDescriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(Iterator.class.getName()));
    assertMessageType(((ArrayType) param).getType(), personType, personType);
  }

  @Test
  public void componentWithStaticInputs() throws IOException {
    location = Location.builder().globalName(TYPE_WITH_DECLARED_SUBTYPES_METADATA).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "plainShape"), Shape.class);
    assertExpectedType(getParameter(typedModel, "animal"), Animal.class);
    assertExpectedType(getParameter(typedModel, "rectangleSubtype"), Rectangle.class);
  }

  @Test
  public void retrieveKeysFromBooleanMetadataKey() {
    location = Location.builder().globalName(BOOLEAN_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertSuccessResult(result);
    String booleanMetadataResolver = "BooleanMetadataResolver";
    assertThat(result.get().getCategories(), contains(booleanMetadataResolver));
    Set<MetadataKey> metadataKeys = result.get().getKeys(booleanMetadataResolver).get();
    assertThat(metadataKeys, hasItems(metadataKeyWithId("FALSE"), metadataKeyWithId("TRUE")));
  }

  @Test
  public void booleanMetadataKey() throws IOException {
    location = Location.builder().globalName(BOOLEAN_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result =
        metadataService.getOperationMetadata(location, newKey("true").build());
    assertSuccessResult(result);
    assertExpectedType(getParameter(result.get().getModel(), "content"), TYPE_LOADER.load(SwordFish.class), true);
  }

  @Test
  public void retrieveKeysFromEnumMetadataKey() {
    location = Location.builder().globalName(ENUM_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<MetadataKeysContainer> result = metadataService.getMetadataKeys(location);
    assertSuccessResult(result);
    String enumMetadataResolver = "EnumMetadataResolver";
    assertThat(result.get().getCategories(), contains(enumMetadataResolver));

    Set<MetadataKey> metadataKeys = result.get().getKeys(enumMetadataResolver).get();
    MetadataKeyMatcher[] metadataKeyMatchers = Stream.of(AnimalClade.values())
        .map(Object::toString)
        .map(MetadataKeyMatcher::metadataKeyWithId)
        .toArray(MetadataKeyMatcher[]::new);

    assertThat(metadataKeys, hasItems(metadataKeyMatchers));
  }

  @Test
  public void enumMetadataKey() throws IOException {
    location = Location.builder().globalName(ENUM_METADATA_KEY).addProcessorsPart().addIndexPart(0).build();
    ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(newKey("MAMMAL").build());
    final ComponentModel typedModel = metadataDescriptor.getModel();
    assertExpectedType(getParameter(typedModel, "content"), TYPE_LOADER.load(Bear.class), true);
  }

  @Test
  public void metadataKeyDefaultValue() throws Exception {
    location = Location.builder().globalName(METADATA_KEY_DEFAULT_VALUE).addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    MetadataType type = result.get().getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(2));
    objectType.getFields().forEach(f -> assertThat(f.getKey().getName().getLocalPart(), isOneOf(TIRES, BRAND)));
    Optional<MetadataKey> metadataKeyOptional = result.get().getMetadataAttributes().getKey();
    assertThat(metadataKeyOptional.isPresent(), is(true));
    assertThat(metadataKeyOptional.get().getId(), is(CAR));
  }

  @Test
  public void defaultValueMultilevelMetadataKey() throws Exception {
    location = Location.builder().globalName(MULTILEVEL_METADATA_KEY_DEFAULT_VALUE).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> metadataDescriptor =
        metadataService.getOperationMetadata(location);
    MetadataType type = getParameter(metadataDescriptor.get().getModel(), "content").getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) type).getFields().size(), is(3));
    List<String> expectedKeys = Arrays.asList("CONTINENT", "COUNTRY", "CITY");
    Optional<ObjectFieldType> missingKey = ((ObjectType) type).getFields().stream()
        .filter(f -> !expectedKeys.contains(f.getKey().getName().getLocalPart()))
        .findFirst();
    assertThat(missingKey.isPresent(), is(false));
    assertResolvedKey(metadataDescriptor, LOCATION_MULTILEVEL_KEY);
  }

  @Test
  public void defaultValueMetadataKey() throws Exception {
    location = Location.builder().globalName(METADATA_KEY_DEFAULT_VALUE).addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    assertResolvedKey(result, CAR_KEY);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType type = descriptor.getModel().getOutput().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) type).getFields(), hasSize(2));
  }

  @Test
  public void operationWhichReturnsListOfMessages() throws Exception {
    location = Location.builder().globalName("listOfMessages").addProcessorsPart().addIndexPart(0).build();
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertMessageType(((ArrayType) param).getType(), TYPE_LOADER.load(String.class),
                      TYPE_LOADER.load(StringAttributes.class));
  }

  @Test
  public void operationWhichReturnsDynamicListOfMessages() throws Exception {
    location = Location.builder().globalName("dynamicListOfMessages").addProcessorsPart().addIndexPart(0).build();
    MetadataType param = getResolvedTypeFromList();
    assertMessageType(((ArrayType) param).getType(), personType, TYPE_BUILDER.voidType().build());
  }

  @Test
  public void operationWhichReturnsDynamicListOfObjects() throws Exception {
    location = Location.builder().globalName("dynamicListOfObjects").addProcessorsPart().addIndexPart(0).build();
    MetadataType param = getResolvedTypeFromList();
    assertExpectedType(((ArrayType) param).getType(), personType);
  }

  @Test
  public void operationReceivesListOfObjects() throws Exception {
    location = Location.builder().globalName("objectListAsInput").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    MetadataType objects = getParameter(operationMetadata.get().getModel(), "objects").getType();

    assertThat(objects, is(instanceOf(ArrayType.class)));
    assertThat(((ArrayType) objects).getType(), is(personType));
  }

  @Test
  public void operationReceivesNullTypeOfList() throws Exception {
    location = Location.builder().globalName("nullListAsInput").addProcessorsPart().addIndexPart(0).build();
    MetadataResult<ComponentMetadataDescriptor<OperationModel>> operationMetadata =
        metadataService.getOperationMetadata(location);
    MetadataType objects = getParameter(operationMetadata.get().getModel(), "objects").getType();

    assertThat(objects, is(instanceOf(NullType.class)));
  }

  private MetadataType getResolvedTypeFromList() {
    final MetadataResult<ComponentMetadataDescriptor<OperationModel>> result = metadataService.getOperationMetadata(location);
    assertSuccessResult(result);
    ComponentMetadataDescriptor<OperationModel> descriptor = result.get();
    MetadataType param = descriptor.getModel().getOutput().getType();
    assertThat(param, is(instanceOf(ArrayType.class)));
    assertThat(getId(param).get(), is(List.class.getName()));
    return param;
  }

  /**
   * Test template that sets an "invalid" classloader in TCCL different from the one that was used to register the extension and
   * asserts that, it sets back the original classloader to TCCL. Done in this way due to it is not possible to change extension
   * model classloader property once it is registered.
   */
  private void resolveTestWithContextClassLoader(String flowName, Callback<MetadataOperationTestCase> doAction)
      throws Exception {
    location = Location.builder().globalName(flowName).addProcessorsPart().addIndexPart(0).build();
    TestThreadContextClassLoaderResolver.reset();
    final ClassLoader originalClassLoader = org.mule.test.metadata.extension.MetadataConnection.class.getClassLoader();
    withContextClassLoader(mock(ClassLoader.class), () -> {
      doAction.execute(MetadataOperationTestCase.this);
      assertThat(TestThreadContextClassLoaderResolver.getCurrentState(), is(sameInstance(originalClassLoader)));
    });
  }

  private void assertInheritedResolvers(MetadataKey key) throws IOException {
    final ComponentMetadataDescriptor<OperationModel> metadataDescriptor = getSuccessComponentDynamicMetadata(key);
    final OperationModel typedModel = metadataDescriptor.getModel();
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
