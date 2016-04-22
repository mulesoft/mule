/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MetadataKeyBuilder.newKey;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.UnionTypeBuilder;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.NullPayload;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.MetadataKeysResolver;
import org.mule.runtime.api.metadata.resolving.MetadataOutputResolver;
import org.mule.runtime.core.api.lifecycle.Initialisable;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.extension.api.introspection.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.module.http.internal.ParameterMap;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MetadataResolver implements Initialisable, MetadataKeysResolver, MetadataOutputResolver<String>
{
    private static final ClassTypeLoader  TYPE_LOADER = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader();
    private static final String ANY = "ANY";
    private Map<String, MetadataType> types = new HashMap<>();
    private Class[] classes = new Class[]{InputStream.class, NullPayload.class, ParameterMap.class};

    @Override
    public List<MetadataKey> getMetadataKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException
    {
        List<MetadataKey> keyList = new LinkedList<>();
        types.keySet().stream().forEach(
                aKey -> keyList.add(newKey(aKey).build())
        );
        return keyList;
    }

    @Override
    public MetadataType getOutputMetadata(MetadataContext context, String key) throws MetadataResolvingException, ConnectionException
    {
        return types.get(key);
    }

    @Override
    public void initialise() throws InitialisationException
    {
        Arrays.stream(classes).map(aClass -> types.put(aClass.getSimpleName(), TYPE_LOADER.load(aClass)));
        UnionTypeBuilder builder = new BaseTypeBuilder<>(JAVA).unionType()
                .of(types.get("InputStream"))
                .of(types.get("NullPayload"))
                .of(types.get("ParameterMap"));
        types.put(ANY, builder.build());
    }

}
