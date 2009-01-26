/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 
package org.mule.osgi;

import aQute.lib.osgi.Analyzer;
import aQute.lib.osgi.Builder;
import aQute.lib.osgi.Jar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Manifest;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.osgi.Maven2OsgiConverter;
import org.codehaus.plexus.util.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;

/**
 * Goal which touches a timestamp file.
 * 
 * @goal repackage
 * @phase package
 */
public class BndMojo extends AbstractMojo
{
    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Artifact resolver, needed to download source jars for inclusion in classpath.
     * 
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     * @required
     * @readonly
     */
    protected ArtifactResolver resolver;
    /**
     * Artifact factory, needed to download source jars for inclusion in classpath.
     * 
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     * @required
     * @readonly
     */
    protected ArtifactFactory artifactFactory;

    /**
     * List of Remote Repositories used by the resolver
     * 
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    protected java.util.List remoteRepos;

    /**
     * Location of the local repository.
     * 
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    protected org.apache.maven.artifact.repository.ArtifactRepository local;

    /** @component */
    private ArtifactMetadataSource source;

    /**
     * @component
     */
    private Maven2OsgiConverter m_maven2OsgiConverter;

    /**
     * @parameter
     * @required
     */
    private List<Archive> archives;

    /**
     * @parameter
     */
    private String versionAppend = "osgi";

    /**
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}"
     * @required
     */
    private String buildDirectoryPath;

    /**
     * The BND instructions for the bundle.
     * 
     * @parameter
     */
    private Map<String, String> instructions = new HashMap<String, String>();

    /**
     * The directory for the generated bundles.
     * 
     * @parameter expression="${project.build.outputDirectory}"
     * @required
     */
    private File outputDirectory;

    /**
     * The directory for the pom
     * 
     * @parameter expression="${basedir}"
     * @required
     */
    private File baseDir;

    public void execute() throws MojoExecutionException
    {
        File buildDir = new File(buildDirectoryPath);

        for (Archive a : archives)
        {
            try
            {
                Artifact artifact = artifactFactory.createArtifactWithClassifier(a.getGroupId(),
                    a.getArtifactId(), a.getVersion(), "jar", a.getClassifier());

                HashSet<Artifact> dependencies = new HashSet<Artifact>();
                resolver.resolveTransitively(dependencies, artifact, remoteRepos, local, source);
                resolver.resolve(artifact, remoteRepos, local);

                // Copy the artifact and ensure there is no old version
                File artifactCopy = new File(buildDir, artifact.getFile().getName());
                artifactCopy.delete();
                FileUtils.copyFile(artifact.getFile(), artifactCopy);

                // Add OSGi properties
                File bundle = rebundle(artifactCopy, artifact, a, dependencies);

                // Attach the OSGi-ified artifact
                attach(a, bundle, "jar", a.getClassifier());

            }
            catch (ArtifactResolutionException e)
            {
                throw new MojoExecutionException("Could not resolve archive.", e);
            }
            catch (ArtifactNotFoundException e)
            {
                throw new MojoExecutionException("Could not find archive.", e);
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Could not create file.", e);
            }
            catch (Exception e)
            {
                e.printStackTrace();
                throw new MojoExecutionException("Could not rebundle.", e);
            }

            try
            {
                // Now copy the POM and change the version
                Artifact pom = artifactFactory.createArtifactWithClassifier(a.getGroupId(),
                    a.getArtifactId(), a.getVersion(), "pom", a.getClassifier());
                resolver.resolve(pom, remoteRepos, local);

                // Copy the artifact and ensure there is no old version
                File pomCopy = new File(buildDir, pom.getFile().getName());
                pomCopy.delete();
                FileUtils.copyFile(pom.getFile(), pomCopy);

                if (changeVersion(pomCopy, a.getVersion() + "-" + versionAppend))
                {
                    attach(a, pomCopy, "pom", a.getClassifier());
                }
                else
                {
                    getLog().error(
                        "Could not change artifact POM version as POM was invalid or did not exist for "
                                        + pom);
                }
            }
            catch (ArtifactResolutionException e)
            {
                getLog().error("Could not resolve POM for " + a + ". Skipping.");
            }
            catch (ArtifactNotFoundException e)
            {
                getLog().error("Could not resolve POM for " + a + ". Skipping.");
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Could not create file.", e);
            }
        }
    }

    private void attach(Archive a, File copy, String type, String classifier)
    {
        String version = a.getVersion() + "-" + versionAppend;

        Artifact artifact = artifactFactory.createArtifactWithClassifier(a.getGroupId(), a.getArtifactId(),
            version, type, classifier);
        artifact.setFile(copy);
        project.addAttachedArtifact(artifact);
    }

    private boolean changeVersion(File file, String newVersion) throws IOException
    {
        SAXReader reader = new SAXReader();

        try
        {
            Document doc = reader.read(file);

            Element version = doc.getRootElement().element("version");
            if (version == null)
            {
                version = doc.getRootElement().addElement("version");
            }

            version.setText(newVersion);

            XMLWriter writer = new XMLWriter();
            FileOutputStream out = new FileOutputStream(file);
            writer.setOutputStream(out);
            writer.write(doc);
            out.close();
            return true;
        }
        catch (DocumentException e)
        {
            return false;
        }
    }

    private File rebundle(File jarFile, Artifact artifact, Archive archive, HashSet<Artifact> dependencies)
        throws Exception
    {
        Properties properties = new Properties();
        properties.putAll(getDefaultProperties(project));
        properties.putAll(transformDirectives(instructions, archive));
        if (archive.getInstructions() != null)
        {
            properties.putAll(transformDirectives(archive.getInstructions(), archive));
        }

        header(properties, "Bundle-Name", artifact.getArtifactId());
        header(properties, Analyzer.BUNDLE_VERSION, archive.getVersion());

        File bundle = new File(jarFile.getAbsolutePath() + ".osgi");
        Builder builder = buildOSGiBundle(jarFile, bundle, dependencies, properties);

        List errors = builder.getErrors();
        List warnings = builder.getWarnings();

        for (Iterator w = warnings.iterator(); w.hasNext();)
        {
            String msg = (String) w.next();
            getLog().warn("Warning building bundle " + artifact + " : " + msg);
        }
        for (Iterator e = errors.iterator(); e.hasNext();)
        {
            String msg = (String) e.next();
            getLog().error("Error building bundle " + artifact + " : " + msg);
        }

        if (errors.size() > 0)
        {
            String failok = properties.getProperty("-failok");
            if (null == failok || "false".equalsIgnoreCase(failok))
            {
                jarFile.delete();

                throw new MojoFailureException("Error(s) found in bundle configuration");
            }
        }

        return bundle;
    }

    protected Builder buildOSGiBundle(File jarFile,
                                      File bundleFile,
                                      HashSet<Artifact> dependencies,
                                      Properties properties) throws Exception
    {
        Builder builder = new Builder();
        builder.setBase(new File(buildDirectoryPath));
        builder.setProperties(properties);

        // TODO: this doesn't seem to do much now
        List<Jar> cp = new ArrayList<Jar>();
        Jar jar = new Jar(jarFile);
        cp.add(jar);
        for (Artifact artifact : dependencies)
        {
            cp.add(new Jar(artifact.getFile()));
        }

        builder.setClasspath(cp.toArray(new Jar[0]));

        if (!properties.containsKey(Analyzer.EXPORT_PACKAGE)
            && !properties.containsKey(Analyzer.PRIVATE_PACKAGE))
        {
            if (properties.containsKey(Analyzer.EXPORT_CONTENTS))
            {
                /*
                 * if we have exportcontents but no export packages or private
                 * packages then we're probably embedding or inlining one or more
                 * jars, so set private package to a non-null (but empty) value to
                 * keep Bnd happy.
                 */
                properties.put(Analyzer.PRIVATE_PACKAGE, "!*");
            }
            else
            {
                String bsn = properties.getProperty(Analyzer.BUNDLE_SYMBOLICNAME);
                String namespace = bsn.replaceAll("\\W", ".");

                properties.put(Analyzer.EXPORT_PACKAGE, namespace + ".*");
            }
        }

        Jar built = builder.build();
        jar = builder.getJar();

        dumpManifest("BND Manifest:", jar.getManifest(), getLog());

        built.addAll(jar);
        built.write(bundleFile);

        builder.close();
        return builder;
    }

    protected static void dumpManifest(String title, Manifest manifest, Log log)
    {
        log.info(title);
        log.info("------------------------------------------------------------------------");
        for (Iterator i = manifest.getMainAttributes().entrySet().iterator(); i.hasNext();)
        {
            Map.Entry entry = (Map.Entry) i.next();
            log.info(entry.getKey() + ": " + entry.getValue());
        }
        log.info("------------------------------------------------------------------------");

    }

    protected Properties getDefaultProperties(MavenProject currentProject)
    {
        Properties properties = new Properties();

        properties.put(Analyzer.IMPORT_PACKAGE, "*");
        // remove the verbose Include-Resource entry from generated manifest
        properties.put(Analyzer.REMOVE_HEADERS, Analyzer.INCLUDE_RESOURCE);

        header(properties, Analyzer.BUNDLE_DESCRIPTION, currentProject.getDescription());
        StringBuffer licenseText = printLicenses(currentProject.getLicenses());
        if (licenseText != null)
        {
            header(properties, Analyzer.BUNDLE_LICENSE, licenseText);
        }
        if (currentProject.getOrganization() != null)
        {
            header(properties, Analyzer.BUNDLE_VENDOR, currentProject.getOrganization().getName());
            if (currentProject.getOrganization().getUrl() != null)
            {
                header(properties, Analyzer.BUNDLE_DOCURL, currentProject.getOrganization().getUrl());
            }
        }

        properties.putAll(currentProject.getProperties());
        properties.putAll(currentProject.getModel().getProperties());
        properties.putAll(getProperties(currentProject.getModel(), "project.build."));
        properties.putAll(getProperties(currentProject.getModel(), "pom."));
        properties.putAll(getProperties(currentProject.getModel(), "project."));
        properties.put("project.baseDir", baseDir);
        properties.put("project.build.directory", buildDirectoryPath);
        properties.put("project.build.outputdirectory", outputDirectory);

        properties.put("classifier", "");

        return properties;
    }

    private static StringBuffer printLicenses(List licenses)
    {
        if (licenses == null || licenses.size() == 0) return null;
        StringBuffer sb = new StringBuffer();
        String del = "";
        for (Iterator i = licenses.iterator(); i.hasNext();)
        {
            License l = (License) i.next();
            String url = l.getUrl();
            if (url == null) continue;
            sb.append(del);
            sb.append(url);
            del = ", ";
        }
        if (sb.length() == 0) return null;
        return sb;
    }

    private static Map getProperties(Model projectModel, String prefix)
    {
        Map properties = new HashMap();
        Method methods[] = Model.class.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++)
        {
            String name = methods[i].getName();
            if (name.startsWith("get"))
            {
                try
                {
                    Object v = methods[i].invoke(projectModel, null);
                    if (v != null)
                    {
                        name = prefix + Character.toLowerCase(name.charAt(3)) + name.substring(4);
                        if (v.getClass().isArray())
                            properties.put(name, Arrays.asList((Object[]) v).toString());
                        else
                            properties.put(name, v);

                    }
                }
                catch (Exception e)
                {
                    // too bad
                }
            }
        }
        return properties;
    }

    private static void header(Properties properties, String key, Object value)
    {
        if (value == null) return;

        if (value instanceof Collection && ((Collection) value).isEmpty()) return;

        properties.put(key, value.toString().replaceAll("[\r\n]", ""));
    }

    /*
     * transform directives from their XML form to the expected BND syntax (eg.
     * _include becomes -include)
     */
    protected static Map<String, String> transformDirectives(Map<String, String> originalInstructions,
                                                             Archive archive)
    {
        Map<String, String> transformedInstructions = new HashMap<String, String>();
        for (Map.Entry<String, String> e : originalInstructions.entrySet())
        {
            String key = (String) e.getKey();
            if (key.startsWith("_"))
            {
                key = "-" + key.substring(1);
            }

            String value = (String) e.getValue();
            if (null == value)
            {
                value = "";
            }
            else
            {
                value = value.replaceAll("[\r\n]", "");
                value = value.replaceAll("\\$\\{version\\}", archive.getVersion());
            }

            transformedInstructions.put(key, value);
        }
        return transformedInstructions;
    }

}
