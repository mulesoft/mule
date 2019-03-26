/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.streaming.object.factory;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.internal.streaming.StreamingStrategy;

/**
 * This interface is used to flag that the implementing class is involved in some sort of streaming and to provide the
 * {@link StreamingStrategy} that it uses.
 *
 * @since 4.2
 */
@NoImplement
public interface HasStreamingStrategy {

  /**
   * 
   * @return the {@link StreamingStrategy} associated with the implementing class.
   */
  StreamingStrategy getStreamingStrategy();

}
