/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.capability.Xml;
import org.mule.runtime.extension.api.annotation.connector.Providers;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.test.metadata.extension.resolver.TestContentAndOutputResolverWithKeyResolver;

@Extension(name = "Metadata")
@Operations({MetadataOperations.class, MetadataFailureOperations.class, MetadataInheritedExtensionResolversOperations.class, MetadataInheritedOperationResolversOperations.class})
@Providers(MetadataConnectionProvider.class)
@Sources(MetadataSource.class)
@Xml(namespaceLocation = "http://www.mulesoft.org/schema/mule/metadata", namespace = "metadata")
@MetadataScope(keysResolver = TestContentAndOutputResolverWithKeyResolver.class,
        contentResolver = TestContentAndOutputResolverWithKeyResolver.class,
        outputResolver = TestContentAndOutputResolverWithKeyResolver.class)
public class MetadataExtension
{

}