/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.sampledata;

import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.sampledata.SampleDataResult;
import org.mule.runtime.app.declaration.api.ComponentElementDeclaration;
import org.mule.runtime.core.api.connector.ConnectionManager;
import org.mule.runtime.core.internal.message.DefaultMessageBuilder;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;
import org.mule.runtime.module.tooling.internal.utils.ArtifactHelper;

import java.util.Collections;

public class SampleDataExecutor {

  protected ConnectionManager connectionManager;
  protected ReflectionCache reflectionCache;
  protected ArtifactHelper artifactHelper;

  public SampleDataExecutor(ConnectionManager connectionManager, ReflectionCache reflectionCache, ArtifactHelper artifactHelper) {
    this.connectionManager = connectionManager;
    this.reflectionCache = reflectionCache;
    this.artifactHelper = artifactHelper;
  }

  public SampleDataResult getSampleData(ComponentModel componentModel, ComponentElementDeclaration componentElementDeclaration) {
    // TODO pending SDK sample data implementation
    //  add validations for missing required params/config/connection
    DefaultMessageBuilder builder = new DefaultMessageBuilder();
    builder.value(Collections.singletonMap("id", "x"));
    builder.attributesValue(Collections.singletonMap("time", "now"));
    return SampleDataResult.resultFrom(builder.build());
  }
}
