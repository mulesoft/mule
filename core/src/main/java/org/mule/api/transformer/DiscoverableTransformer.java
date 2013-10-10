/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
