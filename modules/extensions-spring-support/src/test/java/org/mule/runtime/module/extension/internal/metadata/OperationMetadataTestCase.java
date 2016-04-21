/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.AGE_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.BRAND_VALUE;
import static org.mule.test.metadata.extension.resolver.TestResolverWithCache.NAME_VALUE;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataManager;
import org.mule.runtime.api.metadata.ProcessorId;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.runtime.core.internal.metadata.DefaultMetadataCache;
import org.mule.runtime.core.internal.metadata.MuleMetadataManager;
import org.mule.runtime.module.extension.internal.util.ExtensionsTestUtils;

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

    @Test
    public void getMetadataKeysWithKeyParam() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeysResult = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeysResult.isSuccess(), is(true));
        final List<MetadataKey> metadataKeys = metadataKeysResult.get();
        assertThat(metadataKeys.size(), is(3));
        assertThat(metadataKeys, contains(METADATA_KEYS.toArray()));
    }

    @Test
    public void getMetadataKeysWithoutKeyParam() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);
        final MetadataResult<List<MetadataKey>> metadataKeys = metadataManager.getMetadataKeys(componentId);
        assertThat(metadataKeys.isSuccess(), is(true));
        assertThat(metadataKeys.get().size(), is(1));
        assertThat(metadataKeys.get().get(0), instanceOf(NullMetadataKey.class));
    }

    @Test
    public void dynamicOperationMetadata() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

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
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

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
        // Resolver for content and output type, no @Content param, resolves only output, with keysResolver and KeyParam
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
        // Resolver for content and output type, no return type, resolves only @Content, with key and KeyParam
        componentId = new ProcessorId(CONTENT_ONLY_IGNORES_OUTPUT, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void operationOutputWithoutKeyParam() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentAndOutputMetadataWithoutKeyParam() throws Exception
    {
        componentId = new ProcessorId(CONTENT_AND_OUTPUT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), is(empty()));
    }

    @Test
    public void contentMetadataWithoutKeysWithKeyParam() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), void.class, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);
    }

    @Test
    public void outputMetadataWithoutKeysWithKeyParam() throws Exception
    {
        componentId = new ProcessorId(OUTPUT_METADATA_WITHOUT_KEYS_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

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
        componentId  = new ProcessorId(MESSAGE_ATTRIBUTES_PERSON_TYPE_METADATA, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();
        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, String.class);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void getContentMetadataWithKey() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

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
        componentId = new ProcessorId(OUTPUT_METADATA_WITH_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata();


        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), personType, void.class);

        assertThat(metadataDescriptor.getParametersMetadata().size(), is(1));
        assertExpectedType(metadataDescriptor.getParametersMetadata().get(0), "type", String.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", Object.class);
    }

    @Test
    public void dynamicContentWithoutKeyParam() throws Exception
    {
        componentId = new ProcessorId(CONTENT_METADATA_WITHOUT_KEY_PARAM, FIRST_PROCESSOR_INDEX);

        final ComponentMetadataDescriptor metadataDescriptor = getComponentDynamicMetadata(nullMetadataKey);

        assertExpectedOutput(metadataDescriptor.getOutputMetadata(), Object.class, void.class);

        assertThat(metadataDescriptor.getContentMetadata().isPresent(), is(true));
        assertExpectedType(metadataDescriptor.getContentMetadata().get(), "content", personType);

        assertThat(metadataDescriptor.getParametersMetadata(), empty());
    }

    @Test
    public void dynamicOutputWithoutKeyParam() throws Exception
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
}
