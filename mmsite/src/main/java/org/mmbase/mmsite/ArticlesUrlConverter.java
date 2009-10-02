package org.mmbase.mmsite;

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import org.mmbase.util.transformers.CharTransformer;
import org.mmbase.util.transformers.Identifier;
import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.framework.*;
import org.mmbase.framework.basic.DirectoryUrlConverter;
import org.mmbase.framework.basic.BasicFramework;
import org.mmbase.framework.basic.Url;
import org.mmbase.framework.basic.BasicUrl;
import org.mmbase.util.functions.*;
import org.mmbase.util.logging.*;
import org.mmbase.mmsite.LocaleUtil;

/**
 * UrlConverter that can filter and create urls for articles, f.e. '/articles/2345/article_title'
 * This UrlConverter works on the second last piece of the path, the nodenumber. The last part of 
 * the path, 'article_title' does nothing. When related with 'posrel' to a 'pages' node, its path 
 * gets prepended to the link f.e. when the article is published on a page named 'News' it
 * can become '/news/articles/2345/article_title'. This UrlConverter relies on 
 * {@link SiteUrlConverter} for multilanguage and the optional use of an extension.
 * <br />
 *  &lt;urlconverter class="org.mmbase.mmsite.ArticlesUrlConverter"&gt;
 *    &lt;description xml:lang="en"&gt;
 *      An UrlConverter the create nice looking link for articles in mmsite.
 *    &lt;/description&gt;
 *    &lt;param name="directory"&gt;/&lt;/param&gt;
 *    &lt;param name="useTitle"&gt;true&lt;/param&gt;
 *    &lt;param name="template"&gt;/article.jsp&lt;/param&gt;
 *  &lt;/urlconverter&gt;
 *
 * @author Andr&eacute; van Toly
 * @version $Id$
 * @since MMBase-1.9
 */
public class ArticlesUrlConverter extends DirectoryUrlConverter {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(ArticlesUrlConverter.class);

    private static CharTransformer trans = new Identifier();
    private boolean useTitle = false;
    private String template = "/article.jspx";

    private final LocaleUtil  localeUtil = new LocaleUtil();

    public ArticlesUrlConverter(BasicFramework fw) {
        super(fw);
        setDirectory("/articles/");
        addBlock(ComponentRepository.getInstance().getComponent("mmsite").getBlock("article"));
    }

    public void setUseTitle(boolean t) {
        useTitle = t;
    }
    
    public void setTemplate(String t) {
        template = t;
    }

    @Override public int getDefaultWeight() {
        int q = super.getDefaultWeight();
        return Math.max(q, q + 2001);
    }

    public static final Parameter<Node> ARTICLE = new Parameter<Node>("article", Node.class);

    @Override
    public Parameter[] getParameterDefinition() {
        return new Parameter[] {Parameter.REQUEST, Framework.COMPONENT, Framework.BLOCK, Parameter.CLOUD, ARTICLE, LocaleUtil.LOCALE};
    }

    @Override
    public boolean isFilteredMode(Parameters frameworkParameters) throws FrameworkException {
        HttpServletRequest request = org.mmbase.framework.basic.BasicUrlConverter.getUserRequest(frameworkParameters.get(Parameter.REQUEST));
        String path = FrameworkFilter.getPath(request);
        for (String e : SiteUrlConverter.excludedPaths) {
            if (path.startsWith("/" + e + "/")) {
                return false;
            }
        }
        return super.isFilteredMode(frameworkParameters);
    }
    
    /**
     * Generates a nice url for an 'articles'.
     */
    @Override protected void getNiceDirectoryUrl(StringBuilder b,
                                                 Block block,
                                                 Parameters parameters,
                                                 Parameters frameworkParameters,  boolean action) throws FrameworkException {
        if (log.isDebugEnabled()) {
            log.debug("" + parameters + frameworkParameters);
            log.debug("Found mmsite block: " + block);
        }
        int b_len = b.length();
        if (block.getName().equals("article")) {
            Node n = frameworkParameters.get(ARTICLE);
            if (n == null) throw new IllegalStateException("No articles parameter used in " + frameworkParameters);
            
            // check if related to pages
            Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            //Cloud cloud = n.getCloud();
            Query query = Queries.createRelatedNodesQuery(n, cloud.getNodeManager("pages"), "posrel", "source");
            NodeList nl = cloud.getList(query);
            if (nl.size() > 1) log.warn(nl.size() + " pages found related to articles #" + n.getNumber() + ", could only return first !");
            if (nl.size() > 0) {
                Node clusterNode = nl.getNode(0);
                Node pages = cloud.getNode(clusterNode.getIntValue("pages.number"));
                if (log.isDebugEnabled()) log.debug("Found pages: " + pages.getNumber());
                
                String path = pages.getStringValue("path");
                if (path.startsWith("/")) {
                    path = path.substring(1, path.length());
                }
                if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
                b.append(path);
            }

            b.append("/").append(n.getNumber());
            if (useTitle) {
                b.append("/").append(trans.transform(n.getStringValue("title")));
            }
            
            if (b.length() > b_len) {   // check if url is altered
                if (SiteUrlConverter.useExtension) {
                    b.append(".").append(SiteUrlConverter.extension);
                }
            }
            localeUtil.appendLanguage(b, frameworkParameters);

            if (log.isDebugEnabled()) {
                log.debug("b: " + b.toString());
            }
        }
    }


    /**
     * Translates the result of {@link #getNiceUrl} back to an actual JSP which can render the block
     */
    @Override
    public Url getFilteredInternalDirectoryUrl(List<String>  path, Map<String, ?> params, Parameters frameworkParameters) throws FrameworkException {
        if (log.isDebugEnabled()) log.debug("path pieces: " + path + ", path size: " + path.size());

        HttpServletRequest request = frameworkParameters.get(Parameter.REQUEST);

        StringBuilder result = new StringBuilder();
        if (path.size() == 0) {
            return Url.NOT; // handled by mmsite
        } else {
            result.append(template + "?n=");

            // last element can contain language and/or extension
            String last = path.get(path.size() - 1); 
            last = localeUtil.setLanguage(last, request);
            if (SiteUrlConverter.useExtension && last.indexOf(SiteUrlConverter.extension) > -1) {
                last = last.substring(0, last.lastIndexOf(SiteUrlConverter.extension));
            }
            path.set(path.size() - 1, last);    // put it back
            
            String nr;
            if (path.size() > 0) {
                if (useTitle && path.size() > 1) { // nodenumber is second last element
                    nr = path.get(path.size() - 2);
                } else {
                    nr = path.get(path.size() - 1);
                }
            } else {
                if (log.isDebugEnabled()) log.debug("path not > 0");
                return Url.NOT;
            }
            Cloud cloud = frameworkParameters.get(Parameter.CLOUD);
            if (log.isDebugEnabled()) log.debug("articles nr: " + nr);
            if (cloud.hasNode(nr)) {
                Node article = cloud.getNode(nr);

                boolean show = article.getBooleanValue("show");
                //Date today = new Date();
                //Date online = article.getDateValue("online");
                //Date offline = article.getDateValue("offline");
                String nmName = article.getNodeManager().getName();

                if (!nmName.equals("articles")) {
                    throw new FrameworkException("Not an articles");
                } else if (!show) {
                    throw new FrameworkException("Articles not shown: " + show);
                } else {
                    frameworkParameters.set(ARTICLE, article);
                    result.append(nr);
                }
            } else {
                // node not found
                return Url.NOT;
            }

        }

        if (log.isDebugEnabled()) log.debug("returning: " + result.toString());
        return new BasicUrl(this, result.toString());
    }

}
