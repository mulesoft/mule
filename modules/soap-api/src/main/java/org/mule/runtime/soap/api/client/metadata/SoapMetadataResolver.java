/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.soap.api.client.metadata;

import org.mule.runtime.api.metadata.MetadataResolvingException;

import java.util.Set;

/**
 * An object that is in charge of resolving {@link SoapOperationMetadata} for different operations.
 *
 * @since 4.0
 */
public interface SoapMetadataResolver {

  /**
   * @param operation the name of the operation that the metadata is going to fetched for
   * @return a new {@link SoapOperationMetadata} with the INPUT body type, headers type and attachments type.
   * @throws MetadataResolvingException in any error case.
   */
  SoapOperationMetadata getInputMetadata(String operation) throws MetadataResolvingException;

  /**
   * @param operation the name of the operation that the metadata is going to fetched for
   * @return a new {@link SoapOperationMetadata} with the OUTPUT body type, headers type and attachments type.
   * @throws MetadataResolvingException in any error case.
   */
  SoapOperationMetadata getOutputMetadata(String operation) throws MetadataResolvingException;

  /**
   * @return a {@link Set} with the available operations names for the specified service.
   */
  Set<String> getAvailableOperations();
}
