/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.registry;

import org.mule.api.transformer.Transformer;

/**
 * A TransformResolver is used to find transformers that match a certain criteria in the registry.  Implementations
 * of this class will use some or all of the information passed in to discover a matching transformer.
 * <p/>
 * Any implementations of this class must be registered with the registry before it will get picked up. Typically this
 * is done using the registry-bootstrap.properties.
 */
public interface TransformerResolver
{
    enum RegistryAction
    {
        ADDED,
        REMOVED
    }

    /**
     * Responsible for finding a transformer with the given criteria.  Note that if a transformer is not found
     * null should be return, an exception must NOT be thrown.
     *
     * @param criteria the transform critera
     * @return a transformer from the registry that matches the criteria or null if a transformer was not found
     * @throws ResolverException Only thrown if an exception is thrown during the search, this exception will just be a wrapper
     */
    Transformer resolve(TransformCriteria criteria) throws ResolverException;

    /**
     * A callback that is called when a transformer is registered or unregistered from the registry.  This is used
     * in situations where the resolver caches transformers and the cache needs to be updated
     *
     * @param transformer    the transformer that has changed
     * @param registryAction whether the transformer was added or removed
     */
    void transformerChange(Transformer transformer, RegistryAction registryAction);
}
