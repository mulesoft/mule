/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.heisenberg.extension;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@Ignore
public class InvalidIgnoredSource extends Source {

  @Override
  public void onStart(SourceCallback sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
