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
package org.mule.test.providers.file;

import org.mule.providers.file.FileMessageAdapter;
import org.mule.umo.provider.UMOMessageAdapter;

import java.io.File;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public class FileMessageAdapterTestCase extends org.mule.tck.providers.AbstractMessageAdapterTestCase
{
    private File message;

    /*
	 * (non-Javadoc)
	 * 
	 * @see junit.framework.TestCase#setUp()
	 */
    protected void setUp() throws Exception
    {
        super.setUp();
        message = File.createTempFile("simple", ".mule");
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#getValidMessage()
	 */
    public Object getValidMessage()
    {
        return message;
    }

    /*
	 * (non-Javadoc)
	 * 
	 * @see org.mule.tck.providers.AbstractMessageAdapterTestCase#createAdapter()
	 */
    public UMOMessageAdapter createAdapter(Object payload) throws Exception
    {
        return new FileMessageAdapter((File)payload);
    }
}
