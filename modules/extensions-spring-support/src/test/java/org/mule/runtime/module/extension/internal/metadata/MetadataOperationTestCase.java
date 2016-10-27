/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
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

import org.mule.functional.listener.Callback;
import org.mule.metadata.api.model.DateType;
import org.mule.metadata.api.model.DictionaryType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectFieldType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.ParameterMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.DefaultMetadataCache;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.tck.message.StringAttributes;
import org.mule.test.metadata.extension.model.animals.Animal;
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
    assertSuccess(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys.size(), is(3));
    assertThat(metadataKeys, hasItems(metadataKeyWithId(PERSON), metadataKeyWithId(CAR), metadataKeyWithId(HOUSE)));
  }

  @Test
  public void getMetadataKeysWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);
    final MetadataResult<MetadataKeysContainer> metadataKeys = metadataService.getMetadataKeys(componentId);
    assertSuccess(metadataKeys);
    final Set<MetadataKey> keys = getKeysFromContainer(metadataKeys.get());
    assertThat(keys.size(), is(1));
    assertThat(keys.iterator().next(), instanceOf(NullMetadataKey.class));
  }

  @Test
  public void getMultilevelKeys() throws Exception {
    componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
    final MetadataResult<MetadataKeysContainer> metadataKeysResult = metadataService.getMetadataKeys(componentId);
    assertSuccess(metadataKeysResult);
    final Set<MetadataKey> metadataKeys = getKeysFromContainer(metadataKeysResult.get());
    assertThat(metadataKeys, hasSize(2));

    assertThat(metadataKeys, hasItem(metadataKeyWithId(AMERICA).withDisplayName(AMERICA).withPartName(CONTINENT)));
    assertThat(metadataKeys, hasItem(metadataKeyWithId(EUROPE).withDisplayName(EUROPE).withPartName(CONTINENT)));
  }

  @Test
  public void injectComposedMetadataKeyIdInstanceInMetadataResolver() throws Exception {
    componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
    MetadataKey key = newKey(AMERICA, CONTINENT).withChild(newKey(USA, COUNTRY).withChild(newKey(SAN_FRANCISCO, CITY))).build();
    final MetadataResult<ComponentMetadataDescriptor> metadataResult = metadataService.getMetadata(componentId, key);
    assertSuccess(metadataResult);
  }

  @Test
  public void dynamicOperationMetadata() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(PERSON_METADATA_KEY);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void outputAndMultipleInputWithKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_AND_MULTIPLE_INPUT_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("firstPerson"), "firstPerson", personType, true);
    assertExpectedType(input.getParameterMetadata("otherPerson"), "otherPerson", personType, true);
  }

  @Test
  public void dynamicOutputWithoutContentParam() throws Exception {
    // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyId
    componentId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
  }

  @Test
  public void dynamicContentWithoutOutput() throws Exception {
    // Resolver for content and output type, no return type, resolves only @Content, with key and KeyId
    componentId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void operationOutputWithoutKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("content"), "content", Object.class);
  }

  @Test
  public void contentAndOutputMetadataWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void contentMetadataWithoutKeysWithKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void outputMetadataWithoutKeysWithKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
  }

  @Test
  public void messageAttributesVoidTypeMetadata() throws Exception {
    componentId = new ProcessorId(MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor> componentMetadata = metadataService.getMetadata(componentId);
    assertSuccess(componentMetadata);
    ComponentMetadataDescriptor metadataDescriptor = componentMetadata.get();
    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), ExtensionsTestUtils.TYPE_BUILDER.anyType().build(), void.class);
    assertSuccess(metadataDescriptor.getInputMetadata());
    assertThat(metadataDescriptor.getInputMetadata().get().getAllParameters().isEmpty(), is(true));
  }

  @Test
  public void messageAttributesStringTypeMetadata() throws Exception {
    componentId = new ProcessorId(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, StringAttributes.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);

  }

  @Test
  public void attributesDynamicPersonTypeMetadata() throws Exception {
    componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    MetadataType type = metadataDescriptor.getOutputMetadata().get().getAttributesMetadata().get().getType();
    assertThat(type, is(instanceOf(DictionaryType.class)));
    DictionaryType dictionary = (DictionaryType) type;
    assertThat(dictionary.getKeyType(), is(instanceOf(DateType.class)));
    assertThat(dictionary.getValueType(), is(instanceOf(StringType.class)));
  }

  @Test
  public void attributesUnionTypeMetadata() throws Exception {
    componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor descriptor = getSuccessComponentDynamicMetadata();
    assertExpectedOutput(descriptor.getOutputMetadata(), Shape.class, AbstractOutputAttributes.class);
  }

  @Test
  public void getContentMetadataWithKey() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();


    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), typeBuilder.anyType().build(), void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void getOutputMetadataWithKey() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", Object.class);
  }

  @Test
  public void dynamicContentWithoutKeyId() throws Exception {
    componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), typeBuilder.anyType().build(), void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("content"), "content", personType, true);
  }

  @Test
  public void dynamicOutputWithoutKeyId() throws Exception {
    componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);

    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("content"), "content", Object.class);
  }

  @Test
  public void dynamicOutputAndContentWithCache() throws Exception {
    componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata(NULL_METADATA_KEY);

    assertThat(metadataDescriptor.getOutputMetadata().isSuccess(), is(true));
    assertThat(metadataDescriptor.getOutputMetadata().get().getPayloadMetadata().isSuccess(), is(true));
    MetadataType outputType = metadataDescriptor.getOutputMetadata().get().getPayloadMetadata().get().getType();

    assertSuccess(metadataDescriptor.getInputMetadata());
    MetadataType content = metadataDescriptor.getInputMetadata().get().getParameterMetadata("content").get().getType();
    assertThat(content, is(equalTo(outputType)));

  }

  @Test
  public void resolverContentWithContextClassLoader() throws Exception {
    doResolverTestWithContextClassLoader(RESOLVER_CONTENT_WITH_CONTEXT_CLASSLOADER,
                                         source -> source.getSuccessComponentDynamicMetadata());
  }

  @Test
  public void resolverOutputWithContextClassLoader() throws Exception {
    doResolverTestWithContextClassLoader(RESOLVER_OUTPUT_WITH_CONTEXT_CLASSLOADER,
                                         source -> source.getSuccessComponentDynamicMetadata());
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
    assertInheritedResolvers(metadataDescriptor);
  }

  @Test
  public void shouldInheritOperationParentResolvers() throws Exception {
    componentId = new ProcessorId(SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS, FIRST_PROCESSOR_INDEX);
    final ComponentMetadataDescriptor metadataDescriptor = getSuccessComponentDynamicMetadata();
    assertInheritedResolvers(metadataDescriptor);
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
    MetadataResult<ComponentMetadataDescriptor> componentMetadata = metadataService.getMetadata(componentId);
    assertSuccess(componentMetadata);
    assertExpectedType(componentMetadata.get().getInputMetadata().get().getParameterMetadata("animal"), "animal", Animal.class);
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

    assertThat(configCache.asMap().keySet(), hasItems(AGE, NAME, BRAND));
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
    MetadataResult<ComponentMetadataDescriptor> metadata = metadataService.getMetadata(componentId);
    assertSuccess(metadata);

    InputMetadataDescriptor input = metadata.get().getInputMetadata().get();

    assertExpectedType(input.getParameterMetadata("plainShape"), "plainShape", Shape.class);
    assertExpectedType(input.getParameterMetadata("animal"), "animal", Animal.class);
    assertExpectedType(input.getParameterMetadata("rectangleSubtype"), "rectangleSubtype", Rectangle.class);
  }

  @Test
  public void booleanMetadataKey() throws IOException {
    componentId = new ProcessorId(BOOLEAN_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor> result = metadataService.getMetadata(componentId, newKey("true").build());
    assertSuccess(result);
    assertExpectedType(result.get().getInputMetadata().get().getParameterMetadata("content"), "content",
                       TYPE_LOADER.load(SwordFish.class), true);
  }

  @Test
  public void enumMetadataKey() throws IOException {
    componentId = new ProcessorId(ENUM_METADATA_KEY, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor> result = metadataService.getMetadata(componentId, newKey("MAMMAL").build());
    assertSuccess(result);
    assertExpectedType(result.get().getInputMetadata().get().getParameterMetadata("content"), "content",
                       TYPE_LOADER.load(Bear.class), true);
  }

  @Test
  public void metadataKeyDefaultValue() throws Exception {
    componentId = new ProcessorId(METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    MetadataResult<ComponentMetadataDescriptor> result = metadataService.getMetadata(componentId);
    assertSuccess(result);
    MetadataType type = result.get().getOutputMetadata().get().getPayloadMetadata().get().getType();
    assertThat(type, is(instanceOf(ObjectType.class)));
    ObjectType objectType = (ObjectType) type;
    assertThat(objectType.getFields(), hasSize(2));
    objectType.getFields().forEach(f -> assertThat(f.getKey().getName().getLocalPart(), isOneOf(TIRES, BRAND)));
  }

  @Test
  public void defaultValueMultilevelMetadataKey() throws Exception {
    componentId = new ProcessorId(MULTILEVEL_METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor> result = metadataService.getMetadata(componentId);
    assertSuccess(result);
    ComponentMetadataDescriptor descriptor = result.get();
    ParameterMetadataDescriptor param = descriptor.getInputMetadata().get().getParameterMetadata("content").get();
    assertThat(param.getType(), is(instanceOf(ObjectType.class)));

    assertThat(((ObjectType) param.getType()).getFields().size(), is(3));

    List<String> expectedKeys = Arrays.asList("CONTINENT", "COUNTRY", "CITY");
    Optional<ObjectFieldType> missingKey = ((ObjectType) param.getType()).getFields().stream()
        .filter(f -> !expectedKeys.contains(f.getKey().getName().getLocalPart()))
        .findFirst();

    assertThat(missingKey.isPresent(), is(false));
  }

  @Test
  public void defaultValueMetadataKey() throws Exception {
    componentId = new ProcessorId(METADATA_KEY_DEFAULT_VALUE, FIRST_PROCESSOR_INDEX);
    final MetadataResult<ComponentMetadataDescriptor> result = metadataService.getMetadata(componentId);
    assertSuccess(result);
    ComponentMetadataDescriptor descriptor = result.get();
    TypeMetadataDescriptor param = descriptor.getOutputMetadata().get().getPayloadMetadata().get();
    assertThat(param.getType(), is(instanceOf(ObjectType.class)));
    assertThat(((ObjectType) param.getType()).getFields(), hasSize(2));
  }

  /**
   * Test template that sets an "invalid" classloader in TCCL different from the one that was used to register the extension and
   * asserts that, it sets back the original classloader to TCCL. Done in this way due to it is not possible to change extension
   * model classloader property once it is registered.
   */
  private void doResolverTestWithContextClassLoader(String flowName, Callback<MetadataOperationTestCase> doAction)
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
    assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);
    assertInheritedResolvers(metadataDescriptor);
  }

  private void assertInheritedResolvers(ComponentMetadataDescriptor metadataDescriptor) throws IOException {
    assertSuccess(metadataDescriptor.getInputMetadata());
    InputMetadataDescriptor input = metadataDescriptor.getInputMetadata().get();
    assertExpectedType(input.getParameterMetadata("type"), "type", String.class);
    assertExpectedType(input.getParameterMetadata("content"), "content", Object.class);
  }
}
