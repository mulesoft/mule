/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.petstore.extension;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.extension.api.annotation.metadata.MetadataScope;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;

@MetadataScope(outputResolver = PetstoreMetadataResolver.class)
public class SentientSource extends Source<ComponentLocation, Void> {

  public static ComponentLocation capturedLocation;

  private ComponentLocation location;

  @Override
  public void onStart(SourceCallback<ComponentLocation, Void> sourceCallback) throws MuleException {
    capturedLocation = location;
  }

  @Override
  public void onStop() {
    capturedLocation = null;
  }
}
