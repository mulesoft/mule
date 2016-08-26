/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extension.db.internal.resolver.query;

import org.mule.extension.db.internal.parser.QueryTemplateParser;
import org.mule.extension.db.internal.util.FileReader;

import java.io.IOException;

/**
 * Resolves a bulk query reading the queries from a file
 */
public class FileBulkQueryFactory extends BulkQueryFactory {

  private final String file;
  private final FileReader fileReader;

  public FileBulkQueryFactory(String file, QueryTemplateParser queryTemplateParser, FileReader fileReader) {
    super(queryTemplateParser);
    this.file = file;
    this.fileReader = fileReader;
  }

  @Override
  protected String resolveBulkQueries() {
    try {
      return fileReader.getResourceAsString(file);
    } catch (IOException e) {
      throw new QueryResolutionException("Unable to read script file: " + file);
    }
  }

  @Override
  public String toString() {
    return file;
  }
}
