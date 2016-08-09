/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer;

import org.mule.compatibility.core.api.endpoint.ImmutableEndpoint;
import org.mule.compatibility.core.transport.service.TransportFactoryException;
import org.mule.compatibility.core.transport.service.TransportServiceDescriptor;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.AbstractTransformer;
import org.mule.runtime.core.transformer.TransformerUtils;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransportTransformerUtils extends TransformerUtils {

  private static Logger LOGGER = LoggerFactory.getLogger(AbstractTransformer.class);

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static boolean isSourceTypeSupportedByFirst(List<Transformer> transformers, Class clazz) {
    Transformer transformer = firstOrNull(transformers);
    return null != transformer && transformer.isSourceDataTypeSupported(DataType.fromType(clazz));
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected static interface TransformerSource {

    public List<Transformer> getTransformers() throws TransportFactoryException;
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  protected static List<Transformer> getTransformersFromSource(TransformerSource source) {
    try {
      List<Transformer> transformers = source.getTransformers();
      TransformerUtils.initialiseAllTransformers(transformers);
      return transformers;
    } catch (MuleException e) {
      LOGGER.debug(e.getMessage(), e);
      return null;
    }
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static List<Transformer> getDefaultInboundTransformers(final TransportServiceDescriptor serviceDescriptor,
                                                                final ImmutableEndpoint endpoint) {
    return getTransformersFromSource(() -> serviceDescriptor.createInboundTransformers(endpoint));
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static List<Transformer> getDefaultResponseTransformers(final TransportServiceDescriptor serviceDescriptor,
                                                                 final ImmutableEndpoint endpoint) {
    return getTransformersFromSource(() -> serviceDescriptor.createResponseTransformers(endpoint));
  }

  /**
   * @deprecated Transport infrastructure is deprecated.
   */
  @Deprecated
  public static List<Transformer> getDefaultOutboundTransformers(final TransportServiceDescriptor serviceDescriptor,
                                                                 final ImmutableEndpoint endpoint) {
    return getTransformersFromSource(() -> serviceDescriptor.createOutboundTransformers(endpoint));
  }
}
