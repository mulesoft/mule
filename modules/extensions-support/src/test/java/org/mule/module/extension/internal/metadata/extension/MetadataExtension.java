/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension;

import org.mule.extension.api.annotation.Extension;
import org.mule.extension.api.annotation.Operations;
import org.mule.extension.api.annotation.Sources;
import org.mule.extension.api.annotation.capability.Xml;
import org.mule.extension.api.annotation.connector.Providers;

@Extension(name = "Metadata")
@Operations({MetadataOperations.class, MetadataFailureOperations.class})
@Providers(MetadataConnectionProvider.class)
@Sources(MetadataSource.class)
@Xml(schemaLocation = "http://www.mulesoft.org/schema/mule/metadata", namespace = "metadata", schemaVersion = "3.7")
public class MetadataExtension
{

}