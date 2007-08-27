/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.tcp.protocols;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * The XmlMessageProtocol is an application level tcp protocol that can be used to
 * read streaming xml documents. The only requirement is that each document include
 * an xml declaration at the beginning of the document of the form "<?xml...". In
 * section 2.8, the xml 1.0 standard contains "Definition: XML documents
 * <strong>SHOULD</strong> begin with an XML declaration which specifies the version
 * of XML being used" while the xml 1.1 standard contains "Definition: XML 1.1
 * documents <strong>MUST</strong> begin with an XML declaration which specifies the
 * version of XML being used". The SHOULD indicates a recommendation that, if not
 * followed, needs to be carefully checked for unintended consequences. MUST
 * indicates a mandatory requirement for a well-formed document. Please make sure
 * that the xml documents being streamed begin with an xml declaration when using
 * this class.
 * </p>
 * <p>
 * Data are read until a new document is found or there are no more data
 * (momentarily).  For slower networks,
 * {@link org.mule.providers.tcp.protocols.XmlMessageEOFProtocol} may be more reliable.
 * </p>
 * <p>
 * Also, the default character encoding for the platform is used to decode the
 * message bytes when looking for the XML declaration. Some caution with message
 * character encodings is warranted.
 * </p>
 * <p>
 * Finally, this class uses a PushbackInputStream to enable parsing of individual
 * messages. The stream stores any pushed-back bytes into it's own internal buffer
 * and not the original stream. Therefore, the read buffer size is intentionally
 * limited to insure that unread characters remain on the stream so that all data may
 * be read later.
 * </p>
 */
public class XmlMessageProtocol extends ByteProtocol
{
    private static final String XML_PATTERN = "<?xml";

    private static final int READ_BUFFER_SIZE = 4096;
    private static final int PUSHBACK_BUFFER_SIZE = READ_BUFFER_SIZE * 2;

    private ConcurrentHashMap pbMap = new ConcurrentHashMap();

    public XmlMessageProtocol()
    {
        super(STREAM_OK);
    }

    public Object read(InputStream is) throws IOException
    {
        PushbackInputStream pbis = (PushbackInputStream) pbMap.get(is);
        if (null == pbis)
        {
            pbis = new PushbackInputStream(is, PUSHBACK_BUFFER_SIZE);
            PushbackInputStream prev = (PushbackInputStream) pbMap.putIfAbsent(is, pbis);
            pbis = null == prev ? pbis : prev;
        }

        int len = -1;
        try
        {
            // read until xml pattern is seen (and then pushed back) or no more data
            // to read. return all data as message
            byte[] buffer = new byte[READ_BUFFER_SIZE];
            StringBuffer message = new StringBuffer(READ_BUFFER_SIZE);
            int patternIndex = -1;
            boolean repeat;
            do
            {
                len = safeRead(pbis, buffer);
                if (len >= 0)
                {
                    // TODO take encoding into account, ideally from the incoming XML
                    message.append(new String(buffer, 0, len));
                    // start search at 2nd character in buffer (index=1) to
                    // indicate whether we have reached a new document.
                    patternIndex = message.toString().indexOf(XML_PATTERN, 1);
                    repeat = isRepeat(patternIndex, len, pbis.available());
                }
                else
                {
                    // never repeat on closed stream (and avoid calling available)
                    repeat = false;
                }

            }
            while (repeat);

            if (patternIndex > 0)
            {
                // push back the start of the next message and
                // ignore the pushed-back characters in the return buffer
                pbis.unread(message.substring(patternIndex, message.length()).getBytes());
                message.setLength(patternIndex);
            }

            // TODO encoding here, too...
            return nullEmptyArray(message.toString().getBytes());

        }
        finally
        {
            // TODO - this doesn't seem very reliable, since loop above can end
            // without EOF.  On the other hand, what else can we do?  Entire logic
            // is not very dependable, IMHO.  XmlMessageEOFProtocol is more likely
            // to be correct here, I think.

            // clear from map if stream has ended
            if (len < 0)
            {
                pbMap.remove(is);
            }
        }
    }

    /**
     * Show we continue reading?  This class, following previous implementations, only
     * reads while input is saturated.
     * @see XmlMessageEOFProtocol
     *
     * @param patternIndex The index of the xml tag (or -1 if the next message not found)
     * @param len The amount of data read this loop (or -1 if EOF)
     * @param available The amount of data available to read
     * @return true if the read should continue
     */
    protected boolean isRepeat(int patternIndex, int len, int available)
    {
        return patternIndex < 0 && len == READ_BUFFER_SIZE && available > 0;
    }
}
