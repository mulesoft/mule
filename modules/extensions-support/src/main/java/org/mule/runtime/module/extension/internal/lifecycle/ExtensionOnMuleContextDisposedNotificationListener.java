/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.lifecycle;

import static org.mule.runtime.core.api.context.notification.MuleContextNotification.CONTEXT_DISPOSED;

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

/**
 * A {@link NotificationListener} implementation to catch artifact context disposal events and dispatch to the corresponding
 * extension's {@link ArtifactLifecycleListener} callback.
 */
public class ExtensionOnMuleContextDisposedNotificationListener
    implements MuleContextNotificationListener<MuleContextNotification> {

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
    Optional<ClassLoader> extensionClassLoader = getExtensionClassLoader(extensionModel);
    if (artifactLifecycleListener.isPresent()
        && extensionClassLoader.isPresent()
        && extensionClassLoader.get() instanceof ArtifactClassLoader
        && executionClassLoader instanceof ArtifactClassLoader) {
      ArtifactDisposalContext context = new DefaultArtifactDisposalContext((ArtifactClassLoader) executionClassLoader,
                                                                           (ArtifactClassLoader) extensionClassLoader.get());
      serverNotificationManager
          .addListenerSubscription(new ExtensionOnMuleContextDisposedNotificationListener(artifactLifecycleListener.get(),
                                                                                          context),
                                   ON_CONTEXT_DISPOSED_SELECTOR);
    }
  }

  private static Optional<ArtifactLifecycleListener> getArtifactLifecycleListener(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ArtifactLifecycleListenerModelProperty.class)
        .map(ArtifactLifecycleListenerModelProperty::getArtifactLifecycleListener);
  }

  private static Optional<ClassLoader> getExtensionClassLoader(ExtensionModel extensionModel) {
    return extensionModel.getModelProperty(ClassLoaderModelProperty.class)
        .map(ClassLoaderModelProperty::getClassLoader);
  }

  private final ArtifactLifecycleListener artifactLifecycleListener;
  private final ArtifactDisposalContext artifactDisposalContext;

  private ExtensionOnMuleContextDisposedNotificationListener(ArtifactLifecycleListener artifactLifecycleListener,
                                                             ArtifactDisposalContext artifactDisposalContext) {
    this.artifactLifecycleListener = artifactLifecycleListener;
    this.artifactDisposalContext = artifactDisposalContext;
  }

  @Override
  public void onNotification(MuleContextNotification notification) {
    artifactLifecycleListener.onArtifactDisposal(artifactDisposalContext);
  }
}
