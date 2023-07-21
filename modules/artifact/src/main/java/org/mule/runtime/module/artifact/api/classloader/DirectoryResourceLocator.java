/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

@NoInstantiate
public final class DirectoryResourceLocator implements LocalResourceLocator {

  private String[] directories;

  public DirectoryResourceLocator(String... directories) {
    this.directories = directories;
  }

  @Override
  public URL findLocalResource(String resourceName) {
    if (directories != null && resourceName != null) {
      for (String directory : directories) {
        File resourceFile = new File(directory, resourceName);
        if (resourceFile.exists()) {
          try {
            return resourceFile.toURI().toURL();
          } catch (MalformedURLException e) {
            throw new MuleRuntimeException(createStaticMessage(format("Can not load resource with name %s.", resourceName)), e);
          }
        }
      }
    }
    return null;
  }
}
