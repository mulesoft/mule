/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.globalconfig.api.maven;

import static org.mule.maven.client.api.MavenClientProvider.discoverProvider;

import org.mule.maven.client.api.MavenClient;
import org.mule.maven.client.api.MavenClientProvider;
import org.mule.maven.client.api.model.MavenConfiguration;
import org.mule.runtime.api.util.LazyValue;

import java.util.function.Supplier;

/**
 * Allows to obtain {@link MavenClient} instances and configure how those are created.
 * 
 * @since 4.5
 */
public class MavenClientFactory {

  public static Supplier<MavenClientProvider> mavenClientProvider =
      new LazyValue<>(() -> discoverProvider(MavenClientProvider.class.getClassLoader()));

  /**
   * Obtains a {@link MavenClient} though the {@link MavenClientProvider} configured through
   * {@link #setMavenClientProvider(Supplier)}.
   * <p>
   * By default, the {@link MavenClientProvider} is discovered through SPI.
   * 
   * @param mavenConfiguration
   * @return a new {@link MavenClient} for the provided {@code mavenConfiguration}.
   */
  public static MavenClient createMavenClient(MavenConfiguration mavenConfiguration) {
    return mavenClientProvider.get().createMavenClient(mavenConfiguration);
  }

  /**
   * Registers the {@link MavenClientProvider} to use for obtaining a {@link MavenClient}.
   * 
   * @param mavenClientProvider
   */
  public static void setMavenClientProvider(Supplier<MavenClientProvider> mavenClientProvider) {
    MavenClientFactory.mavenClientProvider = mavenClientProvider;
  }

}
