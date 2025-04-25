package org.mule.runtime.module.launcher.util;

import java.net.JarURLConnection;

/**
 * Utility methods for configuring {@link JarURLConnection} behavior.
 */
public final class JarURLConnectionUtils {

  private JarURLConnectionUtils() {
    // Utility class
  }

  /**
   * Disables the default caching behavior of {@link JarURLConnection} for the "jar" protocol.
   * <p>
   * By default, Java caches open JAR file handles when resolving resources via {@link JarURLConnection}. This can cause file
   * locking issues, especially on Windows platforms, preventing the deletion or modification of JAR files during the undeployment
   * of applications or extensions.
   * <p>
   * Disabling caching prevents these file locks by ensuring that JAR files are not retained unnecessarily by the JVM once
   * accessed.
   * <p>
   * This method should be called early during runtime initialization, before any JAR resources are loaded.
   */
  public static void disableJarURLConnectionCache() {
    JarURLConnection.setDefaultUseCaches("jar", false);
  }
}
