/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.http.internal;

import static java.lang.String.format;
import static org.mule.extension.http.api.error.HttpError.NOT_FOUND;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.extension.http.api.HttpRequestAttributes;
import org.mule.extension.http.api.error.ResourceNotFoundException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.util.IOUtils;
import org.mule.runtime.core.util.StringUtils;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.runtime.operation.Result;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.activation.MimetypesFileTypeMap;

public class StaticResourceLoader {

  private static final String ANY_PATH = "/*";
  private static final String ROOT_PATH = "/";
  private static final String DEFAULT_MIME_TYPE = "application/octet-stream";
  private MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();

  /**
   * The resource base from where documents are served up. For example: /Users/maxthemule/resources
   */
  @Parameter
  private String resourceBasePath;

  /**
   * The default file to serve when a directory is specified. The default value is 'index.html'.
   */
  @Parameter
  @Optional(defaultValue = "index.html")
  private String defaultFile;

  /**
   * The {@link HttpRequestAttributes} coming from an HTTP listener source to check the required resources.
   */
  @Parameter
  @Optional(defaultValue = "#[attributes]")
  private HttpRequestAttributes attributes;

  public Result load() throws ResourceNotFoundException {
    // TODO: MULE-10163 - Analyse removing the static resource loader in favor of file read
    String path = attributes.getRequestPath();
    String contextPath = attributes.getListenerPath();

    // Get rid of ending wildcards.
    if (contextPath.equals(ANY_PATH)) {
      contextPath = ROOT_PATH;
    }
    if (contextPath.endsWith(ANY_PATH)) {
      contextPath = StringUtils.removeEnd(contextPath, ANY_PATH);
    }

    if (!ROOT_PATH.equals(contextPath)) {
      // Remove the listener context path from the request as this isn't part of the path.
      path = path.substring(contextPath.length());
    }

    File file = new File(resourceBasePath + path);
    Result result;

    if (file.isDirectory()) {
      if (!path.endsWith("/")) {
        // Just fix the path, don't force a redirect
        path = path + "/";
      }
      file = new File(resourceBasePath + path + defaultFile);
    }

    InputStream in = null;
    try {
      in = new FileInputStream(file);

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      IOUtils.copyLarge(in, baos);

      byte[] buffer = baos.toByteArray();

      String mimeType = mimeTypes.getContentType(file);
      if (mimeType == null) {
        mimeType = DEFAULT_MIME_TYPE;
      }

      result = Result.builder().output(buffer).mediaType(MediaType.parse(mimeType)).build();
      return result;
    } catch (IOException e) {
      throw new ResourceNotFoundException(e, NOT_FOUND, getExceptionMessage(path));
    } finally {
      IOUtils.closeQuietly(in);
    }
  }

  private I18nMessage getExceptionMessage(String path) {
    return createStaticMessage(format("The file: %s was not found.", resourceBasePath + path));
  }

}
