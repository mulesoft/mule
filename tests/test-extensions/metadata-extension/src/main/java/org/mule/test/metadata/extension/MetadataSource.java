/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.Parameter;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;

import java.util.Map;


@MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
        contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
        outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
public class MetadataSource extends Source<Map<String, Object>, String>
{

    @MetadataKeyParam
    @Parameter
    public String type;

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }
}
