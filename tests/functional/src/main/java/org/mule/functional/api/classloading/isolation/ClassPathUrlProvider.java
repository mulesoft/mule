/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.functional.api.classloading.isolation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.mule.runtime.core.util.Preconditions.checkArgument;

import com.google.common.collect.Lists;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Uses java system properties to get the classpath {@link URL}s.
 *
 * @since 4.0
 */
public class ClassPathUrlProvider {

  protected final transient Logger logger = LoggerFactory.getLogger(this.getClass());
  private final List<URL> urls;

  /**
   * Creates an instance of the provider that uses system properties to get the classpath {@link URL}s.
   */
  public ClassPathUrlProvider() {
    this.urls = new ArrayList<>();
  }


  /**
   * Creates an instance of the provided with a list of {@link URL}s to be appended in addition to the classpath ones.
   *
   * @param urls {@link URL}s to be added to the ones already in classpath, not null or empty.
   */
  public ClassPathUrlProvider(List<URL> urls) {
    checkNotNull(urls, "urls cannot be null");
    checkArgument(urls.size() > 0, "urls cannot be empty");

    this.urls = urls;
  }

  /**
   * @return Gets the urls from the {@code java.class.path} and {@code sun.boot.class.path} system properties
   */
  public List<URL> getURLs() {
    final Set<URL> urls = new LinkedHashSet<>();
    addUrlsFromSystemProperty(urls, "java.class.path");
    addUrlsFromSystemProperty(urls, "sun.boot.class.path");

    if (logger.isDebugEnabled()) {
      StringBuilder builder = new StringBuilder("ClassPath:");
      urls.stream().forEach(url -> builder.append(File.pathSeparator).append(url));
      logger.debug(builder.toString());
    }

    urls.addAll(this.urls);

    return Lists.newArrayList(urls);
  }

  protected void addUrlsFromSystemProperty(final Collection<URL> urls, final String propertyName) {
    for (String file : System.getProperty(propertyName).split(":")) {
      try {
        urls.add(new File(file).toURI().toURL());
      } catch (MalformedURLException e) {
        throw new IllegalArgumentException("Cannot create a URL from file path: " + file, e);
      }
    }
  }

}
