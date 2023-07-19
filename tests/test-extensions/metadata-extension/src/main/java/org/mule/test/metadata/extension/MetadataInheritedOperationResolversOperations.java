/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.metadata.extension;

import static org.mule.runtime.extension.api.annotation.param.MediaType.ANY;

import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Content;
import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.runtime.extension.api.annotation.param.Optional;

public class MetadataInheritedOperationResolversOperations extends MetadataOperationsParent {

  @MediaType(value = ANY, strict = false)
  public Object shouldInheritOperationParentResolvers(@org.mule.sdk.api.annotation.param.Connection MetadataConnection connection,
                                                      @MetadataKeyId String type,
                                                      @Optional @Content Object content) {
    return null;
  }

}
