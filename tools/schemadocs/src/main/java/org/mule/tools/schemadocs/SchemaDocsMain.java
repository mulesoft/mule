/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tools.schemadocs;

import org.mule.util.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;

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


public class SchemaDocsMain
{

    public static final String BACKUP = ".bak";
    public static final String XSD = ".xsd";
    public static final String MULE = "mule";
    public static final String TAG = "tag";
    public static final String XSL_FILE = "rename-tag.xsl";
    public static final List TARGET_PATH = Arrays.asList(new String[]{"tools", "schemadocs", "target"});
    public static final String[] BLOCKED = new String[]{"wssecurity"};

    protected final Log logger = LogFactory.getLog(getClass());

    public static void main(String[] args) throws Exception
    {
        if (null == args || args.length != 3)
        {
            throw new IllegalArgumentException("Needs 3 arguments: prefix, postfix and destination");
        }

        try
        {
            new SchemaDocsMain(args[0], args[1], args[2]);
        }
        catch (Exception ex)
        {
            // using System.err on purpose here so that errors appear in the build
            // process and are not swallowed by the logging config
            ex.printStackTrace(System.err);

            throw ex;
        }
    }

    public SchemaDocsMain(String prefix, String postfix, String normalizedPath)
            throws IOException, TransformerException, ParserConfigurationException
    {
        logger.info("Generating " + normalizedPath);
        logger.debug("prefix: " + prefix);
        logger.debug("postfix: " + postfix);
        File normalized = inTargetDir(normalizedPath);
        backup(normalized);
        InputStream xslSource = IOUtils.getResourceAsStream(XSL_FILE, getClass());
        if (null == xslSource)
        {
            throw new IllegalStateException("Cannot open " + XSL_FILE);
        }
        create(normalized, false);
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

    protected void create(File file, boolean dir) throws IOException
    {
        if (!file.getParentFile().exists())
        {
            create(file.getParentFile(), true);
        }
        logger.debug("creating " + file);
        if (dir)
        {
            file.mkdir();
        }
        else
        {
            file.createNewFile();
        }
    }

    // if possible, and path not absolute, place in target directory
    protected File inTargetDir(String path)
    {
        if (path.startsWith(File.separator))
        {
            return new File(path);
        }
        else
        {
            File dir = new File(".");
            Iterator dirs = TARGET_PATH.iterator();
            boolean foundPath = false;
            while (dirs.hasNext())
            {
                File next = new File(dir, (String) dirs.next());
                if (next.exists())
                {
                    foundPath = true;
                    dir = next;
                }
                else if (foundPath)
                {
                    // in this case we started down the path, but failed
                    // (this avoids us placing the file somewhere other than "target"
                    // to workaround, specify absolute path)
                    throw new IllegalArgumentException("Could not find " + next + " while placing in target directory");
                }
            }
            File target = new File(dir, path);
            logger.info("Target: " + target);
            return target;
        }
    }

    protected void processSchema(InputStream xslSource, OutputStream out)
            throws TransformerException, IOException, ParserConfigurationException
    {
        TransformerFactory factory = TransformerFactory.newInstance();
        Templates template = factory.newTemplates(new StreamSource(xslSource));
        Transformer xformer = template.newTransformer();
        Iterator urls = listSchema2().iterator();
        while (urls.hasNext())
        {
            URL url = (URL) urls.next();
            String tag = tagFromFileName(url.getFile());
            logger.info(tag + " : " + url);
            xformer.setParameter(TAG, tag);
            Source source = new StreamSource(url.openStream());
            xformer.transform(source, new StreamResult(out));
//            xformer.transform(source, new StreamResult(System.out));
            out.flush();
        }
    }

    // this avoids using File objects since we may be dealing with classpath jars etc etc
    protected static String tagFromFileName(String name)
    {
        String dropExtension = toLeftOf(name, ".", name);
        String dropSlash = toRightOf(dropExtension, "/", dropExtension, true);
        String dropBackslash = toRightOf(dropSlash, "\\", dropSlash, true);
        return toRightOf(dropBackslash, "-", "mule", false);
    }

    protected static String toRightOf(String text, String delim, String deflt, boolean far)
    {
        int index = far ? text.lastIndexOf(delim) : text.indexOf(delim);
        if (index > -1)
        {
            return text.substring(index+1);
        }
        else
        {
            return deflt;
        }
    }

    protected static String toLeftOf(String text, String delim, String deflt)
    {
        int index = text.lastIndexOf(delim);
        if (index > -1)
        {
            return text.substring(0, index);
        }
        else
        {
            return deflt;
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
            list.add(resources[i].getURL());
        }
        return list;
    }

    // this is a bit ad-hoc, but seems to work efficiently
    // http://forum.java.sun.com/thread.jspa?threadID=286026&messageID=1119510
    protected List listSchema2() throws IOException
    {
        ClassLoader loader = getClass().getClassLoader();
        List files = new LinkedList();
        Enumeration resources = loader.getResources("META-INF");
        FilenameFilter filter =
                new FilenameFilter() {
                    public boolean accept(File dir, String name)
                    {
                        if (name.startsWith(MULE) && name.endsWith(XSD))
                        {
                            for (int i = 0; i < BLOCKED.length; ++i)
                            {
                                if (name.indexOf(BLOCKED[i]) > -1)
                                {
                                    return false;
                                }
                            }
                            return true;
                        }
                        else
                        {
                            return false;
                        }
                    }
                };
        while (resources.hasMoreElements())
        {
            URL url = (URL) resources.nextElement();
            logger.debug("url: " + url);
            if (url.toString().startsWith("jar:"))
            {
                readFromJar(url, files);
            }
            else if ("file".equals(url.getProtocol()))
            {
                readFromDirectory(new File(url.getFile()), files, filter);
            }
        }
        return files;
    }

    // this is used from within idea
    protected void readFromDirectory(File dir, List files, FilenameFilter filter) throws MalformedURLException
    {
        String[] names = dir.list(filter);
        for (int i = 0; i < names.length; ++i)
        {
            String name = names[i];
            logger.debug("file: " + name);
            files.add(new File(dir, name).toURL());
        }
    }

    // this is used from within maven
    protected void readFromJar(URL jarUrl, List resources) throws IOException
    {
        JarURLConnection jarConnection = (JarURLConnection) jarUrl.openConnection();
        Enumeration entries = jarConnection.getJarFile().entries();
        while (entries.hasMoreElements())
        {
            JarEntry entry = (JarEntry) entries.nextElement();
            String name = new File(entry.getName()).getName();
            if (name.startsWith(MULE) && name.endsWith(XSD))
            {
                logger.debug("entry: " + entry);
                resources.add(new URL(jarUrl, entry.getName()));
            }
        }
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
