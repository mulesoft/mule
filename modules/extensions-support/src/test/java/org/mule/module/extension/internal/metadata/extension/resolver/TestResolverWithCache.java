/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension.resolver;

import org.mule.api.connection.ConnectionException;
import org.mule.api.metadata.MetadataCache;
import org.mule.api.metadata.MetadataContext;
import org.mule.api.metadata.MetadataKey;
import org.mule.api.metadata.MetadataResolvingException;
import org.mule.api.metadata.resolving.FailureCode;
import org.mule.api.metadata.resolving.MetadataContentResolver;
import org.mule.api.metadata.resolving.MetadataOutputResolver;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.java.JavaTypeLoader;

import java.io.Serializable;
import java.util.Optional;

public class TestResolverWithCache implements MetadataContentResolver, MetadataOutputResolver
{

    private static final String CACHE_ELEMENT_KEY = "ACCOUNT";
    public static final String ERROR_MESSAGE = "Cache element was expected. There was no element in the cache for the key: " + CACHE_ELEMENT_KEY;

    @Override
    public MetadataType getContentMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        MetadataCache cache = context.getCache();
        Optional<? extends Serializable> element = cache.get(CACHE_ELEMENT_KEY);
        if (!element.isPresent())
        {
            throw new MetadataResolvingException(ERROR_MESSAGE, FailureCode.RESOURCE_UNAVAILABLE);
        }

        return buildMetadataType((Class) element.get());
    }

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, MetadataKey key) throws MetadataResolvingException, ConnectionException
    {
        MetadataCache cache = context.getCache();
        if (cache.get(CACHE_ELEMENT_KEY).isPresent())
        {
            return buildMetadataType((Class) cache.get(CACHE_ELEMENT_KEY).get());
        }

        Class cachedModel = SerializableAccount.class;
        cache.put(CACHE_ELEMENT_KEY, cachedModel);
        return buildMetadataType(cachedModel);
    }

    private MetadataType buildMetadataType(Class model)
    {
        return new JavaTypeLoader(this.getClass().getClassLoader()).load(model);
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
