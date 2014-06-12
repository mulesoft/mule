/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

/**
 * A group of operations bound together
 *
 * @since 1.0
 */
public interface MuleExtensionOperationGroup extends Described
{

    /**
     * Defines the types of operations that are allowed into the group
     */
    public static enum AllowedChildsType
    {

        /**
         * Only simple message processors that are not endpoints nor
         * {@link ExtensionOperation}s
         */
        MESSAGE_PROCESSOR,

        /**
         * Only outbound endpoints
         */
        OUTBOUND_ENDPOINT,

        /**
         * Either {@link #MESSAGE_PROCESSOR} or {@link #OUTBOUND_ENDPOINT}.
         * {@link #OWN_EXTENSION_OPERATION} still excluded
         */
        MESSAGE_PROCESSOR_OR_OUTBOUND_ENDPOINT,

        /**
         * Only {@link ExtensionOperation}s defined
         * between the current {@link Extension}
         */
        OWN_EXTENSION_OPERATION,

        /**
         * Whatever
         */
        ANY
    }

    /**
     * The allowed type of child operations
     * @return a {@link org.mule.extensions.introspection.api.MuleExtensionOperationGroup.AllowedChildsType}
     */
    AllowedChildsType getAllowedChildsType();

    /**
     * The minimum amount of operations required on this group.
     * @return an int value greater or equal than zero. Zero means unbounded
     */
    int getMinOperations();

    /**
     * The maximum amount of operations allowed on this group
     * @return an int value greater or equal than zero. Zero means unbounded
     */
    int getMaxOperations();

}
