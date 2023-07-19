/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.test.metadata.extension.resolver.TestInputAndOutputResolverWithKeyResolver;

@MetadataScope(keysResolver = TestInputAndOutputResolverWithKeyResolver.class,
    outputResolver = TestInputAndOutputResolverWithKeyResolver.class)
public class MetadataOperationsParent {

}
