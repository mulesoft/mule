/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.manifest.internal;

import static java.util.regex.Pattern.compile;

import org.mule.runtime.manifest.api.MuleManifest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Enumeration;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link MuleManifest}.
 */
public class DefaultMuleManifest implements MuleManifest {

  private static final Logger logger = LoggerFactory.getLogger(DefaultMuleManifest.class);

  private static final MuleManifest INSTANCE = new DefaultMuleManifest();

  private static Manifest manifest;
  private static volatile String productVersionFromProperties;

  private DefaultMuleManifest() {}

  public static MuleManifest get() {
    return INSTANCE;
  }

  @Override
  public String getProductVersion() {
    final String version = getManifestProperty("Implementation-Version");
    if (version == null) {
      return productVersionFromProperties;
    } else {
      return version;
    }
  }

  public String getProductVersionFromPropertiesFile() {
    if (productVersionFromProperties == null) {
      synchronized (this) {
        if (productVersionFromProperties == null) {
          final String VERSION_PROPERTIES_PATH = "product-version/version.properties";
          final String WARNING_MESSAGE_VERSION_COULDNT_BE_RESOLVED = "Failure reading {} properties file to get productVersion";
          final String COULDNT_BE_RESOLVED_PLACEHOLDER = "<Mule version could not be resolved>";

          Properties versionProps = new Properties();
          try (InputStream versionPropsInputStream =
              MuleManifest.class.getClassLoader().getResourceAsStream(VERSION_PROPERTIES_PATH)) {
            if (versionPropsInputStream == null) {
              logger.warn(WARNING_MESSAGE_VERSION_COULDNT_BE_RESOLVED, VERSION_PROPERTIES_PATH);
              productVersionFromProperties = COULDNT_BE_RESOLVED_PLACEHOLDER;
            }

            versionProps.load(versionPropsInputStream);
            productVersionFromProperties = versionProps.getProperty("mule.version");
          } catch (IOException e) {
            logger.warn(WARNING_MESSAGE_VERSION_COULDNT_BE_RESOLVED, VERSION_PROPERTIES_PATH, e);
            productVersionFromProperties = COULDNT_BE_RESOLVED_PLACEHOLDER;
          }
        }
      }
    }

    return productVersionFromProperties;
  }

  @Override
  public String getVendorName() {
    return getManifestProperty("Specification-Vendor");
  }

  @Override
  public String getVendorUrl() {
    return getManifestProperty("Vendor-Url");
  }

  @Override
  public String getProductUrl() {
    return getManifestProperty("Product-Url");
  }

  @Override
  public String getProductName() {
    return getManifestProperty("Implementation-Title");
  }

  @Override
  public String getProductMoreInfo() {
    return getManifestProperty("More-Info");
  }

  @Override
  public String getProductSupport() {
    return getManifestProperty("Support");
  }

  @Override
  public String getProductLicenseInfo() {
    return getManifestProperty("License");
  }

  @Override
  public String getProductDescription() {
    return getManifestProperty("Description");
  }

  @Override
  public String getBuildNumber() {
    return getManifestProperty("Build-Revision");
  }

  @Override
  public String getBuildDate() {
    return getManifestProperty("Build-Date");
  }

  @Override
  public String getSupportedJdks() {
    return getManifestProperty("Supported-Jdks");
  }

  @Override
  public String getRecommendedJdks() {
    return getManifestProperty("Recommended-Jdks");
  }

  // synchronize this method as manifest initialized here.
  @Override
  public synchronized Manifest getManifest() {
    if (manifest == null) {
      manifest = new Manifest();

      InputStream is = null;
      try {
        // We want to load the MANIFEST.MF from the mule-core jar. Sine we
        // don't know the version we're using we have to search for the jar on the classpath
        URL url = AccessController.doPrivileged(new UrlPrivilegedAction());

        if (url != null) {
          URLConnection urlConnection = url.openConnection();
          urlConnection.setUseCaches(false);
          is = urlConnection.getInputStream();
        }
        if (is != null) {
          try {
            manifest.read(is);
          } finally {
            is.close();
          }
        }
      } catch (IOException e) {
        logger.warn("Failed to read manifest Info, Manifest information will not display correctly: {}", e.getMessage());
      }
    }
    return manifest;
  }

  private static String getManifestProperty(String name) {
    return get().getManifest().getMainAttributes().getValue(new Attributes.Name(name));
  }

  static class UrlPrivilegedAction implements PrivilegedAction<URL> {

    private static final Pattern EMBEDDED_JAR_PATTERN = compile("mule[^-]*-[^-]*-embedded");
    private static final String MANIFEST_PATH = "META-INF/MANIFEST.MF";

    @Override
    public URL run() {
      URL result = null;
      try {
        Enumeration<URL> e = MuleManifest.class.getClassLoader().getResources(MANIFEST_PATH);
        result = getManifestJarURL(e);
      } catch (IOException e1) {
        logger.warn("Failure reading manifest: " + e1.getMessage(), e1);
      }
      return result;
    }

    URL getManifestJarURL(Enumeration<URL> e) {
      SortedMap<String, URL> candidates = new TreeMap<>();
      while (e.hasMoreElements()) {
        URL url = e.nextElement();
        if (url.toExternalForm().contains("mule-manifest")
            || url.toExternalForm().contains("mule-runtime-extension-model")
            || url.toExternalForm().contains("mule-runtime-ee-extension-model")
            || (EMBEDDED_JAR_PATTERN.matcher(url.toExternalForm()).find() && url.toExternalForm().endsWith(".jar"))) {
          candidates.put(url.toExternalForm(), url);
        }
      }
      if (!candidates.isEmpty()) {
        // if mule-manifest and mule-manifest-ee jars are present, then mule-manifest-ee gets precedence
        for (String candidateKey : candidates.keySet()) {
          if (candidateKey.contains("mule-manifest-ee")
              || candidateKey.contains("mule-runtime-ee-extension-model")) {
            return candidates.get(candidateKey);
          }
        }
        return candidates.get(candidates.lastKey());
      }
      return null;
    }
  }

}
