/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.AGE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.APPLICATION_JAVA_MIME_TYPE;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.BRAND;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.NAME;
import static org.mule.test.metadata.extension.resolver.TestMetadataResolverUtils.getKeys;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataCache;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.MetadataContentResolver;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class TestResolverWithCache implements MetadataContentResolver, MetadataOutputResolver, MetadataKeysResolver
{

    public static final String MISSING_ELEMENT_ERROR_MESSAGE = "Missing element in the cache. There was no element in the cache for the key: " + BRAND;
    public static final int AGE_VALUE = 16;
    public static final String NAME_VALUE = "Juan";
    public static final String BRAND_VALUE = "Nikdidas";

    @Override
    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        MetadataCache cache = context.getCache();
        Optional<? extends Serializable> element = cache.get(BRAND);
        if (!element.isPresent())
        {
            throw new MetadataResolvingException(MISSING_ELEMENT_ERROR_MESSAGE, FailureCode.RESOURCE_UNAVAILABLE);
        }

        return buildMetadataType((String) element.get());
    }

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        MetadataCache cache = context.getCache();
        Optional<String> brand = cache.get(BRAND);
        if (brand.isPresent())
        {
            String serializable = brand.get();
            return buildMetadataType(serializable);
        }
        String cachedModel = BRAND_VALUE;
        cache.put(BRAND, cachedModel);
        return buildMetadataType(cachedModel);
    }

    private MetadataType buildMetadataType(String model)
    {
        return BaseTypeBuilder.create(new MetadataFormat(model, model, APPLICATION_JAVA_MIME_TYPE)).objectType().build();
    }

    @Override
    public List<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException
    {
        context.getCache().put(AGE, AGE_VALUE);
        context.getCache().put(NAME, NAME_VALUE);
        return getKeys(context);
    }

    private final class SerializableAccount implements Serializable
    {

        private String accountName;
        private String accountId;

        public SerializableAccount(String accountId, String accountName)
        {
            this.accountId = accountId;
            this.accountName = accountName;
        }

        public String getAccountId()
        {
            return accountId;
        }

        public String getAccountName()
        {
            return accountName;
        }
    }
}
