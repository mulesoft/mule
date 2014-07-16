/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.http.multipart;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/* ================================================================ */
/** Handle a multipart MIME response.
 *
 * @author Greg Wilkins
 * @author Jim Crossley
*/
public class MultiPartOutputStream extends FilterOutputStream
{
    /* ------------------------------------------------------------ */
    private static byte[] __CRLF;
    private static byte[] __DASHDASH;
    private String encoding;
   

    /* ------------------------------------------------------------ */
    private String boundary;
    private byte[] boundaryBytes;

    /* ------------------------------------------------------------ */
    private boolean inPart=false;    
    
    /* ------------------------------------------------------------ */
    public MultiPartOutputStream(OutputStream out, String encoding)
    throws IOException
    {
        super(out);
        this.encoding = encoding;
        
        __CRLF="\015\012".getBytes(encoding);
        __DASHDASH="--".getBytes(encoding);
        
        boundary = "mule"+System.identityHashCode(this)+
        Long.toString(System.currentTimeMillis(),36);
        boundaryBytes=boundary.getBytes(encoding);

        inPart=false;
    }

    

    /* ------------------------------------------------------------ */
    /** End the current part.
     * @exception IOException IOException
     */
    public void close()
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__DASHDASH);
        out.write(__CRLF);
        inPart=false;
        super.close();
    }
    
    /* ------------------------------------------------------------ */
    public String getBoundary()
    {
        return boundary;
    }

    public OutputStream getOut() {return out;}
    
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        inPart=true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        out.write(("Content-Type: "+contentType).getBytes(encoding));
        out.write(__CRLF);
        out.write(__CRLF);
    }
        
    /* ------------------------------------------------------------ */
    /** Start creation of the next Content.
     */
    public void startPart(String contentType, String[] headers)
         throws IOException
    {
        if (inPart)
            out.write(__CRLF);
        inPart=true;
        out.write(__DASHDASH);
        out.write(boundaryBytes);
        out.write(__CRLF);
        out.write(("Content-Type: "+contentType).getBytes(encoding));
        out.write(__CRLF);
        for (int i=0;headers!=null && i<headers.length;i++)
        {
            out.write(headers[i].getBytes(encoding));
            out.write(__CRLF);
        }
        out.write(__CRLF);
    }
    
}
