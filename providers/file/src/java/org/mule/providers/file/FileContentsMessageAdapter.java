/* 
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk 
 * 
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file. 
 *
 */

package org.mule.providers.file;

import org.mule.providers.AbstractMessageAdapter;
import org.mule.providers.file.transformers.FileToByteArray;
import org.mule.umo.MessageException;
import org.mule.umo.provider.MessageTypeNotSupportedException;
import org.mule.umo.provider.UniqueIdNotSupportedException;
import org.mule.umo.transformer.TransformerException;

import java.io.File;

/**
 * <code>FileMessageAdapter</code> provides a wrapper for a message.  Users can
 * obtain the contents of the message through the payload property and can get the
 * filename and directory in the properties using PROPERTY_FILENAME and
 * PROPERTY_DIRECTORY
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class FileContentsMessageAdapter extends AbstractMessageAdapter
{
    private FileToByteArray transformer = new FileToByteArray();

    private byte[] message = null;
    private File file = null;

    public FileContentsMessageAdapter(Object message) throws MessageException
    {
        if(message instanceof File) {
            setMessage((File)message);
        } else {
            throw new MessageTypeNotSupportedException(message, getClass());
        }
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayload()
     */
    public Object getPayload()
    {
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsBytes()
     */
    public byte[] getPayloadAsBytes() throws Exception
    {
        return message;
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#getPayloadAsString()
     */
    public String getPayloadAsString() throws Exception
    {
        return new String(getPayloadAsBytes());
    }

    /* (non-Javadoc)
     * @see org.mule.providers.UMOMessageAdapter#setMessage(java.lang.Object)
     */
    private void setMessage(File message) throws MessageException
    {
        try
        {
            this.file = message;
            this.message = (byte[])transformer.transform(message);
            properties.put(FileConnector.PROPERTY_ORIGINAL_FILENAME, this.file.getName());
            properties.put(FileConnector.PROPERTY_DIRECTORY, this.file.getAbsolutePath());
        } catch (TransformerException e)
        {
            throw new MessageException("Failed to read file: " + file.getAbsolutePath() + ". " + e.getMessage(), e);
        }
    }

    public File getFile()
    {
        return file;
    }

    public String getUniqueId() throws UniqueIdNotSupportedException
    {
        return file.getAbsolutePath();
    }
}
