/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
