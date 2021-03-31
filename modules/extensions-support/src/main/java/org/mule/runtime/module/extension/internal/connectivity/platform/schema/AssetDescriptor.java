/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.connectivity.platform.schema;

import java.util.Objects;

public class AssetDescriptor {

  private String groupId;
  private String assetId;
  private String version;

  public AssetDescriptor(String groupId, String assetId, String version) {
    this.groupId = groupId;
    this.assetId = assetId;
    this.version = version;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getAssetId() {
    return assetId;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AssetDescriptor that = (AssetDescriptor) o;
    return Objects.equals(groupId, that.groupId) && Objects.equals(assetId, that.assetId) && Objects.equals(version, that.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, assetId, version);
  }
}
