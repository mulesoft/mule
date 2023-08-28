/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
