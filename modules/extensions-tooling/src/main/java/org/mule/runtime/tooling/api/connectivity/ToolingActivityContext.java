/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.api.connectivity;

import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.api.annotation.Experimental;
import org.mule.runtime.api.meta.model.ExtensionModel;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Experimental
public final class ToolingActivityContext {

  public static class Builder {

    private ClassLoader classLoader;
    private Set<ExtensionModel> additionalExtensionModels = new HashSet<>();
    private Map<String, Object> parameters = new LinkedHashMap<>();

    public Builder withClassloader(ClassLoader classLoader) {
      checkArgument(classLoader != null, "Classloader cannot be null");
      return this;
    }

    public Builder withExtensionModel(ExtensionModel extensionModel) {
      additionalExtensionModels.add(extensionModel);
      return this;
    }

    public Builder withExtensionModels(Collection<ExtensionModel> extensionModels) {
      additionalExtensionModels.addAll(extensionModels);
      return this;
    }

    public Builder withParameter(String key, Object value) {
      checkArgument(isBlank(key), "key cannot be null nor blank");
      parameters.put(key, value);

      return this;
    }

    public Builder withParameters(Map<String, Object> parameters) {
      parameters.putAll(parameters);
      return this;
    }

    public ToolingActivityContext build() {
      checkState(classLoader != null, "Classloader must be set");
      return new ToolingActivityContext(unmodifiableSet(additionalExtensionModels), unmodifiableMap(parameters));
    }
  }

  private final Set<ExtensionModel> additionalExtensionModels;
  private final Map<String, Object> parameters;

  private ToolingActivityContext(Set<ExtensionModel> additionalExtensionModels, Map<String, Object> parameters) {
    this.additionalExtensionModels = additionalExtensionModels;
    this.parameters = parameters;
  }

  public Map<String, Object> getParameters() {
    return parameters;
  }

  public Set<ExtensionModel> getAdditionalExtensionModels() {
    return additionalExtensionModels;
  }
}
