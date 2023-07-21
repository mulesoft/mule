/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.context;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.ast.api.ArtifactAst;

import java.util.List;
import java.util.Optional;

/**
 * {@link ConfigurationComponentLocator} implementation for using when the {@link ArtifactAst} of an application has not been
 * processed yet.
 * 
 * @since 4.5
 */
public class BaseConfigurationComponentLocator implements ConfigurationComponentLocator {

  private ConfigurationComponentLocator delegate;

  @Override
  public Optional<Component> find(Location location) {
    return delegate == null ? empty() : delegate.find(location);
  }

  @Override
  public List<Component> find(ComponentIdentifier componentIdentifier) {
    return delegate == null ? emptyList() : delegate.find(componentIdentifier);
  }

  @Override
  public List<ComponentLocation> findAllLocations() {
    return delegate == null ? emptyList() : delegate.findAllLocations();
  }

  public void setDelegate(ConfigurationComponentLocator delegate) {
    this.delegate = delegate;
  }
}
