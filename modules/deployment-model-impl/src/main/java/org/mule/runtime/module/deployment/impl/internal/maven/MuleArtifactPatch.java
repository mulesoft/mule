/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
