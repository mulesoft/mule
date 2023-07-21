/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.serializer;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.api.annotation.NoInstantiate;
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

@NoInstantiate
public final class ArtifactObjectSerializer implements ObjectSerializer, Initialisable, MuleContextAware {

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
    javaExternalSerializerProtocol.setMuleContext(context);
    javaInternalSerializerProtocol.setMuleContext(context);
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
