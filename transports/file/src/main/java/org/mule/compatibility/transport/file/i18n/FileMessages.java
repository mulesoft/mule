/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.i18n;

import org.mule.compatibility.core.api.endpoint.EndpointURI;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.i18n.I18nMessageFactory;

import java.io.File;

public class FileMessages extends I18nMessageFactory {

  private static final FileMessages factory = new FileMessages();

  private static final String BUNDLE_PATH = getBundlePath("file");

  public static I18nMessage errorWhileListingFiles() {
    return factory.createMessage(BUNDLE_PATH, 1);
  }

  public static I18nMessage exceptionWhileProcessing(String name, String string) {
    return factory.createMessage(BUNDLE_PATH, 2, name, string);
  }

  public static I18nMessage failedToDeleteFile(File file) {
    return factory.createMessage(BUNDLE_PATH, 3, file.getAbsolutePath());
  }

  public static I18nMessage failedToMoveFile(String from, String to) {
    return factory.createMessage(BUNDLE_PATH, 4, from, to);
  }

  public static I18nMessage moveToDirectoryNotWritable() {
    return factory.createMessage(BUNDLE_PATH, 5);
  }

  public static I18nMessage invalidFileFilter(EndpointURI endpointURI) {
    return factory.createMessage(BUNDLE_PATH, 6, endpointURI);
  }

  public static I18nMessage fileDoesNotExist(String string) {
    return factory.createMessage(BUNDLE_PATH, 7, string);
  }

  public static I18nMessage invalidFilter(Object filter) {
    return factory.createMessage(BUNDLE_PATH, 8, filter.getClass().getName());
  }

}


