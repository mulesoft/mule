/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.util;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.context.MuleContextAware;

/**
 * Simple service to close streams of different types.
 */
@NoImplement
public interface StreamCloserService extends MuleContextAware {

  void closeStream(Object stream);

}
