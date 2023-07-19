/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.streaming.object;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.streaming.object.CursorIteratorProvider;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;

import java.util.Iterator;

/**
 * Specialization of {@link CursorProviderFactory} which creates {@link CursorIteratorProvider} instances out of {@link Iterator}
 * instances
 *
 * @since 4.0
 */
@NoImplement
public interface CursorIteratorProviderFactory extends CursorProviderFactory<Iterator> {

}
