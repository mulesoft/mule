/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
