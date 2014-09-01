/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.multipart;
// ========================================================================
// Copyright (c) 2006-2010 Mort Bay Consulting Pty. Ltd.
// ------------------------------------------------------------------------
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// and Apache License v2.0 which accompanies this distribution.
// The Eclipse Public License is available at
// http://www.eclipse.org/legal/epl-v10.html
// The Apache License v2.0 is available at
// http://www.opensource.org/licenses/apache2.0.php
// You may elect to redistribute this code under either of these licenses.
// ========================================================================


import org.mule.model.streaming.DeleteOnCloseFileInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletException;

import org.apache.commons.io.input.AutoCloseInputStream;

/**
* MultipartInputStream
*
* Handle a MultiPart Mime input stream, breaking it up on the boundary into files and strings.
*/
public class MultiPartInputStream
{
    public static final MultipartConfiguration __DEFAULT_MULTIPART_CONFIG = new MultipartConfiguration(System.getProperty("java.io.tmpdir"));
    protected InputStream _in;
    protected MultipartConfiguration _config;
    protected String _contentType;
    protected MultiMap _map;
    protected Map<String, Part> _parts;
    protected File _tmpDir;




    public class MultiPart implements Part
    {
        protected String _name;
        protected String _filename;
        protected File _file;
        protected OutputStream _out;
        protected String _contentType;
        protected MultiMap<String> _headers;
        protected long _size = 0;

        public MultiPart (String name, String filename)
        throws IOException
        {
            _name = name;
            _filename = filename;
        }

        protected void setContentType (String contentType)
        {
            _contentType = contentType;
        }


        protected void open()
        throws FileNotFoundException, IOException
        {
            //We will either be writing to a file, if it has a filename on the content-disposition
            //and otherwise a byte-array-input-stream, OR if we exceed the getFileSizeThreshold, we
            //will need to change to write to a file.
            if (_filename != null && _filename.trim().length() > 0)
            {
                createFile();
            }
            else
            {
                //Write to a buffer in memory until we discover we've exceed the
                //MultipartConfig fileSizeThreshold
                _out = new ByteArrayOutputStream();
            }
        }

        protected void close()
        throws IOException
        {
            _out.close();
        }


        protected void write (int b)
        throws IOException
        {
            if (MultiPartInputStream.this._config.getMaxFileSize() > 0 && _size + 1 > MultiPartInputStream.this._config.getMaxFileSize())
                throw new IOException ("Multipart Mime part "+_name+" exceeds max filesize");

            if (MultiPartInputStream.this._config.getFileSizeThreshold() > 0 && _size + 1 > MultiPartInputStream.this._config.getFileSizeThreshold() && _file==null)
                createFile();
            _out.write(b);
            _size ++;
        }

        protected void write (byte[] bytes, int offset, int length)
        throws IOException
        {
            if (MultiPartInputStream.this._config.getMaxFileSize() > 0 && _size + length > MultiPartInputStream.this._config.getMaxFileSize())
                throw new IOException ("Multipart Mime part "+_name+" exceeds max filesize");

            if (MultiPartInputStream.this._config.getFileSizeThreshold() > 0 && _size + length > MultiPartInputStream.this._config.getFileSizeThreshold() && _file==null)
                createFile();

            _out.write(bytes, offset, length);
            _size += length;
        }

        protected void createFile ()
        throws IOException
        {
            _file = File.createTempFile("MultiPart", "", MultiPartInputStream.this._tmpDir);
            FileOutputStream fos = new FileOutputStream(_file);
            BufferedOutputStream bos = new BufferedOutputStream(fos);

            if (_size > 0 && _out != null)
            {
                //already written some bytes, so need to copy them into the file
                _out.flush();
                ((ByteArrayOutputStream)_out).writeTo(bos);
                _out.close();
            }
            _out = bos;
        }



        protected void setHeaders(MultiMap<String> headers)
        {
            _headers = headers;
        }

        /**
         * @see Part#getContentType()
         */
        public String getContentType()
        {
            return _contentType;
        }

        /**
         * @see Part#getHeader(java.lang.String)
         */
        public String getHeader(String name)
        {
            return (String)_headers.getValue(name, 0);
        }

        /**
         * @see Part#getHeaderNames()
         */
        public Collection<String> getHeaderNames()
        {
            return _headers.keySet();
        }

        /**
         * @see Part#getHeaders(java.lang.String)
         */
        public Collection<String> getHeaders(String name)
        {
           return _headers.getValues(name);
        }

        /**
         * @see Part#getInputStream()
         */
        public InputStream getInputStream() throws IOException
        {
           if (_file != null)
           {
               // Automatically close and delete the temp file when end of input has been reached (MULE-6732).
               return new BufferedInputStream(new AutoCloseInputStream(new DeleteOnCloseFileInputStream(_file)));
           }
           else
           {
               //part content is in a ByteArrayOutputStream
               return new ByteArrayInputStream(((ByteArrayOutputStream)_out).toByteArray());
           }
        }

        /**
         * @see Part#getName()
         */
        public String getName()
        {
           return _name;
        }

        /**
         * @see Part#getSize()
         */
        public long getSize()
        {
            return _size;
        }

        /**
         * @see Part#write(java.lang.String)
         */
        public void write(String fileName) throws IOException
        {
            if (_file == null)
            {
                //part data is only in the ByteArrayOutputStream and never been written to disk
                _file = new File (_tmpDir, fileName);
                BufferedOutputStream bos = null;
                try
                {
                    bos = new BufferedOutputStream(new FileOutputStream(_file));
                    ((ByteArrayOutputStream)_out).writeTo(bos);
                    bos.flush();
                }
                finally
                {
                    if (bos != null)
                        bos.close();
                }
            }
            else
            {
                //the part data is already written to a temporary file, just rename it
                _file.renameTo(new File(_tmpDir, fileName));
            }
        }

        /**
         * @see Part#delete()
         */
        public void delete() throws IOException
        {
            if (_file != null)
                _file.delete();
        }


        /**
         * Get the file, if any, the data has been written to.
         * @return
         */
        public File getFile ()
        {
            return _file;
        }


        /**
         * Get the filename from the content-disposition.
         * @return null or the filename
         */
        public String getContentDispositionFilename ()
        {
            return _filename;
        }
    }




    /**
     * @param in Request input stream
     * @param contentType Content-Type header
     * @param config MultipartConfiguration
     */
    public MultiPartInputStream(InputStream in, String contentType, MultipartConfiguration config)
    {
        _in = new BufferedInputStream(in);
       _contentType = contentType;
       _config = config;
       if (_config == null)
           _config = __DEFAULT_MULTIPART_CONFIG;
    }



    public Collection<Part> getParts()
    throws IOException
    {
        parse();
        return _parts.values();
    }


    public Part getPart(String name)
    throws IOException, ServletException
    {
        parse();
        return _parts.get(name);
    }


    public MultiMap getMap ()
    throws IOException, ServletException
    {
        parse();
        return _map;
    }


    protected void parse ()
    throws IOException
    {
        //have we already parsed the input?
        if (_parts != null)
            return;

        //initialize
        long total = 0; //keep running total of size of bytes read from input and throw an exception if exceeds MultipartConfiguration._maxRequestSize
        _parts = new HashMap<String, Part>();

        //if its not a multipart request, don't parse it
        if (_contentType == null || !_contentType.startsWith("multipart/form-data"))
            return;

        //sort out the location to which to write the files
        String location = __DEFAULT_MULTIPART_CONFIG.getLocation();
        location = ("".equals(_config.getLocation())? location : _config.getLocation());

        _tmpDir = new File(location);
        if (!_tmpDir.exists())
            _tmpDir.mkdirs();


        String boundary="--"+value(_contentType.substring(_contentType.indexOf("boundary=")));
        byte[] byteBoundary=(boundary+"--").getBytes("ISO-8859-1");

        // Get first boundary
        byte[] bytes;
        String line;
        do
        {
            bytes = readLine(_in);
            line = bytes == null ? null : new String(bytes, "UTF-8");
        }
        while (line != null && !line.equals(boundary));

        if (line == null)
        {
            throw new IOException("Missing initial multi part boundary");
        }

        // Read each part
        boolean lastPart=false;
        String contentDisposition=null;
        String contentType=null;
        outer:while(!lastPart)
        {
            MultiMap<String> headers = new MultiMap<String>();
            while(true)
            {
                bytes=readLine(_in);
                if(bytes==null)
                    break outer;

                // If blank line, end of part headers
                if(bytes.length==0)
                    break;

                total += bytes.length;
                if (_config.getMaxRequestSize() > 0 && total > _config.getMaxRequestSize())
                    throw new IOException ("Request exceeds maxRequestSize ("+_config.getMaxRequestSize()+")");

                line=new String(bytes,"UTF-8");

                //get content-disposition and content-type
                int c=line.indexOf(':',0);
                if(c>0)
                {
                    String key=line.substring(0,c).trim().toLowerCase();
                    String value=line.substring(c+1,line.length()).trim();
                    headers.put(key, value);
                    if (key.equalsIgnoreCase("content-disposition"))
                        contentDisposition=value;
                    if (key.equalsIgnoreCase("content-type"))
                        contentType = value;
                }
            }

            // Extract content-disposition
            boolean form_data=false;
            if(contentDisposition==null)
            {
                throw new IOException("Missing content-disposition");
            }

            StringTokenizer tok=new StringTokenizer(contentDisposition,";");
            String name=null;
            String filename=null;
            while(tok.hasMoreTokens())
            {
                String t=tok.nextToken().trim();
                String tl=t.toLowerCase();
                if(t.startsWith("form-data"))
                    form_data=true;
                else if(tl.startsWith("name="))
                    name=value(t);
                else if(tl.startsWith("filename="))
                    filename=value(t);
            }

            // Check disposition
            if(!form_data)
            {
                continue;
            }
            //It is valid for reset and submit buttons to have an empty name.
            //If no name is supplied, the browser skips sending the info for that field.
            //However, if you supply the empty string as the name, the browser sends the
            //field, with name as the empty string. So, only continue this loop if we
            //have not yet seen a name field.
            if(name==null)
            {
                continue;
            }

            //Have a new Part
            MultiPart part = new MultiPart(name, filename);
            part.setHeaders(headers);
            part.setContentType(contentType);
            _parts.put(name, part);

            part.open();

            try
            {
                int state=-2;
                int c;
                boolean cr=false;
                boolean lf=false;

                // loop for all lines`
                while(true)
                {
                    int b=0;
                    while((c=(state!=-2)?state:_in.read())!=-1)
                    {
                        total ++;
                        if (_config.getMaxRequestSize() > 0 && total > _config.getMaxRequestSize())
                            throw new IOException("Request exceeds maxRequestSize ("+_config.getMaxRequestSize()+")");

                        state=-2;
                        // look for CR and/or LF
                        if(c==13||c==10)
                        {
                            if(c==13)
                                state=_in.read();
                            break;
                        }
                        // look for boundary
                        if(b>=0&&b<byteBoundary.length&&c==byteBoundary[b])
                            b++;
                        else
                        {
                            // this is not a boundary
                            if(cr)
                                part.write(13);

                            if(lf)
                                part.write(10);

                            cr=lf=false;
                            if(b>0)
                                part.write(byteBoundary,0,b);

                            b=-1;
                            part.write(c);
                        }
                    }
                    // check partial boundary
                    if((b>0&&b<byteBoundary.length-2)||(b==byteBoundary.length-1))
                    {
                        if(cr)
                            part.write(13);

                        if(lf)
                            part.write(10);

                        cr=lf=false;
                        part.write(byteBoundary,0,b);
                        b=-1;
                    }
                    // boundary match
                    if(b>0||c==-1)
                    {
                        if(b==byteBoundary.length)
                            lastPart=true;
                        if(state==10)
                            state=-2;
                        break;
                    }
                    // handle CR LF
                    if(cr)
                        part.write(13);

                    if(lf)
                        part.write(10);

                    cr=(c==13);
                    lf=(c==10||state==10);
                    if(state==10)
                        state=-2;
                }
            }
            finally
            {

                part.close();
            }
        }
    }


    /* ------------------------------------------------------------ */
    private String value(String nameEqualsValue)
    {
        String value=nameEqualsValue.substring(nameEqualsValue.indexOf('=')+1).trim();
        int i=value.indexOf(';');
        if(i>0)
            value=value.substring(0,i);
        if(value.startsWith("\""))
        {
            value=value.substring(1,value.indexOf('"',1));
        }
        else
        {
            i=value.indexOf(' ');
            if(i>0)
                value=value.substring(0,i);
        }
        return value;
    }

    public static int CR = '\015';
    public static int LF = '\012';

    private byte[] readLine(InputStream in) throws IOException
    {
        byte[] buf = new byte[256];

        int i=0;
        int loops=0;
        int ch=0;

        while (true)
        {
            ch=in.read();
            if (ch<0)
                break;
            loops++;

            // skip a leading LF's
            if (loops==1 && ch==LF)
                continue;

            if (ch==CR || ch==LF)
                break;

            if (i>=buf.length)
            {
                byte[] old_buf=buf;
                buf=new byte[old_buf.length+256];
                System.arraycopy(old_buf, 0, buf, 0, old_buf.length);
            }
            buf[i++]=(byte)ch;
        }

        if (ch==-1 && i==0)
            return null;

        // skip a trailing LF if it exists
        if (ch==CR && in.available()>=1 && in.markSupported())
        {
            in.mark(1);
            ch=in.read();
            if (ch!=LF)
                in.reset();
        }

        byte[] old_buf=buf;
        buf=new byte[i];
        System.arraycopy(old_buf, 0, buf, 0, i);

        return buf;
    }

}
