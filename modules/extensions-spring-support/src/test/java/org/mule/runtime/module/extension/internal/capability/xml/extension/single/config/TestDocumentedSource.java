/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.extension.single.config;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

import java.io.InputStream;

/**
 * Source Documentation
 */
@Alias("source-alias")
public class TestDocumentedSource extends Source<InputStream, String> {

  @Override
  public void onStart(SourceCallback sourceCallback) throws MuleException {

  }

  @Override
  public void onStop() {

  }
}
