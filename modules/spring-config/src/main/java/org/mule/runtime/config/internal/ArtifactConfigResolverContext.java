/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.dsl.api.xml.parser.XmlConfigurationDocumentLoader;
import org.mule.runtime.dsl.api.xml.parser.XmlApplicationParser;
import org.mule.runtime.dsl.api.ConfigResource;

import java.util.Map;
import java.util.Set;

/**
 * Provides a context for the {@link ArtifactConfigResolver} to hook up the input for the resolver.
 *
 * @since 4.1.4, 4.2.0
 */
public interface ArtifactConfigResolverContext {

  /**
   * @return {@link Map} of artifact properties.
   */
  Map<String, String> getArtifactProperties();

  /**
   * @return {@link ConfigResource[]} of config resources.
   */
  ConfigResource[] getArtifactConfigResources();

  /**
   * @return the artifact name.
   */
  String getArtifactName();

  /**
   * @return {@link XmlConfigurationDocumentLoader} to load configuration resources documents.
   */
  XmlConfigurationDocumentLoader getXmlConfigurationDocumentLoader();

  /**
   * @return {@link ExtensionModel ExtensionModels} to parse the configuration files.
   */
  Set<ExtensionModel> getExtensions();

  /**
   * @return {@link XmlApplicationParser} to parse the configuration files.
   */
  XmlApplicationParser getXmlApplicationParser();

  /**
   * @return {@link ClassLoader} to load imported configuration files as resource.
   */
  ClassLoader getExecutionClassLoader();
}
