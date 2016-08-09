/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.File;

/**
 * Object representation of a maven artifact.
 *
 * @since 4.0
 */
public class MavenArtifact {

  public static final String DOT_CHARACTER = ".";
  public static final String MAVEN_COMPILE_SCOPE = "compile";
  public static final String MAVEN_TEST_SCOPE = "test";
  public static final String MAVEN_PROVIDED_SCOPE = "provided";
  public static final String MAVEN_DEPENDENCIES_DELIMITER = ":";

  public static final String POM_TYPE = "pom";

  private final String groupId;
  private final String artifactId;
  private final String type;
  private final String version;
  private final String scope;

  /**
   * Creates a maven representation of an artifact with its different attributes
   *
   * @param groupId cannot be null or empty
   * @param artifactId cannot be null or empty
   * @param type cannot be null or empty
   * @param version can be empty or null,
   * @param scope cannot be null or empty
   * @throws IllegalArgumentException if any of the mandatory attributes is null
   */
  private MavenArtifact(final String groupId, final String artifactId, final String type, final String version, String scope) {
    checkNullOrEmpty(groupId, "groupId");
    checkNullOrEmpty(artifactId, "artifactId");
    checkNullOrEmpty(type, "type");
    checkNullOrEmpty(scope, "scope");

    this.groupId = groupId;
    this.artifactId = artifactId;
    this.type = type;
    this.version = version;
    this.scope = scope;
  }

  private void checkNullOrEmpty(String value, String param) {
    if (isEmpty(value)) {
      throw new IllegalArgumentException(param + " cannot be null or empty");
    }
  }

  public String getGroupId() {
    return groupId;
  }

  public String getGroupIdAsPath() {
    return getGroupId().replace(DOT_CHARACTER, File.separator);
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getType() {
    return type;
  }

  public String getVersion() {
    return version;
  }

  public String getScope() {
    return scope;
  }

  public boolean isCompileScope() {
    return MAVEN_COMPILE_SCOPE.equals(scope);
  }

  public boolean isTestScope() {
    return MAVEN_TEST_SCOPE.equals(scope);
  }

  public boolean isProvidedScope() {
    return MAVEN_PROVIDED_SCOPE.equals(scope);
  }

  public boolean isPomType() {
    return POM_TYPE.endsWith(type);
  }

  @Override
  public String toString() {
    return groupId + MAVEN_DEPENDENCIES_DELIMITER + artifactId + MAVEN_DEPENDENCIES_DELIMITER + type
        + MAVEN_DEPENDENCIES_DELIMITER + (!isEmpty(version) ? version : "") + MAVEN_DEPENDENCIES_DELIMITER + scope;
  }

  /**
   * Identity is defined by groupId, artifactId, type and version only. Scope is not considered due to the same
   * {@link MavenArtifact} could be referenced from different scopes and the scope is how the dependencies is going to be treated
   * but when working with the whole dependency graph including duplicates the scope shouldn't be part of the identity.
   *
   * @param o another {@link MavenArtifact} to compare with
   * @return true if the two instances are equals
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    MavenArtifact that = (MavenArtifact) o;

    if (!groupId.equals(that.groupId)) {
      return false;
    }
    if (!artifactId.equals(that.artifactId)) {
      return false;
    }
    if (!type.equals(that.type)) {
      return false;
    }
    if (version != null ? !version.equals(that.version) : that.version != null) {
      return false;
    }
    return true;

  }

  @Override
  public int hashCode() {
    int result = groupId.hashCode();
    result = 31 * result + artifactId.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + (version != null ? version.hashCode() : 0);
    return result;
  }

  public static MavenArtifactBuilder builder() {
    return new MavenArtifactBuilder();
  }

  /**
   * Builder for {@link MavenArtifact}
   */
  public static final class MavenArtifactBuilder {

    private String groupId;
    private String artifactId;
    private String type;
    private String version;
    private String scope;

    public MavenArtifactBuilder withGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public MavenArtifactBuilder withArtifactId(String artifactId) {
      this.artifactId = artifactId;
      return this;
    }

    public MavenArtifactBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public MavenArtifactBuilder withVersion(String version) {
      this.version = version;
      return this;
    }

    public MavenArtifactBuilder withScope(String scope) {
      this.scope = scope;
      return this;
    }

    public MavenArtifact build() {
      return new MavenArtifact(groupId, artifactId, type, version, scope);
    }
  }
}
