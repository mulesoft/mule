/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.utils;

import static com.github.benmanes.caffeine.cache.Caffeine.newBuilder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;

import com.github.benmanes.caffeine.cache.Cache;

import java.net.URI;

/**
 * Cache to avoid recalculating URIs more than once
 *
 * @since 4.0
 */
public class UriCache {

  private static final int MAX_CACHE_SIZE = 2000;
  private static volatile UriCache instance;

  private Cache<String, URI> cache = newBuilder().maximumSize(MAX_CACHE_SIZE).build();

  private UriCache() {}

  public static UriCache getInstance() {
    if (instance == null) {
      synchronized (UriCache.class) {
        if (instance == null) {
          instance = new UriCache();
        }
      }
    }
    return instance;
  }

  public static URI getUriFromString(String uri) {
    try {
      return getInstance().cache.get(uri, u -> URI.create(u));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create URI for " + uri, e));
    }
  }
}
