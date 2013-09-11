/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api.transformer;

/**
 * A interface to denote that a transformer is discoverable. A Transformer can implement this interface so that
 * when a transformation is being 'discovered' for a payload type the transformers implementing this interface
 * will be included in the search.
 */
public interface DiscoverableTransformer extends Converter
{
}
