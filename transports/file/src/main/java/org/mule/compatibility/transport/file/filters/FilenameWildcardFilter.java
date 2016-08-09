/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.file.filters;

import org.mule.compatibility.transport.file.FileConnector;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.routing.filters.WildcardFilter;

import java.io.File;
import java.io.FilenameFilter;

/**
 * <code>FilenameWildcardFilter</code> filters incoming files from a directory, based on file patterns.
 */
public class FilenameWildcardFilter extends WildcardFilter implements FilenameFilter {

  public FilenameWildcardFilter() {
    super();
  }

  public FilenameWildcardFilter(String pattern) {
    super(pattern);
  }

  /**
   * Filter condition decider method.
   * <p/>
   * Returns <code>boolean</code> <code>TRUE</code> if the file conforms to an acceptable pattern or <code>FALSE</code> otherwise.
   * 
   * @param dir The directory to apply the filter to.
   * @param name The name of the file to apply the filter to.
   * @return indication of acceptance as boolean.
   */
  @Override
  public boolean accept(File dir, String name) {
    if (name == null) {
      logger.warn("The filename and/or directory was null");
      return false;
    } else {
      return accept(name);
    }
  }

  @Override
  public boolean accept(MuleMessage message) {
    Object filename = message.getInboundProperty(FileConnector.PROPERTY_ORIGINAL_FILENAME);
    return accept(filename);
  }

}
