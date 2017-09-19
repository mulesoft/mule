/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.utils;

import static java.net.URLEncoder.encode;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.util.MultiMap;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Provides helper methods for encoding and decoding http request and response content.
 * 
 * @since 4.0
 */
public final class HttpEncoderDecoderUtils {

  private HttpEncoderDecoderUtils() {
    // Nothing to do
  }

  private static final String SPACE_ENTITY = "%20";

  /**
   * Extracts the path (what's left of the {@code ?} character) from the passed uri.
   * 
   * @param uri the uri to extract the path from
   * @return the path form the uri
   */
  public static String extractPath(String uri) {
    String path = uri;
    int i = path.indexOf('?');
    if (i > -1) {
      path = path.substring(0, i);
    }
    return path;
  }

  /**
   * Extracts the query parameters (what's right of the {@code ?} character) from the passed uri.
   * 
   * @param uri the uri to extract the parameters from
   * @return the parameters form the uri
   */
  public static String extractQueryParams(String uri) {
    int i = uri.indexOf("?");
    String queryString = "";
    if (i > -1) {
      queryString = uri.substring(i + 1);
    }
    return queryString;
  }

  /**
   * Converts a query-string from a request url into a {@link MultiMap}.
   * <p>
   * This is the inverse of {@link #encodeQueryString(Map)}.
   * 
   * @param queryString the query string to parse
   * @return a map representation of the {@code queryString}
   */
  public static MultiMap<String, String> decodeQueryString(String queryString) {
    return decodeUrlEncodedBody(queryString, UTF_8);
  }

  /**
   * Converts a map to a request url query-string form.
   * <p>
   * This is the inverse of {@link #decodeQueryString(String)}.
   * 
   * @param parameters a map representation of the {@code queryString}
   * @return the generated query string
   */
  public static String encodeQueryString(Map<String, String> parameters) {
    return encodeString(parameters, UTF_8);
  }

  /**
   * Converts an url-encoded body into a {@link MultiMap} with a given encoding.
   * <p>
   * This is the inverse of {@link #encodeString(String, Charset)}.
   * 
   * @param queryString the string to parse
   * @param encoding {@link URLDecoder#decode(String, String)}.
   * @return a map representation of the {@code queryString}
   */
  public static MultiMap<String, String> decodeUrlEncodedBody(String queryString, Charset encoding) {
    MultiMap<String, String> queryParams = new MultiMap<>();
    if (queryString != null && queryString.trim().length() > 0) {
      String[] pairs = queryString.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");

        if (idx != -1) {
          addParam(queryParams, pair.substring(0, idx), pair.substring(idx + 1), encoding);
        } else {
          addParam(queryParams, pair, null, encoding);

        }
      }
    }
    return queryParams;
  }

  /**
   * Decodes uri params from a request path
   *
   * @param pathWithUriParams path with uri param place holders
   * @param requestPath request path
   * @return a map with the uri params present in the request path with the values decoded.
   */
  public static Map<String, String> decodeUriParams(String pathWithUriParams, String requestPath) {
    MultiMap<String, String> uriParams = new MultiMap<>();
    if (pathWithUriParams.contains("{")) {
      final String[] requestPathParts = requestPath.split("/");
      final String[] listenerPathParts = pathWithUriParams.split("/");
      int longerPathSize = Math.min(requestPathParts.length, listenerPathParts.length);
      // split will return an empty string as first path before /
      for (int i = 1; i < longerPathSize; i++) {
        final String listenerPart = listenerPathParts[i];
        if (listenerPart.startsWith("{") && listenerPart.endsWith("}")) {
          String parameterName = listenerPart.substring(1, listenerPart.length() - 1);
          String parameterValue = requestPathParts[i];
          uriParams.put(parameterName, decode(parameterValue, UTF_8));
        }
      }
    }
    return uriParams;
  }

  private static void addParam(MultiMap<String, String> queryParams, String name, String value, Charset encoding) {
    queryParams.put(decode(name, encoding), decode(value, encoding));
  }

  private static String decode(String text, Charset encoding) {
    if (text == null) {
      return null;
    }
    try {
      return URLDecoder.decode(text, encoding.name());
    } catch (UnsupportedEncodingException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * Converts a map to a request url query-string form.
   * <p>
   * This is the inverse of {@link #decodeUrlEncodedBody(String, Charset)}.
   * 
   * @param parameters a map representation of the {@code queryString}
   * @param encoding {@link URLDecoder#decode(String, String)}.
   * @return the generated query string
   */
  public static String encodeString(Map parameters, Charset encoding) {
    String body;
    StringBuilder result = new StringBuilder();
    for (Map.Entry<?, ?> entry : (Set<Map.Entry<?, ?>>) ((parameters).entrySet())) {
      String paramName = entry.getKey().toString();
      Object paramValue = entry.getValue();

      Iterable paramValues = paramValue instanceof Iterable ? (Iterable) paramValue : Arrays.asList(paramValue);
      for (Object value : paramValues) {
        try {
          paramName = encode(paramName, encoding.name());
          paramValue = value != null ? encode(value.toString(), encoding.name()) : null;
        } catch (UnsupportedEncodingException e) {
          throw new MuleRuntimeException(e);
        }

        if (result.length() > 0) {
          result.append("&");
        }
        result.append(paramName);
        if (paramValue != null) {
          // Allowing parameters name with no value assigned
          result.append("=");
          result.append(paramValue);
        }
      }
    }

    body = result.toString();
    return body;
  }

  /**
   * Encodes spaces in a path, replacing them by %20.
   *
   * @param path Path that may contain spaces
   * @return The path with all spaces replaced by %20.
   */
  public static String encodeSpaces(String path) {
    return path.replaceAll(" ", SPACE_ENTITY);
  }

  /**
   * Appends a query parameter to an URL that may or may not contain query parameters already.
   *
   * @param url base URL to apply the new query parameter
   * @param queryParamName query parameter name
   * @param queryParamValue query parameter value
   * @return a new string with the query parameter appended
   */
  public static String appendQueryParam(String url, String queryParamName, String queryParamValue) {
    try {
      return (url.contains("?") ? url + "&" : url + "?")
          + encode(queryParamName, UTF_8.name()) + "=" + encode(queryParamValue, UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new MuleRuntimeException(e);
    }
  }

}
