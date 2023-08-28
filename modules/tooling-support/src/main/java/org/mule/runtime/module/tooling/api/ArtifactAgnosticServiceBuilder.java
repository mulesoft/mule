/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.tooling.api;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoImplement
@NoExtend
public interface ArtifactAgnosticServiceBuilder<T extends ArtifactAgnosticServiceBuilder, S> {

  /**
   * Adds a dependency needed by the artifact that must be included in order to build the artifact.
   * <p>
   * If the dependency is a regular jar file, it will be made available to all extensions since the only possible jar dependency
   * that may be added are specific clients jar for executing the created service like jdbc drivers or JMS clients.
   *
   * @param groupId         group id of the artifact
   * @param artifactId      artifact id of the artifact
   * @param artifactVersion version of the artifact
   * @param classifier      classifier of the artifact
   * @param type            type of the artifact
   * @return the builder
   */
  T addDependency(String groupId, String artifactId, String artifactVersion, String classifier, String type);

  /**
   * Adds a dependency needed by the artifact that must be included in order to build the artifact.
   * <p>
   * If the dependency is a regular jar file, it will be made available to all extensions since the only possible jar dependency
   * that may be added are specific clients jar for executing the created service like jdbc drivers or JMS clients.
   *
   * @param dependency {@link Dependency} to be added to the artifact.
   * @return the builder
   */
  T addDependency(Dependency dependency);

  /**
   * Configures the declaration of mule components that represent this artifact.
   *
   * @param artifactDeclaration set of mule components
   * @return the builder
   */
  T setArtifactDeclaration(ArtifactDeclaration artifactDeclaration);

  /**
   * Sets the artifact properties for the session that would be considered for resolving properties placeholder.
   *
   * @param artifactProperties to be set when generating the artifact.
   * @return the builder.
   */
  T setArtifactProperties(Map<String, String> artifactProperties);

  /**
   * Creates a {@code S service} with the provided configuration
   *
   * @return the created service
   */
  S build();

  class Exclusion {

    private String artifactId;
    private String groupId;

    public Exclusion() {}

    public String getArtifactId() {
      return this.artifactId;
    }

    public String getGroupId() {
      return this.groupId;
    }

    public void setArtifactId(String artifactId) {
      this.artifactId = artifactId;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

  }

  static class Dependency {

    private String groupId;
    private String artifactId;
    private String version;
    private String type = "jar";
    private String classifier;
    private String scope;
    private String systemPath;
    private List<Exclusion> exclusions;
    private String optional;

    public Dependency() {}

    public void addExclusion(Exclusion exclusion) {
      this.getExclusions().add(exclusion);
    }

    public String getArtifactId() {
      return this.artifactId;
    }

    public String getClassifier() {
      return this.classifier;
    }

    public List<Exclusion> getExclusions() {
      if (this.exclusions == null) {
        this.exclusions = new ArrayList();
      }

      return this.exclusions;
    }

    public String getGroupId() {
      return this.groupId;
    }

    public String getOptional() {
      return this.optional;
    }

    public String getScope() {
      return this.scope;
    }

    public String getSystemPath() {
      return this.systemPath;
    }

    public String getType() {
      return this.type;
    }

    public String getVersion() {
      return this.version;
    }

    public void removeExclusion(Exclusion exclusion) {
      this.getExclusions().remove(exclusion);
    }

    public void setArtifactId(String artifactId) {
      this.artifactId = artifactId;
    }

    public void setClassifier(String classifier) {
      this.classifier = classifier;
    }

    public void setExclusions(List<Exclusion> exclusions) {
      this.exclusions = exclusions;
    }

    public void setGroupId(String groupId) {
      this.groupId = groupId;
    }

    public void setOptional(String optional) {
      this.optional = optional;
    }

    public void setScope(String scope) {
      this.scope = scope;
    }

    public void setSystemPath(String systemPath) {
      this.systemPath = systemPath;
    }

    public void setType(String type) {
      this.type = type;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public boolean isOptional() {
      return this.optional != null ? Boolean.parseBoolean(this.optional) : false;
    }

    public void setOptional(boolean optional) {
      this.optional = String.valueOf(optional);
    }

    public String toString() {
      return "Dependency {groupId=" + this.groupId + ", artifactId=" + this.artifactId + ", version=" + this.version + ", type="
          + this.type + "}";
    }

  }

}
