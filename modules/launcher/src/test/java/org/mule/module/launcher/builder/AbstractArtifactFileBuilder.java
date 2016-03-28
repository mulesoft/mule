/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.launcher.builder;

import static com.google.common.base.Preconditions.checkArgument;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import org.mule.util.ClassUtils;
import org.mule.util.FileUtils;
import org.mule.util.FilenameUtils;
import org.mule.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Defines a builder to create files for mule artifacts.
 * <p/>
 * Instances can be configured using the methods that follow the builder pattern until the artifact file
 * is accessed. After that point, builder methods will fail to update the builder state.
 *
 * @param <T> class of the implementation builder
 */
public abstract class AbstractArtifactFileBuilder<T extends AbstractArtifactFileBuilder<T>>  implements TestArtifactDescriptor
{

    private final String fileName;
    private final String id;
    private File artifactFile;
    protected List<ZipResource> resources = new LinkedList<>();
    protected boolean corrupted;

    /**
     * Creates a new builder
     *
     * @param id artifact identifier. Non empty.
     */
    public AbstractArtifactFileBuilder(String id)
    {
        checkArgument(!StringUtils.isEmpty(id), "ID cannot be empty");
        this.id = id;
        this.fileName = id + ".zip";
    }

    /**
     * Creates a new builder from another instance.
     *
     * @param source instance used as template to build the new one. Non null.
     */
    public AbstractArtifactFileBuilder(T source)
    {
        this(source.getId(), source);
    }

    /**
     * Create a new builder from another instance and different ID.
     *
     * @param id artifact identifier. Non empty.
     * @param source instance used as template to build the new one. Non null.
     */
    public AbstractArtifactFileBuilder(String id, T source)
    {
        this(id);
        this.resources.addAll(source.resources);
        this.corrupted = source.corrupted;
    }

    /**
     * Adds a jar file to the artifact lib folder.
     *
     * @param jarFile jar file from a external file or test resource.
     * @return the same builder instance
     */
    public T usingLibrary(String jarFile)
    {
        checkImmutable();
        checkArgument(!StringUtils.isEmpty(jarFile), "Jar file cannot be empty");
        resources.add(new ZipResource(jarFile, "lib/" + FilenameUtils.getName(jarFile)));

        return getThis();
    }

    /**
     * Indicates that the generated artifact file must be a corrupted ZIP.
     *
     * @return the same builder instance
     */
    public T corrupted()
    {
        checkImmutable();
        this.corrupted = true;

        return getThis();
    }

    /**
     * @return current instance. Used just to avoid compilation warnings.
     */
    protected abstract T getThis();

    @Override
    public String getId()
    {
        return id;
    }

    @Override
    public String getZipPath()
    {
        return "/"+ fileName;
    }

    @Override
    public String getDeployedPath()
    {
        if (corrupted)
        {
            return fileName;
        }
        else
        {
            return id;
        }
    }

    @Override
    public File getArtifactFile() throws Exception
    {
        if (artifactFile == null)
        {
            checkArgument(!StringUtils.isEmpty(fileName), "Filename cannot be empty");

            final File tempFile = new File(getTempFolder(), fileName);
            tempFile.deleteOnExit();

            if (corrupted)
            {
                buildBrokenZipFile(tempFile);
            }
            else
            {
                buildZipFile(tempFile);
            }

            artifactFile = new File(tempFile.getAbsolutePath());
        }

        return artifactFile;
    }

    protected final void checkImmutable()
    {
        assertThat("Cannot change attributes once the artifact file was built", artifactFile, is(nullValue()));
    }

    private void buildZipFile(File tempFile) throws Exception
    {
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(tempFile));

        try
        {
            for (ZipResource zipResource : resources)
            {
                addZipResource(out, zipResource);
            }

            addCustomFileContent(out);
        }
        finally
        {
            out.close();
        }
    }

    protected void createPropertiesFile(ZipOutputStream out, Properties props, String propertiesFileName) throws IOException
    {
        if (!props.isEmpty())
        {
            final File applicationPropertiesFile = new File(getTempFolder(), propertiesFileName);
            applicationPropertiesFile.deleteOnExit();
            createPropertiesFile(applicationPropertiesFile, props);

            addZipResource(out, new ZipResource(applicationPropertiesFile.getAbsolutePath(), propertiesFileName));
        }
    }

    protected void createPropertiesFile(File file, Properties props)
    {
        try
        {
            OutputStream out = new FileOutputStream(file);
            props.store(out, "Generated application properties");
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Cannot create properties", e);
        }
    }

    protected void addCustomFileContent(ZipOutputStream out) throws Exception
    {
    }

    protected void addZipResource(ZipOutputStream out, ZipResource zipResource) throws IOException
    {
        URL resourceUrl = ClassUtils.getResource(zipResource.file, ApplicationFileBuilder.class);
        if (resourceUrl == null)
        {
            resourceUrl = new File(zipResource.file).toURI().toURL();
        }
        FileInputStream in = new FileInputStream(resourceUrl.getFile());
        try
        {
            // name the file inside the zip  file
            out.putNextEntry(new ZipEntry(zipResource.alias == null ? zipResource.file : zipResource.alias));

            // buffer size
            byte[] b = new byte[1024];
            int count;

            while ((count = in.read(b)) > 0)
            {
                System.out.println();
                out.write(b, 0, count);
            }
        }
        finally
        {
            in.close();
        }
    }

    protected String getTempFolder()
    {
        return System.getProperty("java.io.tmpdir");
    }

    private void buildBrokenZipFile(File tempFile) throws IOException
    {
        FileUtils.write(tempFile, "This is content represents invalid compressed data");
    }

    protected static final class ZipResource
    {

        final String file;
        final String alias;

        protected ZipResource(String file, String alias)
        {
            this.file = file;
            this.alias = alias;
        }
    }
}
