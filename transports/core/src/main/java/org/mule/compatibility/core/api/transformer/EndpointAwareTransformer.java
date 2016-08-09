/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.api.transformer;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.endpoint.EndpointAware;
import org.mule.runtime.core.api.transformer.Transformer;

public interface EndpointAwareTransformer extends EndpointAware, Transformer {

  /**
   * The endpoint that this transformer is attached to
   * 
   * @return the endpoint associated with the transformer
   * @deprecated
   */
  @Deprecated
  ImmutableEndpoint getEndpoint();

}
