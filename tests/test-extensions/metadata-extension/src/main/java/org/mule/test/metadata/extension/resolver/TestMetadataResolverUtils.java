/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension.resolver;

import org.mule.metadata.api.builder.BaseTypeBuilder;
import org.mule.metadata.api.builder.ObjectTypeBuilder;
import org.mule.metadata.api.model.MetadataFormat;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.test.metadata.extension.MetadataConnection;

import java.util.List;
import java.util.stream.Collectors;

public class TestMetadataResolverUtils
{

    public static final String TIRES = "Tires";
    public static final String DIRECTION = "Direction";
    public static final String NAME = "Name";
    public static final String APPLICATION_JAVA_MIME_TYPE = "application/java";
    public static final String BRAND = "Brand";
    public static final String SIZE = "Size";
    public static final String AGE = "Age";

    public static List<MetadataKey> getKeys(MetadataContext context) throws ConnectionException
    {
        MetadataConnection connection = (MetadataConnection) context.getConnection().get();

        return connection.getEntities().stream()
                .map(e -> MetadataKeyBuilder.newKey(e).build())
                .collect(Collectors.toList());
    }

    public static MetadataType getMetadata(MetadataKey key)
    {
        final ObjectTypeBuilder objectBuilder = BaseTypeBuilder.create(new MetadataFormat(key.getId(), key.getId(), APPLICATION_JAVA_MIME_TYPE)).objectType();

        switch (key.getId())
        {
            case MetadataConnection.CAR:
                objectBuilder.addField().key(TIRES).value().numberType();
                objectBuilder.addField().key(BRAND).value().stringType();
                break;
            case MetadataConnection.HOUSE:
                objectBuilder.addField().key(DIRECTION).value().stringType();
                objectBuilder.addField().key(SIZE).value().numberType();
                break;
            case MetadataConnection.PERSON:
                objectBuilder.addField().key(NAME).value().stringType();
                objectBuilder.addField().key(AGE).value().numberType();
                break;
        }

        return objectBuilder.build();
    }

}
