/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.streaming.object;

import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;

import java.util.Iterator;

/**
 * Specialization of {@link CursorProviderFactory} which creates {@link CursorIteratorProvider} instances
 * out of {@link Iterator} instances
 *
 * @since 4.0
 */
public interface CursorIteratorProviderFactory extends CursorProviderFactory<Iterator> {

}
