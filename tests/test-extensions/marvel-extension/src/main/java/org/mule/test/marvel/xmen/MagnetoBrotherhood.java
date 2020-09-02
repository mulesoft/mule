/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.xmen;

import static java.util.Arrays.asList;
import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import java.util.Iterator;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@MediaType(ANY)
public class MagnetoBrotherhood extends Source<Iterator, Void> {

  @Override
  public void onStart(SourceCallback<Iterator, Void> sourceCallback) throws MuleException {
    sourceCallback.handle(makeResult());
  }

  private Result<Iterator, Void> makeResult() {
    return Result.<Iterator, Void>builder()
        .output(asList("QuickSilver", "Scarlet Witch", "Toad", "MasterMind", "Mystique", "Blob").iterator())
        .build();
  }

  @Override
  public void onStop() {
    // Nothing to do
  }

}
