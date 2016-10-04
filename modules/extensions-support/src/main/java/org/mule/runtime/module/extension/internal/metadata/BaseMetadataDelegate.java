/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.metadata.resolving.FailureCode.NO_DYNAMIC_TYPE_AVAILABLE;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.module.extension.internal.util.MetadataTypeUtils.isNullType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.resolving.FailureCode;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataFailure;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.extension.api.introspection.RuntimeComponentModel;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;

import java.util.List;
import java.util.stream.Stream;

/**
 * //TODO
 */
class BaseMetadataDelegate {

  protected final RuntimeComponentModel component;
  protected final MetadataResolverFactory resolverFactory;

  BaseMetadataDelegate(RuntimeComponentModel component) {
    this.component = component;
    this.resolverFactory = component.getMetadataResolverFactory();
  }

  /**
   * Uses the {@link MetadataDelegate} to resolve dynamic metadata of the component, executing internally one of the
   * {@link MetadataType} resolving components: {@link InputTypeResolver#getInputMetadata} or
   * {@link OutputTypeResolver#getOutputType}
   *
   * @param staticType static type used as default if no dynamic type is available
   * @param delegate   Delegate which performs the final invocation to the one of the metadata resolvers
   * @return a {@link MetadataResult} with the {@link MetadataType} resolved by the delegate invocation. Success if the type has
   * been successfully fetched, Failure otherwise.
   */
  protected MetadataResult<MetadataType> resolveMetadataType(boolean allowsNullType, MetadataType staticType,
                                                             MetadataDelegate delegate, String elementName) {
    try {
      MetadataType dynamicType = delegate.resolve();
      // TODO review this once MULE-10438 and MDM-21 are done
      if (isNullType(staticType) || dynamicType == null) {
        return success(staticType);
      }

      if (isNullType(dynamicType) && !allowsNullType) {
        return failure(staticType, format("An error occurred while resolving the MetadataType of the [%s]", elementName),
                       NO_DYNAMIC_TYPE_AVAILABLE,
                       "The resulting MetadataType was of NullType, but it is not a valid type for this element");
      }

      return success(dynamicType);
    } catch (Exception e) {
      return failure(staticType, e);
    }
  }

  /**
   * Merge multiple failed {@link MetadataResult} into one {@link MetadataFailure}.
   *
   * @param results the results to be merged as one
   * @return a new single {@link MetadataFailure}
   */
  protected <T> MetadataResult<T> mergeFailures(T descriptor, MetadataResult<?>... results) {
    List<MetadataResult<?>> failedResults = Stream.of(results).filter(result -> !result.isSuccess()).collect(toList());
    String messages = failedResults.stream().map(f -> f.getFailure().get().getMessage()).collect(joining(" and "));
    String stackTrace = failedResults.size() == 1 ? failedResults.get(0).getFailure().get().getReason() : "";
    FailureCode failureCode =
        failedResults.size() == 1 ? failedResults.get(0).getFailure().get().getFailureCode() : FailureCode.MULTIPLE;
    return failure(descriptor, messages, failureCode, stackTrace);
  }

  @FunctionalInterface
  protected interface MetadataDelegate {

    MetadataType resolve() throws MetadataResolvingException, ConnectionException;

  }

}
