/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;

/**
 * Configuration builders that implements this interface will receive a reference to the context of the domain they belong to.
 */
public interface ParentMuleContextAwareConfigurationBuilder extends ConfigurationBuilder {

  /**
   * @param parentContext MuleContext of the domain.
   */
  void setParentContext(MuleContext parentContext, ArtifactAst parentAst);

}
