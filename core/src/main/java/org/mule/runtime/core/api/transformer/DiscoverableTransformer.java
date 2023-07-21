/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.transformer;

/**
 * A interface to denote that a transformer is discoverable. A Transformer can implement this interface so that when a
 * transformation is being 'discovered' for a payload type the transformers implementing this interface will be included in the
 * search.
 */
public interface DiscoverableTransformer extends Converter {
}
