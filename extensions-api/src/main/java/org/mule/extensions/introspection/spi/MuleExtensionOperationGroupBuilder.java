/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.spi;

import org.mule.extensions.introspection.api.MuleExtensionOperationGroup;

public interface MuleExtensionOperationGroupBuilder extends Builder<MuleExtensionOperationGroup>
{

    MuleExtensionOperationGroupBuilder setName(String name);

    MuleExtensionOperationGroupBuilder setDescription(String description);

    MuleExtensionOperationGroupBuilder setAllowedChildsType(MuleExtensionOperationGroup.AllowedChildsType type);

    MuleExtensionOperationGroupBuilder setMinOperations(int minOperations);

    MuleExtensionOperationGroupBuilder setMaxOperations(int maxOperations);

}
