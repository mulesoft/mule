/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.api.serializer;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.MuleContextAware;
import org.mule.runtime.core.internal.serialization.JavaExternalSerializerProtocol;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.serializer.protocol.CustomJavaSerializationProtocol;

public class ArtifactObjectSerializer implements ObjectSerializer, Initialisable, MuleContextAware {

  private volatile JavaExternalSerializerProtocol javaExternalSerializerProtocol;
  private volatile CustomJavaSerializationProtocol javaInternalSerializerProtocol;
  private MuleContext muleContext;

  public ArtifactObjectSerializer(ClassLoaderRepository classLoaderRepository) {
    checkArgument(classLoaderRepository != null, "ClassLoaderRepository cannot be null");

    javaExternalSerializerProtocol = new JavaExternalSerializerProtocol();
    javaInternalSerializerProtocol = new CustomJavaSerializationProtocol(classLoaderRepository);
  }

  @Override
  public SerializationProtocol getInternalProtocol() {
    return javaInternalSerializerProtocol;
  }

  @Override
  public SerializationProtocol getExternalProtocol() {
    return javaExternalSerializerProtocol;
  }

  @Override
  public void setMuleContext(MuleContext context) {
    this.muleContext = context;
  }

  @Override
  public void initialise() throws InitialisationException {
    try {
      muleContext.getInjector().inject(javaInternalSerializerProtocol);
      muleContext.getInjector().inject(javaExternalSerializerProtocol);
    } catch (MuleException e) {
      throw new InitialisationException(e, this);
    }
  }
}
