/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.metadata.extension;

import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.module.extension.internal.metadata.extension.MetadataConnection;

public class MetadataInheritedExtensionResolversOperations
{
    public void shouldInheritExtensionResolvers(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }
}
