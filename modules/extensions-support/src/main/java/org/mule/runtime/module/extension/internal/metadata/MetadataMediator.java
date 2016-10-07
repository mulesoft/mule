/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.metadata.descriptor.builder.MetadataDescriptorBuilder.componentDescriptor;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.mergeResults;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataProvider;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.InputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.OutputMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.InputTypeResolver;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.OutputTypeResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.Content;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.introspection.metadata.MetadataResolverFactory;
import org.mule.runtime.extension.api.introspection.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.introspection.property.MetadataKeyPartModelProperty;

import java.util.List;

/**
 * Resolves a Component's Metadata by coordinating the several moving parts that are affected by the Metadata fetching process, so
 * that such pieces can remain decoupled.
 * <p/>
 * This mediator will coordinate the resolvers: {@link MetadataResolverFactory}, {@link TypeKeysResolver},
 * {@link InputTypeResolver} and {@link OutputTypeResolver}, and the descriptors that represent their results:
 * {@link ComponentMetadataDescriptor}, {@link OutputMetadataDescriptor} and {@link TypeMetadataDescriptor}
 *
 * @since 4.0
 */
public class MetadataMediator {

  private final ComponentModel component;
  private final List<ParameterModel> metadataKeyParts;
  private final MetadataKeyIdObjectResolver keyIdObjectResolver;
  private final MetadataKeysDelegate keysDelegate;
  private final MetadataOutputDelegate outputDelegate;
  private final MetadataInputDelegate inputDelegate;

  public MetadataMediator(ComponentModel componentModel) {
    this.component = componentModel;
    this.metadataKeyParts = getMetadataKeyParts(componentModel);
    this.keyIdObjectResolver = new MetadataKeyIdObjectResolver(componentModel, metadataKeyParts);
    this.keysDelegate = new MetadataKeysDelegate(componentModel, metadataKeyParts);

    this.outputDelegate = new MetadataOutputDelegate(componentModel);
    this.inputDelegate = new MetadataInputDelegate(componentModel);
  }

  /**
   * Resolves the list of types available for the Content or Output of the associated {@link MetadataKeyProvider} Component,
   * representing them as a list of {@link MetadataKey}.
   * <p>
   * If no {@link MetadataKeyId} is present in the component's input parameters, then a {@link NullMetadataKey} is returned.
   * Otherwise, the {@link TypeKeysResolver#getKeys} associated with the current Component will be invoked to obtain
   * the keys
   *
   * @param context current {@link MetadataContext} that will be used by the {@link TypeKeysResolver}
   * @return Successful {@link MetadataResult} if the keys are obtained without errors Failure {@link MetadataResult} when no
   * Dynamic keys are a available or the retrieval fails for any reason
   */
  public MetadataResult<MetadataKeysContainer> getMetadataKeys(MetadataContext context) {
    return keysDelegate.getMetadataKeys(context);
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} without a key specified, this is
   * for the cases when the {@link ComponentModel} doesn't have a {@link MetadataKeyId} associated or the cases when the
   * {@link ComponentModel}s {@link MetadataKeyId} type has a default value to be built. (For multilevel {@link MetadataKey}s,
   * all the part members must have a default value)
   *
   * @return a successful {@link MetadataResult} of {@link ComponentMetadataDescriptor} with the Metadata representation of the
   * component, a failure {@link MetadataResult} if an error occur.
   */
  public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context) {
    try {
      if (metadataKeyParts.isEmpty()) {
        return getMetadata(context, new NullMetadataKey());
      }
      Object resolvedKey = keyIdObjectResolver.resolve();
      return getMetadata(context, resolvedKey);
    } catch (MetadataResolvingException e) {
      return failure(e, e.getFailure());
    }
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} using the specified
   * {@link MetadataKey}
   * <p>
   * If Component's {@link Content} parameter has a {@link InputTypeResolver} associated or its Output has a
   * {@link OutputTypeResolver} associated that can be used to resolve dynamic {@link MetadataType}, then the
   * {@link ComponentMetadataDescriptor} will contain those Dynamic types instead of the static type declaration.
   *
   * @param context current {@link MetadataContext} that will be used by the metadata resolvers.
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved, used both for input and output types
   * @return Successful {@link MetadataResult} if the MetadataTypes are resolved without errors Failure {@link MetadataResult}
   * when the Metadata retrieval of any element fails for any reason
   */
  public MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context, MetadataKey key) {
    try {
      Object resolvedKey = keyIdObjectResolver.resolve(key);
      return getMetadata(context, resolvedKey);
    } catch (MetadataResolvingException e) {
      return failure(e, e.getFailure());
    }
  }

  /**
   * Resolves the {@link ComponentMetadataDescriptor} for the associated {@link MetadataProvider} Component using static and
   * dynamic resolving of the Component parameters, attributes and output.
   * <p>
   * If Component's {@link Content} parameter has a {@link InputTypeResolver} associated or its Output has a
   * {@link OutputTypeResolver} associated that can be used to resolve dynamic {@link MetadataType}, then the
   * {@link ComponentMetadataDescriptor} will contain those Dynamic types instead of the static type declaration.
   *
   * @param context current {@link MetadataContext} that will be used by the {@link InputTypeResolver} and
   *                {@link OutputTypeResolver}
   * @param key     {@link MetadataKey} of the type which's structure has to be resolved, used both for input and output types
   * @return Successful {@link MetadataResult} if the MetadataTypes are resolved without errors Failure {@link MetadataResult}
   * when the Metadata retrieval of any element fails for any reason
   */
  private MetadataResult<ComponentMetadataDescriptor> getMetadata(MetadataContext context, Object key) {
    MetadataResult<OutputMetadataDescriptor> output = outputDelegate.getOutputMetadataDescriptor(context, key);
    MetadataResult<InputMetadataDescriptor> input = inputDelegate.getInputMetadataDescriptors(context, key);
    ComponentMetadataDescriptor componentDescriptor = componentDescriptor(component.getName())
        .withInputDescriptor(input)
        .withOutputDescriptor(output)
        .build();

    return mergeResults(componentDescriptor, output, input);
  }

  private List<ParameterModel> getMetadataKeyParts(ComponentModel componentModel) {
    return componentModel.getParameterModels().stream()
        .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .collect(toList());
  }

}
