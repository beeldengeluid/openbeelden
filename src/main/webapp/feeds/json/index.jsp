<%@ page contentType="application/json;charset=utf-8" 
%><%@ page session="false" 
%><%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"
%><%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><%@ taglib uri="http://www.mmbase.org/mmbase-taglib-2.0" prefix="mm"
%><%@ taglib uri="http://www.opensymphony.com/oscache" prefix="os"
%><%@ taglib tagdir="/WEB-INF/tags/oip" prefix="oip"
%><mm:content 
    type="application/json" expires="1800" postprocessor="reducespace" language="${param.locale}">
  <%-- version: '$Id$' --%>
  <jsp:directive.page session="false" />
  <jsp:directive.page import="org.mmbase.bridge.*" />
  <% response.setHeader("Access-Control-Allow-Origin", "*"); %>
  <mm:cloud>  

    <mm:import externid="type" />
    <mm:import externid="set">openimages</mm:import>
    <mm:import externid="lang">nl</mm:import>
    
    <mm:import externid="max">100</mm:import>
    <mm:import externid="offset">0</mm:import>
    
    
    <mm:escaper type="tagstripper" id="tagstripper" />
    
    <c:choose>
      <c:when test="${!empty set}">
        
        <mm:listnodescontainer type="properties">
          <mm:constraint field="key" value="oai-set" operator="EQUAL" />
          <mm:constraint field="value" value="${set}" operator="EQUAL" />
          <mm:maxnumber value="1" />
          <mm:listnodes>
            <mm:field name="parent" id="portal" write="false" />
          </mm:listnodes>
        </mm:listnodescontainer>

        <mm:import id="analyzer" reset="true">org.apache.lucene.analysis.KeywordAnalyzer</mm:import>
        <mm:import id="value">media* <oip:portalquery portal="${set eq 'openimages' ? 'pool_oip' : portal}" /></mm:import>
        <mm:import id="fields" reset="true">indexId</mm:import>

      </c:when>
      <c:otherwise>
        <!-- default portal, display latest -->
        <mm:import id="value">media*</mm:import>
        <mm:import id="fields">indexId</mm:import>
      </c:otherwise>
    </c:choose>

    <mm:import id="index">media</mm:import>
    
    
    <c:choose>
      <c:when test="${type eq 'video'}"><mm:import id="extraconstraints"> otype:EQ:<mm:nodeinfo type="number" nodetype="videofragments" /></mm:import></c:when>
      <c:when test="${type eq 'audio'}"><mm:import id="extraconstraints"> otype:EQ:<mm:nodeinfo type="number" nodetype="audiofragments" /></mm:import></c:when>
      <c:when test="${type eq 'image'}"><mm:import id="extraconstraints"> otype:EQ:<mm:nodeinfo type="number" nodetype="imagefragments" /></mm:import></c:when>
      <c:otherwise><mm:import id="extraconstraints"></mm:import></c:otherwise>
    </c:choose>
    <mm:import id="sortfields">REVERSE:lastmodified</mm:import>
    <mm:import id="filter">
      <c:if test="${!empty start}">lastmodified:GTE:<mm:time format="yyyyMMddHHmm" time="$start" />00</c:if>
      <c:if test="${!empty end}">${!empty start ? '+ ' : ''}lastmodified:LTE:<mm:time format="yyyyMMddHHmm" time="$end" />00</c:if>
    </mm:import>

    <mm:nodelistfunction 
        module="lucene"
        id="list"
        name="search"
        referids="index,value,offset,max,fields?,sortfields?,filter?,analyzer?,extraconstraints" />

    <mm:function
        module="lucene"
        id="total"
        name="searchsize"
        write="false"
        referids="index,value,fields?,filter?,analyzer?,extraconstraints" />

<c:set var="cachekey">oai-json.t${type}.s${set}.l${lang}.m${max}.o${offset}</c:set>
<os:cache key="${cachekey}" time="10">
{
  "repository" : "Open Image Platform",
  "admin-email" : "info@openimage.eu",
  "base-url" : "<mm:url page="/" absolute="true" />",
  "size" : ${total}, 
  "media" : [
    <mm:listnodes referid="list">
      <mm:nodeinfo type="type" id="ntype" write="false" />
      {
        "number" : "${_node.number}",
        "identifier" : "oai:openimages.eu:${_node.number}",
        "title" : "<mm:field name="title" escape="js-double-quotes" />",
        "language" : "${_node.language}",
        "subtitle" : "${_node.subtitle}",
        "keywords" : [<mm:field name="keywords" escape="none"><mm:isnotempty>
            <c:forEach items="${fn:split(_, ';')}" var="subject" varStatus="st">
              "<c:out value="${fn:trim(subject)}" />"<c:if test="${st.last ne true}">,</c:if>
            </c:forEach>
        </mm:isnotempty></mm:field>],
        "intro" : "<mm:field name="intro" escape="tagstripper,text/xml" />",
        "body" : "<mm:field name="body" escape="tagstripper,text/xml" />",
        "creator" : "${_node.creator}",
        "publisher" : "${_node.publisher}",
        "contributor" : [<mm:field name="contributor" escape="none"><mm:isnotempty>
          <c:forEach items="${fn:split(_, ';')}" var="key" varStatus="st">
            "<c:out value="${fn:trim(key)}" />"<c:if test="${st.last ne true}">,</c:if>
          </c:forEach>
        </mm:isnotempty></mm:field>],
        "date" : "<mm:field name="date"><mm:time format="yyyy-MM-dd" /></mm:field>",
        "type" : "<c:choose><c:when test="${fn:startsWith(ntype,'image')}">image</c:when><c:when test="${fn:startsWith(ntype,'audio')}">audio</c:when><c:otherwise>video</c:otherwise></c:choose>",
        "length" : "${_node.length}",
        "urls" : [
        <mm:functioncontainer>
          <mm:param name="format">WEBM,OGV,MP4,OGG,TS,M3U8,MP3,OGG,OGA,PNG,JPG,JPEG,MPG,MPEG</mm:param>
          <mm:listfunction name="filteredurls" varStatus="st">
          <%-- c:if test="${!empty _.URL and _.available}" --%>
            <mm:node number="${_.source.number}">
            
            <c:choose>
              <c:when test="${!empty _.codec and fn:indexOf(_.codec,'H264') gt -1}">
                <c:set var="codecs">avc1.42E01E,mp4a.40.2</c:set>
              </c:when>
              <c:when test="${!empty _.codec and _.codec ne 'UNKNOWN'}">
                <c:set var="codecs">${mm:escape('lowercase', _.codec)}</c:set>
                <c:if test="${!empty _.acodec and _.acodec ne 'UNKNOWN'}">
                  <c:set var="codecs">${codecs},${mm:escape('lowercase', _.acodec)}</c:set>
                </c:if>
              </c:when>
              <c:otherwise><c:set var="codecs" value="" /></c:otherwise>
            </c:choose>
            
            <c:choose>
              <c:when test="${fn:startsWith(ntype,'image')}">
                {
                  "mimetype" : "${_.mimeType}",
                  "format" : "${_.state eq 'SOURCE' ? 'source' : 'image'}",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:when>
              <c:when test="${!empty _node.label}">
                {
                  "mimetype" : "${_.mimeType}",
                  "codecs" : "${codecs}",
                  "format" : "${_node.label}",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:when>
              <c:otherwise>
                {
                  "mimetype" : "${_.mimeType}",
                  "codecs" : "${codecs}",
                  "format" : "unknown",
                  "url" : "<mm:url page="${_.URL}" absolute="true" />"
                }<c:if test="${st.last ne true}">,</c:if>
              </c:otherwise>
            </c:choose>
            </mm:node>
          <%-- /c:if --%>
          </mm:listfunction>
        </mm:functioncontainer>
        ],
        <c:if test="${fn:startsWith(ntype, 'video')}">
          <mm:hasfunction name="thumbnail"><mm:nodefunction name="thumbnail">
            "thumb" : "<mm:image width="512" height="288" crop="middle" absolute="true" />",
          </mm:nodefunction></mm:hasfunction>
        </c:if>
        <c:if test="${fn:startsWith(ntype, 'image')}">
          <mm:relatednodes type="imagesources" max="1">
            "thumb" : "<mm:image width="512" height="288" crop="middle" absolute="true" />",
          </mm:relatednodes>
        </c:if>
        <c:if test="${fn:startsWith(ntype, 'audio')}">
          <mm:relatednodescontainer type="images" role="related" searchdirs="destination">
            <mm:maxnumber value="1" />
            <mm:relatednodes>
              "thumb" : "<mm:image width="512" height="288" crop="middle" absolute="true" />",
            </mm:relatednodes>
          </mm:relatednodescontainer>
        </c:if>
        "coverage" : [<mm:field name="coverage" escape="none"><mm:isnotempty>
          <c:forEach items="${fn:split(_, ';')}" var="coverage" varStatus="st">
            "<c:out value="${fn:trim(coverage)}" />"<c:if test="${st.last ne true}">,</c:if>
          </c:forEach>
        </mm:isnotempty></mm:field>]
      
      }<mm:last inverse="true">,</mm:last>
    </mm:listnodes>
  ]
}
</os:cache>
  </mm:cloud>
</mm:content>