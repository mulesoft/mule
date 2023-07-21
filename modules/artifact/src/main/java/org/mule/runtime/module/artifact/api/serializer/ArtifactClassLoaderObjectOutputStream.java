/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.serializer;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Optional;

/**
 * Customized version of {@link ObjectOutputStream} that for each serialized object, writes the identifier of the classLoader that
 * loaded the object's class.
 * <p>
 * Is intended to be used along with {@link ArtifactClassLoaderObjectInputStream}.
 */
@NoInstantiate
public final class ArtifactClassLoaderObjectOutputStream extends ObjectOutputStream {

  private final ClassLoaderRepository classLoaderRepository;

  /**
   * Creates a new output stream.
   *
   * @param classLoaderRepository contains the registered classloaders that can be used to load serialized classes. Non null.
   * @param out                   output stream to write to
   * @throws IOException if an I/O error occurs while writing stream header
   */
  public ArtifactClassLoaderObjectOutputStream(ClassLoaderRepository classLoaderRepository, OutputStream out) throws IOException {
    super(out);
    this.classLoaderRepository = classLoaderRepository;
  }

  @Override
  protected void annotateClass(Class<?> clazz) throws IOException {
    Optional<String> id = classLoaderRepository.getId(clazz.getClassLoader());
    if (id.isPresent()) {
      this.writeInt(id.get().length());
      this.writeBytes(id.get());
    } else {
      this.writeInt(-1);
    }
  }
}
