/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.artifact.descriptor.api;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.deployment.meta.Product;
import org.mule.runtime.api.meta.MuleVersion;

import java.io.File;

@NoImplement
public interface ArtifactDescriptor {

  String getName();

  File getRootFolder();

  // void setRootFolder(File rootFolder);

  MuleVersion getMinMuleVersion();

  // void setMinMuleVersion(MuleVersion minMuleVersion);

  ClassLoaderConfiguration getClassLoaderConfiguration();

  // void setClassLoaderConfiguration(ClassLoaderConfiguration classLoaderConfiguration);

  BundleDescriptor getBundleDescriptor();

  Product getRequiredProduct();

  // void setRequiredProduct(Product requiredProduct);

  // void setBundleDescriptor(BundleDescriptor bundleDescriptor);

}
