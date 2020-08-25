/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.marvel.xmen;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.util.ArrayList;
import java.util.List;

@MediaType(TEXT_PLAIN)
public class CerebroDetectNewMutants extends Source<List<Result<String, Void>>, Void> {

  @Override
  public void onStart(SourceCallback<List<Result<String, Void>>, Void> sourceCallback) throws MuleException {
    sourceCallback.handle(makeResult());
  }

  private Result<List<Result<String, Void>>, Void> makeResult() {
    List<Result<String, Void>> mutants = new ArrayList<>();
    mutants.add(Result.<String, Void>builder()
        .output("Banshee")
        .build());
    mutants.add(Result.<String, Void>builder()
        .output("Jubilee")
        .build());
    mutants.add(Result.<String, Void>builder()
        .output("Blink")
        .build());

    return Result.<List<Result<String, Void>>, Void>builder()
        .output(mutants)
        .build();
  }

  @Override
  public void onStop() {
    // Nothing to do
  }

}
