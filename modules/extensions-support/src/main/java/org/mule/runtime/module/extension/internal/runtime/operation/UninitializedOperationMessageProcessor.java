/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.operation.OperationModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataResolvingException;
import org.mule.runtime.api.metadata.descriptor.ComponentMetadataDescriptor;
import org.mule.runtime.api.metadata.descriptor.TypeMetadataDescriptor;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.value.Value;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.policy.PolicyManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.operation.ExecutionContext;
import org.mule.runtime.extension.api.values.ValueResolvingException;
import org.mule.runtime.internal.dsl.DslConstants;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParameterValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.google.common.collect.ImmutableMap;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class UninitializedOperationMessageProcessor extends OperationMessageProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(UninitializedOperationMessageProcessor.class.getName());

  private OperationMessageProcessor operationMessageProcessorDelegate;
  private Map<String, ValueResolver> resolvedParameters;

  public UninitializedOperationMessageProcessor(ExtensionModel extension,
                                                OperationModel operation,
                                                PolicyManager policyManager,
                                                Registry registry,
                                                OperationMessageProcessor operationMessageProcessorDelegate,
                                                Map<String, ValueResolver> resolvedParameters,
                                                MuleContext muleContext,
                                                ConfigurationProvider configurationProvider) {

    super(extension,
          operation,
          configurationProvider,
          operationMessageProcessorDelegate.target,
          operationMessageProcessorDelegate.targetValue,
          operationMessageProcessorDelegate.resolverSet,
          null,
          operationMessageProcessorDelegate.retryPolicyTemplate,
          muleContext.getExtensionManager(),
          policyManager,
          registry.lookupByType(ReflectionCache.class).get());

    this.operationMessageProcessorDelegate = operationMessageProcessorDelegate;
    this.resolvedParameters = resolvedParameters;
  }

  @Override
  protected void doInitialise() throws InitialisationException {
    //Do nothing
  }

  @Override
  public ReturnDelegate createReturnDelegate() {
    return operationMessageProcessorDelegate.createReturnDelegate();
  }

  @Override
  public TargetReturnDelegate getTargetReturnDelegate() {
    return operationMessageProcessorDelegate.getTargetReturnDelegate();
  }

  @Override
  public ValueReturnDelegate getValueReturnDelegate() {
    return operationMessageProcessorDelegate.getValueReturnDelegate();
  }

  @Override
  public boolean isTargetPresent() {
    return operationMessageProcessorDelegate.isTargetPresent();
  }

  @Override
  public Optional<String> getTarget() {
    return operationMessageProcessorDelegate.getTarget();
  }

  @Override
  public void doStart() throws MuleException {
    operationMessageProcessorDelegate.doStart();
  }

  @Override
  public void doStop() throws MuleException {
    operationMessageProcessorDelegate.doStop();
  }

  @Override
  public void doDispose() {
    operationMessageProcessorDelegate.doDispose();
  }

  @Override
  public ExecutionMediator createExecutionMediator() {
    return operationMessageProcessorDelegate.createExecutionMediator();
  }

  @Override
  public ParameterValueResolver getParameterValueResolver() {
    return operationMessageProcessorDelegate.getParameterValueResolver();
  }

  @Override
  public void resolveParameters(CoreEvent.Builder eventBuilder,
                                BiConsumer<Map<String, Supplier<Object>>, ExecutionContext> afterConfigurer)
      throws MuleException {
    operationMessageProcessorDelegate.resolveParameters(eventBuilder, afterConfigurer);
  }

  @Override
  public void disposeResolvedParameters(ExecutionContext<OperationModel> executionContext) {
    operationMessageProcessorDelegate.disposeResolvedParameters(executionContext);
  }


  @Override
  protected Map<String, Object> getResolutionResult(CoreEvent event, Optional<ConfigurationInstance> configuration)
    throws MuleException {

    ValueResolvingContext valueResolvingContext = ValueResolvingContext.builder(event).build();
    Object config = resolverSet.resolve(valueResolvingContext).asMap().get(CONFIG_ATTRIBUTE_NAME);

    Map resolvedParametersMap = resolvedParameters.entrySet().stream()
        .filter(e -> e.getKey().equals(CONFIG_ATTRIBUTE_NAME))
        .collect(toMap(identity(), value -> {
          try {
            return value.getValue().resolve(valueResolvingContext);
          } catch (MuleException e) {
            LOGGER.error("Could not resolve value [" + value.getKey() + "] value: " + e.getMessage(), e);
            return null;
          }
        }));

    return ImmutableMap.<String, Object>builder().put(CONFIG_ATTRIBUTE_NAME, config).putAll(resolvedParametersMap).build();
  }

  @Override
  public void setMuleContext(MuleContext context) {
    operationMessageProcessorDelegate.setMuleContext(context);
  }

  @Override
  public MetadataResult<MetadataKeysContainer> getMetadataKeys() throws MetadataResolvingException {
    return operationMessageProcessorDelegate.getMetadataKeys();
  }

  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getMetadata() throws MetadataResolvingException {
    return operationMessageProcessorDelegate.getMetadata();
  }

  @Override
  public MetadataResult<ComponentMetadataDescriptor<OperationModel>> getMetadata(MetadataKey key)
      throws MetadataResolvingException {
    return operationMessageProcessorDelegate.getMetadata(key);
  }

  @Override
  public Set<Value> getValues(String parameterName) throws ValueResolvingException {
    return operationMessageProcessorDelegate.getValues(parameterName);
  }

  @Override
  public ExtensionModel getExtensionModel() {
    return operationMessageProcessorDelegate.getExtensionModel();
  }

  @Override
  public List<ValueProviderModel> getModels(String providerName) {
    return operationMessageProcessorDelegate.getModels(providerName);
  }

  @Override
  public Object getAnnotation(QName qName) {
    return operationMessageProcessorDelegate.getAnnotation(qName);
  }

  @Override
  public Map<QName, Object> getAnnotations() {
    return operationMessageProcessorDelegate.getAnnotations();
  }

  @Override
  public void setAnnotations(Map<QName, Object> newAnnotations) {
    operationMessageProcessorDelegate.setAnnotations(newAnnotations);
  }

  @Override
  public ComponentLocation getLocation() {
    return operationMessageProcessorDelegate.getLocation();
  }

  @Override
  public Location getRootContainerLocation() {
    return operationMessageProcessorDelegate.getRootContainerLocation();
  }


  @Override
  public MetadataResult<MetadataKeysContainer> getEntityKeys() throws MetadataResolvingException {
    return operationMessageProcessorDelegate.getEntityKeys();
  }

  @Override
  public MetadataResult<TypeMetadataDescriptor> getEntityMetadata(MetadataKey key) throws MetadataResolvingException {
    return operationMessageProcessorDelegate.getEntityMetadata(key);
  }

  @Override
  public void validateOperationConfiguration(ConfigurationProvider configurationProvider) {
    operationMessageProcessorDelegate.validateOperationConfiguration(configurationProvider);
  }

  @Override
  public ProcessingType getProcessingType() {
    return operationMessageProcessorDelegate.getProcessingType();
  }

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    return operationMessageProcessorDelegate.process(event);
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return operationMessageProcessorDelegate.apply(publisher);
  }

  @Override
  public Mono<CoreEvent> doProcess(CoreEvent event, ExecutionContextAdapter<OperationModel> operationContext) {
    return operationMessageProcessorDelegate.doProcess(event, operationContext);
  }
}
