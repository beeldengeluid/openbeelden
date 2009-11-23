/*

This file is part of the Open Images Platform, a webapplication to manage and publish open media.
    Copyright (C) 2009 Netherlands Institute for Sound and Vision

The Open Images Platform is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Open Images Platform is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with The Open Images Platform.  If not, see <http://www.gnu.org/licenses/>.

*/

package eu.openimages;

import java.util.*;

import org.mmbase.bridge.*;
import org.mmbase.bridge.util.Queries;
import org.mmbase.util.functions.NodeFunction;
import org.mmbase.util.functions.Parameter;
import org.mmbase.util.functions.Parameters;
import org.mmbase.util.LocalizedString;
import org.mmbase.bridge.util.NodeMap;
import org.mmbase.bridge.util.MapNode;
import org.mmbase.util.logging.Logger;
import org.mmbase.util.logging.Logging;


/**
 * Finds a nodes translated node. For example the translation of a node of type 'articles' has a
 * translation in a node of type 'articles_translations' to which it is related via a 'langrel'.
 * The nodemanager to hold the translations can be specified with the property 'translations.builder',
 * otherwise a nodemanager will be guessed by appending '_translations'.
 * Only the translatable fields are part of 'articles_translations', fields like dates etc. are
 * ommited. The same untranslated node is returned when no translation is found. 
 * 
 * @author Andr&eacute; van Toly
 * @version $Id$
 */
public final class NodeTranslation extends NodeFunction<Node> {
    private static final long serialVersionUID = 0L;
    private static final Logger log = Logging.getLoggerInstance(NodeTranslation.class);
    
    public NodeTranslation() {
        super("nodetranslation", Parameter.LOCALE);
    }

    
    @Override
    public Node getFunctionValue(Node node, Parameters parameters) {
        Node translation = null;
        Cloud cloud = node.getCloud();
        NodeManager nm = node.getNodeManager();
        String translations_builder = nm.getProperty("translations.builder");
        if (translations_builder == null) {
            translations_builder = nm.getName() + "_translations";
        }
        NodeManager translationsNM = cloud.getNodeManager(translations_builder);
        
        Locale loc = parameters.get(Parameter.LOCALE);
        Locale oriloc = loc;
        String lang = loc.toString();
        if (log.isDebugEnabled()) log.debug("Trying to find a translated node in language: " + lang);
        
        try {
            while (loc != null && translation == null) {
                Query query = Queries.createRelatedNodesQuery(node, translationsNM, "langrel", "destination");
                Queries.addConstraint(query, Queries.createConstraint(query, "language", Queries.getOperator("EQUAL"), loc.toString(), null, true));
                if (log.isDebugEnabled()) log.debug("query: " + query.toSql());
                
                NodeList nl = cloud.getList(query);
                if (nl.size() > 1) log.warn(nl.size() + " translations found in '" + lang + "' for node " + node.getNumber() + " !");
                
                if (nl.size() > 0) {
                    Node clusterNode = nl.getNode(0); // clusternode
                    translation = cloud.getNode(clusterNode.getIntValue(translationsNM.getName() + ".number"));
                    
                    if (log.isDebugEnabled()) log.debug("Found: " + node.getNumber());
                } else {
                    loc = LocalizedString.degrade(loc, oriloc);
                }
            }
        
        } catch (Exception e) {
            log.error("Exception while building query: " + e);
        }
        
        Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(new NodeMap(node));
        if (translation != null) map.putAll(new NodeMap(translation));
        
        return new MapNode<Object>(map, cloud);
    }

}
