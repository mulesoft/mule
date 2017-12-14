/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.artifact.internal.descriptor;

import static org.mule.runtime.api.deployment.meta.Product.getProductByName;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleManifest.getProductName;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;

/**
 * Validator for {@link ArtifactDescriptor#getRequiredProduct()}.
 *
 * @since 4.1
 */
public class MuleProductArtifactDescriptorValidator implements ArtifactDescriptorValidator {

  @Override
  public void validate(ArtifactDescriptor descriptor) {
    Product requiredProduct = descriptor.getRequiredProduct();
    Product runtimeProduct = getProductByName(getProductName());
    if (!runtimeProduct.supports(requiredProduct)) {
      throw new MuleRuntimeException(createStaticMessage("The artifact %s requires a different runtime. The artifact required runtime is %s and the runtime is %s",
                                                         descriptor.getName(), descriptor.getRequiredProduct().name(),
                                                         runtimeProduct.name()));
    }
  }

}
