/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 *
 */

package org.mule.providers.http;

import java.io.BufferedWriter;
import java.io.FilterWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * Provides a hybrid Writer/OutputStream for sending HTTP response data
 */
public class ResponseWriter extends FilterWriter {
    public static final String CRLF = "\r\n";
    public static final String ISO_8859_1 = "ISO-8859-1";
    private OutputStream outStream = null;
    private String encoding = null;

    public ResponseWriter(final OutputStream outStream) 
    throws UnsupportedEncodingException {
        this(outStream, CRLF, ISO_8859_1);
    }
    
    public ResponseWriter(final OutputStream outStream, final String encoding) 
    throws UnsupportedEncodingException {
        this(outStream, CRLF, encoding);
    }
    
    public ResponseWriter(
            final OutputStream outStream, 
            final String lineSeparator, 
            final String encoding) throws UnsupportedEncodingException {
        super(new BufferedWriter(new OutputStreamWriter(outStream, encoding)));
        this.outStream = outStream;
        this.encoding = encoding;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public void close() throws IOException {
        if(outStream != null) {
            super.close();
            outStream = null;
        }
    }

    /* (non-Javadoc)
     * @see java.io.Writer#flush()
     */
    public void flush() throws IOException {
        if(outStream != null) {
            super.flush();
            outStream.flush();
        }
    }

    public void write(byte b) throws IOException {
        super.flush();
        outStream.write((int)b);
    }
    
    public void write(byte[] b) throws IOException {
        super.flush();
        outStream.write(b);
    }
    
    public void write(byte[] b, int off, int len) throws IOException {
        super.flush();
        outStream.write(b,off,len);
    }

    public void print(String s) throws IOException {
        if (s == null) {
            s = "null";
        }
        write(s);
    }
    
    public void print(int i) throws IOException {
        write(Integer.toString(i));
    }
    
    public void println(int i) throws IOException {
        write(Integer.toString(i));
        write(CRLF);
    }

    public void println(String s) throws IOException {
        print(s);
        write(CRLF);
    }
    
    public void println() throws IOException {
        write(CRLF);
    }
    
}
