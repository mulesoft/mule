/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.introspection.api;

import java.util.List;

/**
 * A scope is a special kind of {@link ExtensionOperation}
 * which encloses groups of child operations. Scopes are operations themselves, but they also include
 * other operations that they might or might not execute.
 * <p/>
 * These child operations are organized into groups, in order to define sequence and restrict the kind of
 * elements that are valid in each case
 * <p/>
 * TODO: This whole concept of scope and groups is still far from defined. We need to think this through more
 *
 * @since 1.0
 */
public interface ExtensionScope extends ExtensionOperation
{

    /**
     * Returns a {@link java.util.List} with the
     * {@link org.mule.extensions.introspection.api.MuleExtensionOperationGroup}s.
     * <p/>
     * Ordering in this list is not random. It defines the order in which the groups are expected.
     * <p/>
     * This list is immutable, not {@code null} and will have at least one element
     *
     * @return a {@link java.util.List} with {@link org.mule.extensions.introspection.api.MuleExtensionOperationGroup}
     */
    List<MuleExtensionOperationGroup> getGroups();

}
