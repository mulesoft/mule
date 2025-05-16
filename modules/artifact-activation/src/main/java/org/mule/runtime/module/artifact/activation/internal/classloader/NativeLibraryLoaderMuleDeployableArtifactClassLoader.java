/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.internal.classloader;

import static org.mule.runtime.api.config.MuleRuntimeFeature.SUPPORT_NATIVE_LIBRARY_DEPENDENCIES;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.meta.MuleVersion.v4_6_0;

import static org.mule.runtime.module.artifact.api.classloader.LoggerClassRegistry.getLoggerClassRegistry;
import static org.mule.runtime.module.artifact.internal.util.FeatureFlaggingUtils.isFeatureEnabled;

import static net.bytebuddy.description.modifier.Visibility.PUBLIC;
import static net.bytebuddy.dynamic.loading.ClassLoadingStrategy.Default.INJECTION;
import static net.bytebuddy.implementation.MethodDelegation.to;

import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.module.artifact.activation.internal.nativelib.NativeLibraryFinder;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.bytebuddy.ByteBuddy;

public abstract class NativeLibraryLoaderMuleDeployableArtifactClassLoader extends MuleDeployableArtifactClassLoader {

  static {
    registerAsParallelCapable();
    getLoggerClassRegistry().register(NativeLibraryLoaderMuleDeployableArtifactClassLoader.class);
  }

  private final Logger logger = LoggerFactory.getLogger(NativeLibraryLoaderMuleDeployableArtifactClassLoader.class);

  public static final String METHOD_NAME = "loadLibrary";
  private final NativeLibraryFinder nativeLibraryFinder;
  protected final boolean supportNativeLibraryDependencies;
  private static final AtomicBoolean areFeatureFlagsConfigured = new AtomicBoolean();
  private final LazyValue<Class<?>> dynamicLibraryLoader = new LazyValue<>(this::getDynamicLibraryLoader);

  public Class<?> getDynamicLibraryLoader() {
    return new ByteBuddy()
        .subclass(Object.class)
        .defineMethod(METHOD_NAME, void.class, PUBLIC)
        .withParameters(String.class)
        .intercept(to(System.class))
        .make()
        .load(this, INJECTION)
        .getLoaded();
  }

  static {
    if (!areFeatureFlagsConfigured.getAndSet(true)) {
      configureSupportNativeLibraryDependencies();
    }
  }

  protected NativeLibraryLoaderMuleDeployableArtifactClassLoader(String artifactId, ArtifactDescriptor artifactDescriptor,
                                                                 ClassLoader parentCl,
                                                                 NativeLibraryFinder nativeLibraryFinder, List<URL> urls,
                                                                 ClassLoaderLookupPolicy lookupPolicy) {
    super(artifactId, artifactDescriptor, urls.toArray(new URL[0]), parentCl, lookupPolicy);
    this.nativeLibraryFinder = nativeLibraryFinder;
    this.supportNativeLibraryDependencies = isFeatureEnabled(SUPPORT_NATIVE_LIBRARY_DEPENDENCIES, artifactDescriptor);
  }

  protected void loadNativeLibraryDependencies(String nativeLibraryName) {
    // TODO: W-12786373 Implement loading native libraries with dependencies.
    if (!dynamicLibraryLoader.isComputed()) {
      Class<?> loader = dynamicLibraryLoader.get();
      Method method;
      try {
        method = loader.getMethod(METHOD_NAME, String.class);
      } catch (NoSuchMethodException e) {
        throw new MuleRuntimeException(createStaticMessage("Could not create native library loader."), e);
      }

      List<String> nativeLibraries = nativeLibraryFinder.findLibraryNames();
      nativeLibraries.remove(nativeLibraryName);
      Collections.reverse(nativeLibraries);

      for (String nativeLibrary : nativeLibraries) {
        try {
          method.invoke(loader.getDeclaredConstructor().newInstance(), nativeLibrary);
        } catch (IllegalAccessException | InvocationTargetException | InstantiationException | NoSuchMethodException e) {
          throw new MuleRuntimeException(createStaticMessage("Could not load %s native library.", nativeLibrary), e);
        }
      }
    }
  }

  /**
   * Configures the {@link MuleRuntimeFeature#SUPPORT_NATIVE_LIBRARY_DEPENDENCIES} feature flag.
   *
   * @since 4.4.0
   */
  private static void configureSupportNativeLibraryDependencies() {
    FeatureFlaggingRegistry featureFlaggingRegistry = FeatureFlaggingRegistry.getInstance();
    featureFlaggingRegistry.registerFeatureFlag(SUPPORT_NATIVE_LIBRARY_DEPENDENCIES, minMuleVersion(v4_6_0));
  }

  private static Predicate<FeatureContext> minMuleVersion(MuleVersion version) {
    return featureContext -> featureContext.getArtifactMinMuleVersion()
        .filter(muleVersion -> muleVersion.atLeast(version)).isPresent();
  }
}
