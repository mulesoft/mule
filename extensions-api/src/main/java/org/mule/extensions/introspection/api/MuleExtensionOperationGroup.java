/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

public interface MuleExtensionOperationGroup extends Described
{

    public static enum AllowedChildsType
    {

        MESSAGE_PROCESSOR,
        OUTBOUND_ENDPOINT,
        MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT,
        OWN_EXTENSION_OPERATION,
        ANY
    }

    AllowedChildsType getAllowedChildsType();

    int getMinOperations();

    int getMaxOperations();

}
