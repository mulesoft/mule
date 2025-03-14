/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.dsl.DslResolvingContext;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;

import java.io.InputStream;
import java.util.ServiceLoader;

import org.w3c.dom.Document;

/**
 *
 * Serializer that can convert an {@link ArtifactDeclaration} into a readable and processable XML representation and from a mule
 * XML configuration file into an {@link ArtifactDeclaration} representation.
 *
 * @since 4.0
 * @deprecated Use mule-artifact-ast instead.
 */
@NoImplement
@Deprecated(forRemoval = true, since = "4.10")
public interface ArtifactDeclarationXmlSerializer {

  /**
   * Provides an instance of the default implementation of the {@link ArtifactDeclarationXmlSerializer}.
   *
   * @param context a {@link DslResolvingContext} that provides access to all the {@link ExtensionModel extensions} required for
   *                loading a given {@code artifact config} to an {@link ArtifactDeclaration}
   * @return an instance of the default implementation of the {@link ArtifactDeclarationXmlSerializer}
   */
  static ArtifactDeclarationXmlSerializer getDefault(DslResolvingContext context) {
    final var defaultArtifactDeclarationXmlSerializer =
        ServiceLoader.load(ArtifactDeclarationXmlSerializer.class, ArtifactDeclarationXmlSerializer.class.getClassLoader())
            .findFirst().get();
    defaultArtifactDeclarationXmlSerializer.setContext(context);
    return defaultArtifactDeclarationXmlSerializer;
  }

  void setContext(DslResolvingContext context);

  /**
   * Serializes an {@link ArtifactDeclaration} into an XML {@link Document}
   *
   * @param declaration {@link ArtifactDeclaration} to be serialized
   * @return an XML representation of the {@link ArtifactDeclaration}
   */
  String serialize(ArtifactDeclaration declaration);

  /**
   * Creates an {@link ArtifactDeclaration} from a given mule artifact XML configuration file.
   *
   * @param configResource the input stream with the XML configuration content.
   * @return an {@link ArtifactDeclaration} that represents the given mule configuration.
   */
  ArtifactDeclaration deserialize(InputStream configResource);

  /**
   * Creates an {@link ArtifactDeclaration} from a given mule artifact XML configuration file.
   *
   * @param name           name of the file to display a better error messages (if there are any).
   * @param configResource the input stream with the XML configuration content.
   * @return an {@link ArtifactDeclaration} that represents the given mule configuration.
   */
  ArtifactDeclaration deserialize(String name, InputStream configResource);

}
