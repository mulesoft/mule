/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.api.cache;

import org.mule.runtime.ast.api.ComponentAst;

import java.util.Optional;

/**
 * Allows to retrieve the {@link MetadataCacheId} for Configs. It's used since the
 * {@link org.mule.runtime.extension.api.component.ComponentParameterization} has no information of the configs, including its
 * child elements.
 *
 * @since 4.5
 */
public interface ConfigurationMetadataCacheIdGenerator {

  /**
   * @param configName    the configuration name to get the {@link MetadataCacheId} for
   * @param justProviders if true, it will return the id corresponding to the calculation of this config internal/child elements.
   *                      If false, it will return the id of the whole config.
   * @return the {{@link MetadataCacheId} corresponding to the configuration}. In case the config was not provided previously
   *         using {@link #addConfiguration(ComponentAst) AddConfiguration}. This could happen, for example, when just performing
   *         a Type Resolution, without Propagation.
   */
  Optional<MetadataCacheId> getConfigMetadataCacheId(String configName, boolean justProviders);

  /**
   * Adds the config to be considered when getting the Cache Ids. If the config was already set before, the values will be
   * recalculated.
   */
  void addConfiguration(ComponentAst configAst);

}
