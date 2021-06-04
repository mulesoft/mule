/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENABLE_POLICY_ISOLATION;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.collection.SmallMap.copy;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXTENSION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_LOCK_PROVIDER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TIME_SUPPLIER;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder.newBuilder;
import static org.mule.runtime.module.deployment.impl.internal.config.FeatureFlaggingUtils.isFeatureEnabled;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.notification.PolicyNotification;
import org.mule.runtime.api.notification.PolicyNotificationListener;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.context.notification.MuleContextListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.policy.Policy;
import org.mule.runtime.core.api.policy.PolicyInstance;
import org.mule.runtime.core.api.policy.PolicyParametrization;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPlugin;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactContextBuilder;
import org.mule.runtime.module.deployment.impl.internal.artifact.CompositeArtifactExtensionManagerFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.proxy.LifecycleFilterProxy;
import org.mule.runtime.module.extension.api.manager.DefaultExtensionManagerFactory;
import org.mule.runtime.module.extension.api.manager.ExtensionManagerFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderRepository;
import org.mule.runtime.policy.api.PolicyPointcut;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link ApplicationPolicyInstance} that depends on a {@link PolicyTemplate} artifact.
 */
public class DefaultApplicationPolicyInstance implements ApplicationPolicyInstance {

  public static final String CLUSTER_MANAGER_ID = "_muleClusterManager";

  private final Application application;
  private final PolicyTemplate template;
  private final PolicyParametrization parametrization;
  private final ServiceRepository serviceRepository;
  private final ClassLoaderRepository classLoaderRepository;
  private final ExtensionModelLoaderRepository extensionModelLoaderRepository;
  private final MuleContextListener muleContextListener;
  private ArtifactContext policyContext;
  private LazyValue<PolicyInstance> policyInstance;

  /**
   * Creates a new policy instance
   *
   * @param application                    application artifact owning the created policy. Non null
   * @param template                       policy template from which the instance will be created. Non null
   * @param parametrization                parameters used to configure the created instance. Non null
   * @param serviceRepository              repository of available services. Non null.
   * @param classLoaderRepository          contains the registered classloaders that can be used to load serialized classes. Non
   *                                       null.
   * @param extensionModelLoaderRepository {@link ExtensionModelLoaderRepository} with the available extension loaders. Non null.
   * @param muleContextListener            the listener to execute for specific events that occur on the {@link MuleContext} of
   *                                       the policy. May be {@code null}.
   */
  public DefaultApplicationPolicyInstance(Application application, PolicyTemplate template,
                                          PolicyParametrization parametrization,
                                          ServiceRepository serviceRepository,
                                          ClassLoaderRepository classLoaderRepository,
                                          ExtensionModelLoaderRepository extensionModelLoaderRepository,
                                          MuleContextListener muleContextListener) {
    this.application = application;
    this.template = template;
    this.parametrization = parametrization;
    this.serviceRepository = serviceRepository;
    this.classLoaderRepository = classLoaderRepository;
    this.extensionModelLoaderRepository = extensionModelLoaderRepository;
    this.muleContextListener = muleContextListener;
  }

  private void initPolicyContext() throws InitialisationException {
    ArtifactContextBuilder artifactBuilder =
        newBuilder().setArtifactType(POLICY)
            .setArtifactProperties(copy(parametrization.getParameters()))
            .setArtifactName(parametrization.getId())
            .setConfigurationFiles(parametrization.getConfig().getAbsolutePath())
            .setExecutionClassloader(template.getArtifactClassLoader().getClassLoader())
            .setServiceRepository(serviceRepository)
            .setClassLoaderRepository(classLoaderRepository)
            .setArtifactPlugins(getFeatureFlaggedArtifactPlugins(template.getDescriptor()))
            .setParentArtifact(application)
            .setExtensionManagerFactory(getFeatureFlaggedExtensionManagerFactory())
            .setMuleContextListener(muleContextListener);

    artifactBuilder.withServiceConfigurator(customizationService -> {
      Registry applicationRegistry = application.getArtifactContext().getRegistry();

      addPolicyCustomizationOverride(OBJECT_LOCK_PROVIDER, customizationService, applicationRegistry);
      addPolicyCustomizationOverride(BASE_PERSISTENT_OBJECT_STORE_KEY, customizationService, applicationRegistry);
      addPolicyCustomizationOverride(BASE_IN_MEMORY_OBJECT_STORE_KEY, customizationService, applicationRegistry);
      addPolicyCustomizationOverride(OBJECT_TIME_SUPPLIER, customizationService, applicationRegistry);
      addPolicyCustomizationOverride(CLUSTER_MANAGER_ID, customizationService, applicationRegistry);
    });
    try {
      policyContext = artifactBuilder.build();
      enableNotificationListeners(parametrization.getNotificationListeners());
      policyContext.getMuleContext().start();
    } catch (MuleException e) {
      throw new InitialisationException(createStaticMessage("Cannot create artifact context for the policy instance"), e, this);
    }
  }

  /**
   * Applies the {@link MuleRuntimeFeature#ENABLE_POLICY_ISOLATION} feature to the policy artifact plugins list.
   *
   * @return The policy artifact plugins.
   */
  private List<ArtifactPlugin> getFeatureFlaggedArtifactPlugins(ArtifactDescriptor policyArtifactDescriptor) {
    if (isFeatureEnabled(ENABLE_POLICY_ISOLATION, policyArtifactDescriptor)) {
      // Returns all the artifact plugins that the policy depends on.
      return template.getOwnArtifactPlugins();
    } else {
      // Returns the artifact plugins that the policy depends and are not imported by the application.
      return template.getArtifactPlugins();
    }
  }

  /**
   * {@link ExtensionManagerFactory} that defers the decision of the {@link ExtensionManager} implementation until the MuleContext
   * is available, in order to apply the {@link MuleRuntimeFeature#ENABLE_POLICY_ISOLATION} feature flag.
   *
   * @return {@link ExtensionManagerFactory} instance.
   */
  private ExtensionManagerFactory getFeatureFlaggedExtensionManagerFactory() {
    return muleContext -> {
      try {
        FeatureFlaggingService featureFlaggingService =
            ((MuleContextWithRegistry) muleContext).getRegistry().lookupObject(FeatureFlaggingService.class);
        if (featureFlaggingService.isEnabled(ENABLE_POLICY_ISOLATION)) {
          // The policy will not share extension models and configuration providers with the application that is being applied to.
          ArtifactExtensionManagerFactory artifactExtensionManagerFactory =
              new ArtifactExtensionManagerFactory(template.getOwnArtifactPlugins(), extensionModelLoaderRepository,
                                                  new DefaultExtensionManagerFactory());
          return artifactExtensionManagerFactory.create(muleContext, getInheritedExtensionModels());
        } else {
          // The policy will share extension models and configuration providers with the application that is being applied to.
          return new CompositeArtifactExtensionManagerFactory(application, extensionModelLoaderRepository,
                                                              template.getOwnArtifactPlugins(),
                                                              new DefaultExtensionManagerFactory()).create(muleContext);
        }
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      }
    };
  }

  /**
   * HTTP and Sockets extension models must be added if found in the parent artifact extensions (backward compatibility for API
   * Gateway).
   * 
   * @return Set containing the parent artifact HTTP and Sockets extension models (if present).
   */
  private Set<ExtensionModel> getInheritedExtensionModels() {
    Set<ExtensionModel> inheritedExtensionModels = new HashSet<>(2);
    ExtensionManager extensionManager = application.getRegistry().<ExtensionManager>lookupByName(OBJECT_EXTENSION_MANAGER).get();
    extensionManager.getExtension("HTTP")
        .ifPresent(inheritedExtensionModels::add);
    extensionManager.getExtension("Sockets")
        .ifPresent(inheritedExtensionModels::add);
    return inheritedExtensionModels;
  }

  private void addPolicyCustomizationOverride(String objectKey, CustomizationService customizationService,
                                              Registry applicationRegistry) {
    applicationRegistry
        .lookupByName(objectKey)
        .map(LifecycleFilterProxy::createLifecycleFilterProxy)
        .ifPresent(s -> customizationService.overrideDefaultServiceImpl(objectKey, s));
  }

  private void enableNotificationListeners(List<NotificationListener> notificationListeners) {
    NotificationListenerRegistry listenerRegistry =
        policyContext.getRegistry().lookupByType(NotificationListenerRegistry.class).get();

    policyContext.getMuleContext().getNotificationManager().addInterfaceToType(PolicyNotificationListener.class,
                                                                               PolicyNotification.class);
    notificationListeners.forEach(listenerRegistry::registerListener);
  }

  private PolicyInstance initPolicyInstance() {
    return policyContext.getRegistry().lookupByType(PolicyInstance.class).get();
  }

  @Override
  public PolicyPointcut getPointcut() {
    return parametrization.getPolicyPointcut();
  }

  @Override
  public int getOrder() {
    return parametrization.getOrder();
  }

  @Override
  public PolicyTemplate getPolicyTemplate() {
    return template;
  }

  @Override
  public void initialise() throws InitialisationException {
    if (policyInstance == null && policyContext == null) {
      initPolicyContext();
    }
    policyInstance = new LazyValue<>(this::initPolicyInstance);
  }

  @Override
  public void dispose() {
    if (policyContext != null) {
      policyContext.getMuleContext().dispose();
    }
  }

  @Override
  public Optional<Policy> getSourcePolicy() {
    return policyInstance.get().getSourcePolicyChain()
        .map(chain -> new Policy(chain, parametrization.getId()));
  }

  @Override
  public Optional<Policy> getOperationPolicy() {
    return policyInstance.get().getOperationPolicyChain()
        .map(chain -> new Policy(chain, parametrization.getId()));
  }

}
