/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSED;

import static java.lang.String.format;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.notification.IntegerAction;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.extension.api.property.ClassLoaderModelProperty;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.extension.internal.loader.java.property.ArtifactLifecycleListenerModelProperty;
import org.mule.sdk.api.artifact.lifecycle.ArtifactDisposalContext;
import org.mule.sdk.api.artifact.lifecycle.ArtifactLifecycleListener;

import java.util.Optional;
import java.util.function.Predicate;

import org.slf4j.Logger;

/**
 * A {@link NotificationListener} implementation to catch artifact context disposal events and dispatch to the corresponding
 * extension's {@link ArtifactLifecycleListener} callback.
 *
 * @since 4.5.0
 */
public class ExtensionOnMuleContextDisposedNotificationListener
    implements MuleContextNotificationListener<MuleContextNotification> {

  private static final Logger LOGGER = getLogger(ExtensionOnMuleContextDisposedNotificationListener.class);

  private static final Predicate<MuleContextNotification> ON_CONTEXT_DISPOSED_SELECTOR =
      notification -> new IntegerAction(CONTEXT_DISPOSED).equals(notification.getAction());

  /**
   * If the Extension modeled by the given {@code extensionModel} has an {@link ArtifactLifecycleListener}, this method will
   * create and register a {@link NotificationListener} for when the artifact's context gets disposed in order to call the
   * {@link ArtifactLifecycleListener#onArtifactDisposal(ArtifactDisposalContext)} method with the corresponding context.
   *
   * @param serverNotificationManager the {@link ServerNotificationManager} associated with the artifact the extension belongs to.
   * @param executionClassLoader      the execution {@link ClassLoader} associated with the artifact the extension belongs to.
   * @param extensionModel            the {@link ExtensionModel} of the extension.
   */
  public static void registerLifecycleListenerForOnContextDisposed(ServerNotificationManager serverNotificationManager,
                                                                   ClassLoader executionClassLoader,
                                                                   ExtensionModel extensionModel) {
    Optional<ArtifactLifecycleListener> artifactLifecycleListener = getArtifactLifecycleListener(extensionModel);
    Optional<ClassLoader> extensionClassLoader = getExtensionClassLoader(extensionModel)
        .filter(ArtifactClassLoader.class::isInstance);

    if (artifactLifecycleListener.isPresent()
        && extensionClassLoader.isPresent()
        && executionClassLoader instanceof ArtifactClassLoader) {
      ArtifactDisposalContext context = new DefaultArtifactDisposalContext((ArtifactClassLoader) executionClassLoader,
                                                                           (ArtifactClassLoader) extensionClassLoader.get());

      String artifactId = ((ArtifactClassLoader) executionClassLoader).getArtifactId();
      String extensionId = ((ArtifactClassLoader) extensionClassLoader.get()).getArtifactId();
      serverNotificationManager
          .addListenerSubscription(new ExtensionOnMuleContextDisposedNotificationListener(artifactId,
                                                                                          extensionId,
                                                                                          artifactLifecycleListener.get(),
                                                                                          context),
                                   ON_CONTEXT_DISPOSED_SELECTOR);
    }
  }

  private static Optional<ArtifactLifecycleListener> getArtifactLifecycleListener(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ArtifactLifecycleListenerModelProperty.class)
        .map(mp -> mp.getArtifactLifecycleListenerFactory().createArtifactLifecycleListener());
  }

  private static Optional<ClassLoader> getExtensionClassLoader(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ClassLoaderModelProperty.class)
        .map(ClassLoaderModelProperty::getClassLoader);
  }

  private final String artifactId;
  private final String extensionId;
  private final ArtifactLifecycleListener artifactLifecycleListener;
  private final ArtifactDisposalContext artifactDisposalContext;

  private ExtensionOnMuleContextDisposedNotificationListener(String artifactId,
                                                             String extensionId,
                                                             ArtifactLifecycleListener artifactLifecycleListener,
                                                             ArtifactDisposalContext artifactDisposalContext) {
    this.artifactId = artifactId;
    this.extensionId = extensionId;
    this.artifactLifecycleListener = artifactLifecycleListener;
    this.artifactDisposalContext = artifactDisposalContext;
  }

  @Override
  public void onNotification(MuleContextNotification notification) {
    try {
      artifactLifecycleListener.onArtifactDisposal(artifactDisposalContext);
    } catch (Throwable t) {
      String message =
          format("Error executing (%s)'s #onArtifactDisposal from extension '%s' on artifact '%s'. This can cause a resource leak",
                 artifactLifecycleListener,
                 extensionId,
                 artifactId);
      LOGGER.error(message, t);
    }
  }
}
