/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
