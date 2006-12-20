/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

/*
 * Copyright 2001-2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.providers.email.transformers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class PlainTextDataSource implements DataSource
{
    public static final String CONTENT_TYPE = "text/plain";

    private final String name;
    private byte[] data;
    private ByteArrayOutputStream os;

    public PlainTextDataSource(String name, String data)
    {
        this.name = name;
        this.data = data == null ? null : data.getBytes();
        os = new ByteArrayOutputStream();
    } // ctor

    public String getName()
    {
        return name;
    } // getName

    public String getContentType()
    {
        return CONTENT_TYPE;
    } // getContentType

    public InputStream getInputStream() throws IOException
    {
        if (os.size() != 0)
        {
            data = os.toByteArray();
        }
        return new ByteArrayInputStream(data == null ? new byte[0] : data);
    } // getInputStream

    public OutputStream getOutputStream() throws IOException
    {
        if (os.size() != 0)
        {
            data = os.toByteArray();
        }
        return new ByteArrayOutputStream();
    } // getOutputStream

} // class PlainTextDataSource
