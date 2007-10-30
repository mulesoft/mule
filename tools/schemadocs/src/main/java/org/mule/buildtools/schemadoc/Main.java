/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.buildtools.schemadoc;

import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


public class Main
{

    public static final String BACKUP = ".bak";
    public static final String TAG = "tag";
    public static final String XSL_FILE = "rename-tag.xsl";

    protected final Log logger = LogFactory.getLog(getClass());

    public static void main(String[] args)
            throws IOException, TransformerException, ParserConfigurationException
    {
        if (null == args || args.length != 3)
        {
            throw new IllegalArgumentException("Needs 3 arguments: prefix, postfix and destination");
        }
        new Main(args[0], args[1], args[2]);
    }

    public Main(String prefix, String postfix, String normalizedPath)
            throws IOException, TransformerException, ParserConfigurationException
    {
        logger.info("Generating " + normalizedPath);
        logger.debug("prefix: " + prefix);
        logger.debug("postfix: " + postfix);
        File normalized = new File(normalizedPath);
        backup(normalized);
        InputStream xslSource = IOUtils.getResourceAsStream(XSL_FILE, getClass());
        if (null == xslSource)
        {
            throw new IllegalStateException("Cannot open " + XSL_FILE);
        }
        logger.debug("creating output file for " + normalized);
        normalized.createNewFile();
        OutputStream out = new FileOutputStream(normalized);
        logger.debug("out: " + out);
        OutputStreamWriter outWriter = new OutputStreamWriter(out);
        IOUtils.copy(IOUtils.getResourceAsStream(prefix, getClass()), outWriter);
        outWriter.flush();
        processSchema(xslSource, out);
        out.flush();
        IOUtils.copy(IOUtils.getResourceAsStream(postfix, getClass()), outWriter);
        outWriter.close();
    }

    protected void processSchema(InputStream xslSource, OutputStream out)
            throws TransformerException, IOException, ParserConfigurationException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        Templates template = factory.newTemplates(new StreamSource(xslSource));
        Transformer xformer = template.newTransformer();
        Iterator files = listSchema2().iterator();
        while (files.hasNext())
        {
            File file = (File) files.next();
            String tag = tagFromFileName(file);
            logger.debug(tag + " : " + file);
            xformer.setParameter(TAG, tag);
            Source source = new StreamSource(new FileInputStream(file));
            xformer.transform(source, new StreamResult(out));
//            xformer.transform(source, new StreamResult(System.out));
            out.flush();
        }
    }

    protected String tagFromFileName(File file)
    {
        String name = file.getName();
        int index = name.indexOf(".");
        name = name.substring(0, index);
        index = name.indexOf("-");
        if (index > -1)
        {
            return name.substring(index+1);
        }
        else
        {
            return "mule";
        }
    }

    // for some reason, this doesn't work
    protected List listSchema1() throws IOException
    {
        PathMatchingResourcePatternResolver resolver =
                new PathMatchingResourcePatternResolver(getClass().getClassLoader());
        Resource[] resources = resolver.getResources("**/META-INF/*.xsd");
        List list = new LinkedList();
        for (int i = 0; i < resources.length; ++i)
        {
            list.add(resources[i].getFile());
        }
        return list;
    }

    // this is a bit ad-hoc, but seems to work efficiently
    protected List listSchema2() throws IOException
    {
        ClassLoader loader = getClass().getClassLoader();
        List files = new LinkedList();
        Enumeration resources = loader.getResources("META-INF");
        FilenameFilter filter =
                new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".xsd");
                    }
                };
        while (resources.hasMoreElements())
        {
            URL url = (URL) resources.nextElement();
            logger.debug("url: " + url);
            if ("file".equals(url.getProtocol()))
            {
                File dir = new File(url.getFile());
                String[] names = dir.list(filter);
                for (int i = 0; i < names.length; ++i)
                {
                    String name = names[i];
                    logger.debug("file: " + name);
                    files.add(new File(dir, name));
                }
            }
        }
        return files;
    }

    protected void backup(File file) throws IOException
    {
        if (file.exists())
        {
            File backup = new File(file.getAbsoluteFile().getParent(), file.getName() + BACKUP);
            if (backup.exists())
            {
                logger.debug("deleting " + backup.getCanonicalPath());
                backup.delete();
            }
            logger.debug("renaming " + file.getCanonicalPath() + " to " + backup.getCanonicalPath());
            file.renameTo(backup);
        }
    }

}
