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

package org.mule.test.util.compression;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.util.compression.CompressionHelper;
import org.mule.util.compression.CompressionStrategy;

/**
 * <code>TestCompression</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CompressionTestCase extends TestCase
{

    /**
     * logger used by this class
     */
    private static transient Log logger = LogFactory.getLog(CompressionTestCase.class);

    public static void main(String[] args)
    {
        TestRunner.run(suite());
    }

    public static Test suite()
    {
        return new TestSuite(CompressionTestCase.class);
    }

    public CompressionTestCase(String testName)
    {
        super(testName);
    }


    // Test cases
    //-------------------------------------------------------------------------

    public void testCompressDefaultGZip() throws Exception
    {
        logger.debug("testCompressDefaultGZip");

        String temp = "This is a compressed string";
        CompressionStrategy strategy = CompressionHelper.getCompressionStrategy();
        byte[] compressed = strategy.compressByteArray(temp.getBytes());
        //For small test data the compressed data will be bigger than the real data
        assertTrue(compressed.length > temp.getBytes().length);

        byte[] uncompressed = strategy.uncompressByteArray(compressed);
        assertTrue(uncompressed.length == temp.getBytes().length);

        assertEquals(temp, new String(uncompressed));

        String tempLarge = temp;
        for (int i = 0; i < 100; i++)
        {
            tempLarge += temp;
        }

        compressed = strategy.compressByteArray(tempLarge.getBytes());

        assertTrue(compressed.length < tempLarge.getBytes().length);

        uncompressed = strategy.uncompressByteArray(compressed);
        assertTrue(uncompressed.length == tempLarge.getBytes().length);

        assertEquals(tempLarge, new String(uncompressed));

    }

}
