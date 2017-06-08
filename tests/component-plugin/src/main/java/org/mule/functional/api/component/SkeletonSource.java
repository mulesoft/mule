/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api.component;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.source.MessageSource;

/**
 * Test source that provides access to the {@link Processor} set by the owner {@link org.mule.runtime.core.api.construct.Flow}.
 *
 * @since 4.0
 */
public class SkeletonSource extends AbstractAnnotatedObject implements MessageSource {

  private Processor listener;

  @Override
  public void setListener(Processor listener) {
    this.listener = listener;
  }

  public Processor getListener() {
    return listener;
  }
}
