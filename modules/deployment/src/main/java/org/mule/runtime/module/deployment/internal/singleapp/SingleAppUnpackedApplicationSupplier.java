/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal.singleapp;

import static org.apache.commons.io.filefilter.DirectoryFileFilter.DIRECTORY;

import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.ArtifactFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Supplier;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Optional.empty;

/**
 * A {@link Supplier<Application>} that provides the app from an unpacked directory. It verifies that only one unpacked app
 * exists. For single app environments.
 *
 * @since 4.6.0
 */
public class SingleAppUnpackedApplicationSupplier implements Supplier<Application> {

  public static Supplier<Application> getSingleAppUnpackedApplicationSupplier(ArtifactFactory<ApplicationDescriptor, Application> applicationFactory) {
    return new SingleAppUnpackedApplicationSupplier(applicationFactory);
  }

  private final ArtifactFactory<ApplicationDescriptor, Application> applicationFactory;

  private SingleAppUnpackedApplicationSupplier(ArtifactFactory<ApplicationDescriptor, Application> applicationFactory) {
    this.applicationFactory = applicationFactory;
  }

  @Override
  public Application get() {
    String applicationName = getSingleAppExplodedDirectory(applicationFactory.getArtifactDir());
    File artifactLocation = new File(applicationFactory.getArtifactDir(), applicationName);
    try {
      return createArtifact(artifactLocation);
    } catch (IOException e) {
      throw new ApplicationSupplierException(e);
    }
  }

  private Application createArtifact(File artifactLocation) throws IOException {
    return applicationFactory.createArtifact(artifactLocation, empty());
  }

  private String getSingleAppExplodedDirectory(File appsDirectory) {
    String[] apps = appsDirectory.list(DIRECTORY);

    if (apps == null) {
      throw new IllegalStateException(format("We got a null while listing the contents of director '%s'. Some common " +
          "causes for this is a lack of permissions to the directory or that it's being deleted concurrently",
                                             appsDirectory.getName()));
    }

    if (apps.length == 0) {
      throw new IllegalStateException("No unpacked application present in apps dir.");
    }
    if (apps.length > 1) {
      throw new IllegalStateException("More than one unpacked application (" + join(", ", apps) + ") present in apps dir.");
    }
    return apps[0];
  }
}
