/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.db.internal.resolver.query;

import org.mule.api.MuleEvent;
import org.mule.module.db.internal.parser.QueryTemplateParser;
import org.mule.module.db.internal.util.FileReader;

import java.io.IOException;

/**
 * Resolves a bulk query reading the queries from a file
 */
public class FileBulkQueryResolver extends AbstractBulkQueryResolver
{

    private final String file;
    private final FileReader fileReader;

    public FileBulkQueryResolver(String file, QueryTemplateParser queryTemplateParser, FileReader fileReader)
    {
        super(null, queryTemplateParser);
        this.file = file;
        this.fileReader = fileReader;
    }

    @Override
    protected String resolveBulkQueries(MuleEvent muleEvent, String bulkQuery)
    {
        try
        {
            return fileReader.getResourceAsString(file);
        }
        catch (IOException e)
        {
            throw new QueryResolutionException("Unable to read bulk query file: " + file);
        }
    }
}
