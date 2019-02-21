/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public final class JarUtils
{
    private static final String MULE_MODULE_FILENAME = "lib" + File.separator + "module";
    private static final String MULE_LIB_FILENAME = "lib" + File.separator + "mule";
    private static final String MULE_HOME = System.getProperty("mule.home");
    
    public static final String MULE_LOCAL_JAR_FILENAME = "mule-local-install.jar";

    private static final Log logger = LogFactory.getLog(JarUtils.class);

    private JarUtils()
    {
        // utility class only
    }

    public static LinkedHashMap readJarFileEntries(File jarFile) throws Exception
    {
        LinkedHashMap entries = new LinkedHashMap();
        JarFile jarFileWrapper = null;
        if (jarFile != null)
        {
            logger.debug("Reading jar entries from " + jarFile.getAbsolutePath());
            try
            {
                jarFileWrapper = new JarFile(jarFile);
                Enumeration iter = jarFileWrapper.entries();
                while (iter.hasMoreElements())
                {
                    ZipEntry zipEntry = (ZipEntry) iter.nextElement();
                    InputStream entryStream = jarFileWrapper.getInputStream(zipEntry);
                    ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
                    try
                    {
                        IOUtils.copy(entryStream, byteArrayStream);
                        entries.put(zipEntry.getName(), byteArrayStream.toByteArray());
                        logger.debug("Read jar entry " + zipEntry.getName() + " from " + jarFile.getAbsolutePath());
                    }
                    finally
                    {
                        byteArrayStream.close();
                    }
                }
            }
            finally
            {
                if (jarFileWrapper != null)
                {
                    try
                    {
                        jarFileWrapper.close();
                    }
                    catch (Exception ignore)
                    {
                        logger.debug(ignore);
                    }
                }
            }
        }
        return entries;
    }
    
    public static void appendJarFileEntries(File jarFile, LinkedHashMap entries) throws Exception
    {
        if (entries != null)
        {
            LinkedHashMap combinedEntries = readJarFileEntries(jarFile);
            combinedEntries.putAll(entries);
            File tmpJarFile = File.createTempFile(jarFile.getName(), null);
            createJarFileEntries(tmpJarFile, combinedEntries);
            FileUtils.deleteFile(jarFile);
            FileUtils.renameFile(tmpJarFile, jarFile);
        }
    }
    
    public static void createJarFileEntries(File jarFile, LinkedHashMap entries) throws Exception
    {
        JarOutputStream jarStream = null;
        FileOutputStream fileStream = null;
        
        if (jarFile != null) 
        {
            logger.debug("Creating jar file " + jarFile.getAbsolutePath());

            try
            {
                fileStream = new FileOutputStream(jarFile);
                jarStream = new JarOutputStream(fileStream);
                
                if (entries != null &&  !entries.isEmpty())
                {
                    Iterator iter = entries.keySet().iterator();
                    while (iter.hasNext())
                    {
                        String jarFilePath = (String) iter.next();
                        Object content = entries.get(jarFilePath);
                        
                        JarEntry entry = new JarEntry(jarFilePath);
                        jarStream.putNextEntry(entry);
                        
                        logger.debug("Adding jar entry " + jarFilePath + " to " + jarFile.getAbsolutePath());
                        
                        if (content instanceof String)
                        {
                            writeJarEntry(jarStream, ((String) content).getBytes());
                        }
                        else if (content instanceof byte[])
                        {
                            writeJarEntry(jarStream, (byte[]) content);
                        }
                        else if (content instanceof File)
                        {
                            writeJarEntry(jarStream, (File) content);
                        }
                    }
                }
                
                jarStream.flush();
                fileStream.getFD().sync();
            }
            finally
            {
                if (jarStream != null)
                {
                    try
                    {
                        jarStream.close();
                    }
                    catch (Exception jarNotClosed)
                    {
                        logger.debug(jarNotClosed);
                    }
                }
                if (fileStream != null)
                {
                    try
                    {
                        fileStream.close();    
                    }
                    catch (Exception fileNotClosed)
                    {
                        logger.debug(fileNotClosed);
                    }
                }
            }
        }
    }
    
    private static void writeJarEntry(OutputStream stream, byte[] entry) throws IOException
    {
        stream.write(entry, 0, entry.length);
    }
    
    private static void writeJarEntry(OutputStream stream, File entry) throws IOException
    {
        FileInputStream fileContentStream = null;
        try
        {
            fileContentStream = new FileInputStream(entry);
            IOUtils.copy(fileContentStream, stream);         
        }
        finally
        {
            if (fileContentStream != null)
            {
                try
                {
                    fileContentStream.close();
                }
                catch (Exception fileContentNotClosed)
                {
                    logger.debug(fileContentNotClosed);
                }
            }
        }
    }

    public static File getMuleHomeFile()
    {
        return new File(MULE_HOME);
    }
    
    public static File getMuleLibDir()
    {   
        return new File(MULE_HOME + File.separator + MULE_LIB_FILENAME);
    }
    
    public static File getMuleModuleDir()
    {   
        return new File(MULE_HOME + File.separator + MULE_MODULE_FILENAME);
    }
    
    public static File getMuleLocalJarFile()
    {
        return new File(getMuleLibDir(), MULE_LOCAL_JAR_FILENAME);
    }    
}
