/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.deployment.impl.internal.maven;

import java.util.List;

public class MuleArtifactPatch {

  private List<String> affectedVersions;
  private String path;

  public List<String> getAffectedVersions() {
    return affectedVersions;
  }

  public void setAffectedVersions(List<String> affectedVersions) {
    this.affectedVersions = affectedVersions;
  }

  public String getPatch() {
    return path;
  }

  public void setPatch(String path) {
    this.path = path;
  }
}
