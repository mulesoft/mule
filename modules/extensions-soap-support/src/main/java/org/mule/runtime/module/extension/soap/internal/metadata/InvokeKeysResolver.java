/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.soap.internal.metadata;

import static org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder.newKey;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.soap.WebServiceDefinition;
import org.mule.runtime.module.extension.soap.internal.runtime.connection.ForwardingSoapClient;
import org.mule.runtime.soap.api.client.metadata.SoapMetadataResolver;

import com.google.common.collect.ImmutableSet;

import java.util.List;
import java.util.Set;

/**
 * Resolves the metadata keys for a given Soap Extension {@link ForwardingSoapClient} connection.
 * <p>
 * Retrieves all the services that are available to hit and for each service will retrieve all available Operations that
 * the user can execute.
 *
 * @since 4.0
 */
public final class InvokeKeysResolver extends BaseInvokeResolver implements TypeKeysResolver {

  /**
   * {@inheritDoc}
   *
   * Resolves multi-level metadata keys with the services available to hit and for each of the services exposes all
   * available operations.
   */
  @Override
  public Set<MetadataKey> getKeys(MetadataContext context) throws MetadataResolvingException, ConnectionException {
    ForwardingSoapClient connection = getConnection(context);
    ImmutableSet.Builder<MetadataKey> keys = ImmutableSet.builder();
    connection.getAllWebServices().forEach(ws -> keys.add(buildServiceKey(connection, ws)));
    return keys.build();
  }

  private MetadataKey buildServiceKey(ForwardingSoapClient connection, WebServiceDefinition ws) {
    String serviceId = ws.getServiceId();
    SoapMetadataResolver resolver = connection.getSoapClient(serviceId).getMetadataResolver();
    MetadataKeyBuilder key = newKey(serviceId).withDisplayName(ws.getFriendlyName());
    List<String> excludedOperations = ws.getExcludedOperations();
    resolver.getAvailableOperations().stream()
        .filter(ope -> !excludedOperations.contains(ope))
        .forEach(ope -> key.withChild(newKey(ope)));
    return key.build();
  }
}
