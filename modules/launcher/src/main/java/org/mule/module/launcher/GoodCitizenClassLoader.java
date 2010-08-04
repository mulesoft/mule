package org.mule.module.launcher;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Collection;
import java.util.Vector;
import java.util.jar.JarFile;

import sun.net.www.protocol.jar.Handler;

/**
 * Fixes major classloader woes by:
 * <ol>
 *  <li>Providing a {@link #close()} method to release any connections to resources.</li>
 *  <li>Disabling caching of jar resources fix e.g. java.util.ResourceBundle 'tagging' the app
 *      and preventing it from being undeployed correctly (no leaving locked jars behind).</li>
 * </ol>
 */
public class GoodCitizenClassLoader extends URLClassLoader
{

    public GoodCitizenClassLoader(URL[] urls, ClassLoader parent)
    {
        super(urls, parent, new NonCachingURLStreamHandlerFactory());
    }

    /**
     * A workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5041014
     */
    public void close()
    {
        // jars
        try
        {
            Class clazz = URLClassLoader.class;
            Field ucp = clazz.getDeclaredField("ucp");
            ucp.setAccessible(true);
            Object urlClassPath = ucp.get(this);
            Field loaders = urlClassPath.getClass().getDeclaredField("loaders");
            loaders.setAccessible(true);
            Collection jarLoaders = (Collection) loaders.get(urlClassPath);
            for (Object jarLoader : jarLoaders)
            {
                try
                {
                    Field loader = jarLoader.getClass().getDeclaredField("jar");
                    loader.setAccessible(true);
                    Object jarFile = loader.get(jarLoader);
                    ((JarFile) jarFile).close();
                }
                catch (Throwable t)
                {
                    // if we got this far, this is probably not a JAR loader so skip it
                }
            }
        }
        catch (Throwable t)
        {
            // probably not a SUN VM
        }

        try
        {
            // now native libs
            Class clazz = ClassLoader.class;
            Field nativeLibraries = clazz.getDeclaredField("nativeLibraries");
            nativeLibraries.setAccessible(true);
            Vector nativelib = (Vector) nativeLibraries.get(this);
            for (Object lib : nativelib)
            {
                Method finalize = lib.getClass().getDeclaredMethod("finalize");
                finalize.setAccessible(true);
                finalize.invoke(lib);
            }
        }
        catch (Exception ex)
        {
            // ignore
        }

    }

    protected static class NonCachingURLStreamHandlerFactory implements URLStreamHandlerFactory
    {
        public URLStreamHandler createURLStreamHandler(String protocol)
        {
            return new NonCachingJarResourceURLStreamHandler();
        }
    }

    /**
     * Prevents jar caching for this classloader, mainly to fix the static ResourceBundle mess/cache
     * that keeps connections open no matter what.
     */
    private static class NonCachingJarResourceURLStreamHandler extends Handler
    {

        @Override
        protected java.net.URLConnection openConnection(URL u) throws IOException
        {
            JarURLConnection c = new sun.net.www.protocol.jar.JarURLConnection(u, this);
            c.setUseCaches(false);
            return c;
        }
    }
}
