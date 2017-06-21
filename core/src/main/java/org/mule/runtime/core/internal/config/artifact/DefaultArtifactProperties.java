/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config.artifact;

import static java.util.Collections.unmodifiableMap;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.artifact.ArtifactProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of {@link ArtifactProperties}.
 *
 * @since 4.0
 */
public class DefaultArtifactProperties implements ArtifactProperties {

  private final Map<Object, Object> configProperties;
  // TODO MULE-11679 - TBD if this properties will be kept or will be removed once we define the placeholder approach for spring
  // integration module.
  private final Map<Object, Object> springProperties;
  private final Map<Object, Object> systemProperties;
  private final Map<Object, Object> artifactProperties;
  private final Map<String, String> environmentProperties;
  private final Map<Object, Object> mergeProperties;

  /**
   * Creates a new instance of {@link DefaultArtifactProperties}.
   * <p>
   * The system properties and environment properties will be constrained to the ones available in those scopes at the time this
   * constructor is called.
   * 
   * @param configProperties the properties defined in the mule configuration files using global property component.
   * @param artifactProperties the artifact properties.
   * @param springProperties the spring properties defined as property placeholders.
   */
  public DefaultArtifactProperties(Map<Object, Object> configProperties,
                                   Map<Object, Object> springProperties,
                                   Map<Object, Object> artifactProperties) {
    checkArgument(configProperties != null, "configProperties cannot be null");
    checkArgument(springProperties != null, "springProperties cannot be null");
    checkArgument(artifactProperties != null, "artifactProperties cannot be null");
    this.configProperties = configProperties;
    this.artifactProperties = artifactProperties;
    this.springProperties = springProperties;
    this.systemProperties = unmodifiableMap(System.getProperties());
    this.environmentProperties = unmodifiableMap(System.getenv());
    HashMap<Object, Object> mergeProperties = new HashMap<>();
    mergeProperties.putAll(this.configProperties);
    mergeProperties.putAll(this.springProperties);
    mergeProperties.putAll(this.artifactProperties);
    mergeProperties.putAll(this.environmentProperties);
    mergeProperties.putAll(this.systemProperties);
    this.mergeProperties = unmodifiableMap(mergeProperties);
  }

  @Override
  public <T, K> T getProperty(K key) {
    return (T) mergeProperties.get(key);
  }

  @Override
  public Set<Object> getPropertyNames() {
    return mergeProperties.keySet();
  }

  @Override
  public Map<Object, Object> toImmutableMap() {
    return mergeProperties;
  }

}
