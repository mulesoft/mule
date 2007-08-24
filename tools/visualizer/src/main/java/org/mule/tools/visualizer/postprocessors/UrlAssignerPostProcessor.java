/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tools.visualizer.postprocessors;

import org.mule.tools.visualizer.components.PostProcessor;
import org.mule.tools.visualizer.config.GraphConfig;
import org.mule.tools.visualizer.config.GraphEnvironment;
import org.mule.util.StringUtils;

import com.oy.shared.lm.graph.Graph;
import com.oy.shared.lm.graph.GraphNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.springframework.util.AntPathMatcher;

public class UrlAssignerPostProcessor implements PostProcessor
{

    private static Logger log = Logger.getLogger(UrlAssignerPostProcessor.class);

    private class Pattern implements Comparable
    {
        private String pattern;

        private String url;

        private AntPathMatcher matcher = new AntPathMatcher();

        Pattern(String pattern, String url)
        {
            this.pattern = pattern;
            this.url = url;
        }

        public boolean match(String className)
        {
            return matcher.match(this.pattern, className);
        }

        public boolean equals(Object obj)
        {
            if (!(obj instanceof Pattern))
            {
                return false;
            }
            if (this == obj)
            {
                return true;
            }
            Pattern rhs = (Pattern) obj;
            return new EqualsBuilder().appendSuper(super.equals(obj)).append(pattern, rhs.pattern).isEquals();
        }
        
        public int hashCode()
        {
            return (null == pattern ? 0 : pattern.hashCode()) ^ (null == url ? 0 : url.hashCode());
        }

        public String toString()
        {
            return ToStringBuilder.reflectionToString(this) + "\n";
        }

        public int compareTo(Object arg)
        {
            Pattern rhs = (Pattern) arg;
            return -this.pattern.compareTo(rhs.pattern);

        }
    }

    private List packagePatterns = null;

    public void postProcess(Graph graph, GraphEnvironment env)
    {

        initPatterns(env.getConfig());

        GraphNode[] nodes = graph.getNodes();
        for (int i = 0; i < nodes.length; i++)
        {
            GraphNode node = nodes[i];
            String caption = node.getInfo().getCaption();
            String header = node.getInfo().getHeader();
            String className = "";

            className = extractFullClassName(caption, header);
            String url = getUrlPatternForClass(className);
            String classNameUrl = className.replaceAll("\\.", "/");

            url = StringUtils.replace(url, "${classname}", classNameUrl);
            url = StringUtils.replace(url, "${header}", header);

            node.getInfo().setAttributes(node.getInfo().getAttributes() + "\n URL=\"" + url + "\" ");
        }
    }

    private void initPatterns(GraphConfig config)
    {

        if (packagePatterns == null)
        {
            packagePatterns = new ArrayList();
            for (Iterator iter = config.getUrls().keySet().iterator(); iter.hasNext();)
            {
                String pattern = (String) iter.next();
                String url = config.getUrls().getProperty(pattern);
                packagePatterns.add(new Pattern(pattern, url));
            }
            Collections.sort(packagePatterns);
            log.info("patterns : " + packagePatterns);
        }

    }

    private String getUrlPatternForClass(String className)
    {

        for (Iterator iter = packagePatterns.iterator(); iter.hasNext();)
        {
            Pattern element = (Pattern) iter.next();
            if (element.match(className))
            {
                log.info(className + " match pattern " + element);
                return element.url;
            }
        }

        return "http://mule.codehaus.org/docs/apidocs/${classname}.html";
    }

    private String extractFullClassName(String caption, String header)
    {
        String className = "";
        className = getAttribute(caption, "className");
        if (className == null)
        {
            className = getAttribute(caption, "implementation");
        }
        if (className == null)
        {
            className = header;
        }
        if (className == null)
        {
            className = "";
        }

        return className;
    }

    private String getAttribute(String caption, String attrib)
    {
        String result = null;
        String toSearch = attrib + " :";
        int index = caption.indexOf(toSearch);
        if (index != -1)
        {
            String sub = caption.substring(index + toSearch.length());
            int indexEnd = sub.indexOf("\n");
            result = sub.substring(0, indexEnd);
        }
        return result;
    }

}
