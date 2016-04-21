/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.metadata.extension;

import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyParam;
import org.mule.runtime.extension.api.annotation.param.Connection;

public class MetadataInheritedOperationResolversOperations extends MetadataOperationsParent
{
    public void shouldInheritOperationParentResolvers(@Connection MetadataConnection connection, @MetadataKeyParam String type, @Content Object content)
    {
    }

}
