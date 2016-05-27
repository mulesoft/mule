/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils.toMetadataType;
import static org.mule.test.metadata.extension.MetadataConnection.CAR;
import static org.mule.test.metadata.extension.MetadataConnection.HOUSE;
import static org.mule.test.metadata.extension.MetadataConnection.PERSON;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.AMERICA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.SAN_FRANCISCO;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.USA;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.buildAmericaKey;
import static org.mule.test.metadata.extension.resolver.TestMultiLevelKeyResolver.buildEuropeKey;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.AGE_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.BRAND_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.NAME_VALUE;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.impl.DefaultUnionType;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataManager;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.core.internal.metadata.DefaultMetadataCache;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;
import org.mule.test.metadata.extension.LocationKey;
import org.mule.test.metadata.extension.model.animals.Bear;
import org.mule.test.metadata.extension.model.attribute.AnimalsOutputAttributes;
import org.mule.test.metadata.extension.model.attribute.ShapeOutputAttributes;
import org.mule.test.metadata.extension.model.shapes.Circle;
import org.mule.test.metadata.extension.model.shapes.Rectangle;
import org.mule.test.metadata.extension.model.shapes.Square;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class OperationMetadataTestCase extends MetadataExtensionFunctionalTestCase
{

    private static final String MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA = "messageAttributesPersonTypeMetadata";
    private static final String MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA = "messageAttributesNullTypeMetadata";
    private static final String CONFIG = "config";
    private static final String ALTERNATIVE_CONFIG = "alternative-config";

    @Override
    protected String getConfigFile()
    {
        return METADATA_TEST;
    }

    @Test
    public void getMetadataKeysWithKeyId() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeysResult.isSuccess(), is(true));
        final List<MetadataKey> metadataKeys = metadataKeysResult.get();
        assertThat(metadataKeys.size(), is(3));
        List<String> keyIds = metadataKeys.stream().map(MetadataKey::getId).collect(toList());
        assertThat(keyIds, hasItems(PERSON, CAR, HOUSE));
    }

    @Test
    public void getMetadataKeysWithoutKeyId() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeys.isSuccess(), is(true));
        assertThat(metadataKeys.get().size(), is(1));
        assertThat(metadataKeys.get().get(0), instanceOf(NullMetadataKey.class));
    }

    @Test
    public void getMultilevelKeys() throws Exception
    {
        MetadataKey expectedAmerica = buildAmericaKey();
        MetadataKey expectedEurope = buildEuropeKey();

        componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeysResult.isSuccess(), is(true));

        final List<MetadataKey> metadataKeys = metadataKeysResult.get();
        assertThat(metadataKeys, hasSize(2));

        MetadataKey america = metadataKeys.get(0);
        assertThat(america, is(expectedAmerica));

        MetadataKey europe = metadataKeys.get(1);
        assertThat(europe, is(expectedEurope));
    }

    @Test
    public void injectComposedMetadataKeyIdInstanceInOperation() throws Exception
    {
        LocationKey payload = (LocationKey) runFlow(SIMPLE_MULTILEVEL_KEY_RESOLVER).getMessage().getPayload();

        LocationKey expected = new LocationKey();
        expected.setContinent(AMERICA);
        expected.setCountry(USA);
        expected.setCity(SAN_FRANCISCO);

        assertThat(payload, is(expected));
    }

    @Test
    public void injectComposedMetadataKeyIdInstanceInMetadataResolver() throws Exception
    {
        componentId = new ProcessorId(SIMPLE_MULTILEVEL_KEY_RESOLVER, FIRST_PROCESSOR_INDEX);
        MetadataKey key = newKey(AMERICA).withChild(newKey(USA).withChild(newKey(SAN_FRANCISCO))).build();
        final MetadataResult<ComponentMetadataDescriptor> metadataKeysResult = metadataManager.getMetadata(componentId, key);
        assertThat(metadataKeysResult.isSuccess(), is(true));
    }

    @Test
    public void dynamicOperationMetadata() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertInheritedResolvers(metadataDescriptor);
    }

    private void assertInheritedResolvers(ComponentMetadataDescriptor metadataDescriptor) throws IOException
    {
        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void staticOperationMetadata() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentStaticMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);
    }

    @Test
    public void dynamicOutputWithoutContentParam() throws Exception
    {
        // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyId
        componentId = new ProcessorId(OUTPUT_ONLY_WITHOUT_CONTENT_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(false));
    }

    @Test
    public void dynamicContentWithoutOutput() throws Exception
    {
        // Resolver for content and output type, no return type, resolves only @Content, with key and KeyId
        componentId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void operationOutputWithoutKeyId() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentAndOutputMetadataWithoutKeyId() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentMetadataWithoutKeysWithKeyId() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void outputMetadataWithoutKeysWithKeyId() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(false));
    }

    @Test
    public void messageAttributesNullTypeMetadata() throws Exception
    {
        componentId = new ProcessorId(MESSAGE_ATTRIBUTES_NULL_TYPE_METADATA, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentStaticMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), ExtensionsTestUtils.TYPE_BUILDER.anyType().build(), void.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void messageAttributesStringTypeMetadata() throws Exception
    {
        componentId = new ProcessorId(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, String.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void attributesUnionTypeMetadata() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        MetadataType shapeType = metadataDescriptor.getOutputMetadata().getPayloadMetadata().getType();
        assertThat(shapeType, is(instanceOf(DefaultUnionType.class)));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasSize(2));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasItems(toMetadataType(Circle.class), toMetadataType(Rectangle.class)));

        MetadataType attributesType = metadataDescriptor.getOutputMetadata().getAttributesMetadata().getType();
        assertThat(attributesType, is(instanceOf(DefaultUnionType.class)));
        assertThat(((DefaultUnionType) attributesType).getTypes(), hasSize(2));
        assertThat(((DefaultUnionType) attributesType).getTypes(), hasItems(toMetadataType(ShapeOutputAttributes.class), toMetadataType(AnimalsOutputAttributes.class)));
    }

    @Test
    public void attributesDynamicPersonTypeMetadata() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_ATTRIBUTES_WITH_DYNAMIC_METADATA, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, personType);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);
    }

    @Test
    public void getContentMetadataWithKey() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void getOutputMetadataWithKey() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);
    }

    @Test
    public void dynamicContentWithoutKeyId() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_ID, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata(nullMetadataKey);

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void dynamicOutputWithoutKeyId() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata(nullMetadataKey);

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void dynamicOutputAndContentWithCache() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata(nullMetadataKey);

        assertThat(metadataDescriptor.getContentMetadata().get().getType(),
                   is(equalTo(metadataDescriptor.getOutputMetadata().getPayloadMetadata().getType())));

    }

    @Test
    public void shouldInheritOperationResolvers() throws Exception
    {
        componentId = new ProcessorId(SHOULD_INHERIT_OPERATION_RESOLVERS, FIRST_PROCESSOR_INDEX);

        assertInheritedResolvers();
    }

    @Test
    public void shouldInheritExtensionResolvers() throws Exception
    {
        componentId = new ProcessorId(SHOULD_INHERIT_EXTENSION_RESOLVERS, FIRST_PROCESSOR_INDEX);
        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertInheritedResolvers(metadataDescriptor);
    }

    @Test
    public void shouldInheritOperationParentResolvers() throws Exception
    {
        componentId = new ProcessorId(SHOULD_INHERIT_OPERATION_PARENT_RESOLVERS, FIRST_PROCESSOR_INDEX);
        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertInheritedResolvers(metadataDescriptor);
    }

    private void assertInheritedResolvers() throws IOException
    {
        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);
        assertInheritedResolvers(metadataDescriptor);
    }

    @Test
    public void multipleCaches() throws Exception
    {
        // using config
        componentId = new ProcessorId(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
        getComponentDynamicMetadata();
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        getComponentDynamicMetadata();

        // using alternative-config
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG, FIRST_PROCESSOR_INDEX);
        getComponentDynamicMetadata();

        MuleMetadataManager metadataManager = (MuleMetadataManager) muleContext.getRegistry().lookupObject(MetadataManager.class);
        Map<String, ? extends MetadataCache> caches = metadataManager.getMetadataCaches();

        assertThat(caches.keySet(), hasSize(2));
        assertThat(caches.keySet(), hasItems(CONFIG, ALTERNATIVE_CONFIG));
    }

    @Test
    public void elementsAreStoredInCaches() throws Exception
    {
        // using config
        componentId = new ProcessorId(OUTPUT_AND_METADATA_KEY_CACHE_RESOLVER, FIRST_PROCESSOR_INDEX);
        metadataManager.getMetadataKeys(componentId);
        getComponentDynamicMetadata();

        // using alternative-config
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_CACHE_RESOLVER_WITH_ALTERNATIVE_CONFIG, FIRST_PROCESSOR_INDEX);
        getComponentDynamicMetadata();

        MuleMetadataManager metadataManager = (MuleMetadataManager) muleContext.getRegistry().lookupObject(MetadataManager.class);
        DefaultMetadataCache configCache = (DefaultMetadataCache) metadataManager.getMetadataCaches().get(CONFIG);

        assertThat(configCache.asMap().keySet(), hasItems(AGE, NAME, BRAND));
        assertThat(configCache.get(AGE).get(), is(AGE_VALUE));
        assertThat(configCache.get(NAME).get(), is(NAME_VALUE));
        assertThat(configCache.get(BRAND).get(), is(BRAND_VALUE));

        DefaultMetadataCache alternativeConfigCache = (DefaultMetadataCache) metadataManager.getMetadataCaches().get(ALTERNATIVE_CONFIG);
        assertThat(alternativeConfigCache.asMap().keySet(), hasItems(BRAND));
        assertThat(alternativeConfigCache.get(BRAND).get(), is(BRAND_VALUE));
    }

    @Test
    public void abstractClassWithSubtypesMetadataType()
    {
        componentId = new ProcessorId(TYPE_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId);
        assertThat(metadata.isSuccess(), is(true));

        TypeMetadataDescriptor shapeMetadata = metadata.get().getParametersMetadata().get(0);
        assertThat(shapeMetadata.getName(), is("plainShape"));

        MetadataType shapeType = shapeMetadata.getType();
        assertThat(shapeType, is(instanceOf(DefaultUnionType.class)));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasSize(2));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasItems(toMetadataType(Circle.class), toMetadataType(Rectangle.class)));
    }

    @Test
    public void instantiableClassWithSubtypesMetadataType()
    {
        componentId = new ProcessorId(TYPE_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId);
        assertThat(metadata.isSuccess(), is(true));

        TypeMetadataDescriptor rectangleMetadata = metadata.get().getParametersMetadata().get(1);
        assertThat(rectangleMetadata.getName(), is("rectangleSubtype"));

        MetadataType shapeType = rectangleMetadata.getType();
        assertThat(shapeType, is(instanceOf(DefaultUnionType.class)));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasSize(2));
        assertThat(((DefaultUnionType) shapeType).getTypes(), hasItems(toMetadataType(Rectangle.class), toMetadataType(Square.class)));
    }

    @Test
    public void interfaceWithSubtypesMetadataType()
    {
        componentId = new ProcessorId(TYPE_WITH_DECLARED_SUBTYPES_METADATA, FIRST_PROCESSOR_INDEX);
        MetadataResult<ComponentMetadataDescriptor> metadata = metadataManager.getMetadata(componentId);
        assertThat(metadata.isSuccess(), is(true));

        TypeMetadataDescriptor animalMetadata = metadata.get().getParametersMetadata().get(2);
        assertThat(animalMetadata.getName(), is("animal"));
        assertThat(animalMetadata.getType(), is(toMetadataType(Bear.class)));
    }
}
