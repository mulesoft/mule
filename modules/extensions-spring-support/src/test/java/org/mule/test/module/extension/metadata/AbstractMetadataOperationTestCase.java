/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.metadata;

import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;

public abstract class AbstractMetadataOperationTestCase extends MetadataExtensionFunctionalTestCase<OperationModel> {

  public AbstractMetadataOperationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
    this.provider = resolutionType == ResolutionType.EXPLICIT_RESOLUTION ? MetadataService::getOperationMetadata
        : (metadataService, componentId, key) -> metadataService.getOperationMetadata(componentId);
  }
}
