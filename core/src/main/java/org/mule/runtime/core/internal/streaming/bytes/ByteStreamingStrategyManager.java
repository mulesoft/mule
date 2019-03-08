/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.bytes;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.internal.streaming.StreamingStrategy;

@NoImplement
public interface ByteStreamingStrategyManager {

  public CursorStreamProviderFactory getDefaultCursorProviderFactory(StreamingStrategy streamingStrategy);

}
