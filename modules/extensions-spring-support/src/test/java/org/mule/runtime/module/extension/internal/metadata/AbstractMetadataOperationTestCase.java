/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static org.mule.runtime.module.extension.internal.metadata.MetadataExtensionFunctionalTestCase.ResolutionType.EXPLICIT_RESOLUTION;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.metadata.MetadataService;

public abstract class AbstractMetadataOperationTestCase extends MetadataExtensionFunctionalTestCase<OperationModel> {

  AbstractMetadataOperationTestCase(ResolutionType resolutionType) {
    super(resolutionType);
    this.provider = resolutionType == EXPLICIT_RESOLUTION ? MetadataService::getOperationMetadata
        : (metadataService, componentId, key) -> metadataService.getOperationMetadata(componentId);
  }
}
