<#assign helpPages = config.scoped["HelpPages"]["help-pages"]>
<#assign helpLink = helpPages.getChildValue("share-help")!"">
<#assign siteActive><#if page.url.templateArgs.site??>true<#else>false</#if></#assign>
<#assign isGuest = (user.name=='guest') />
<#if !isGuest>
<script type="text/javascript">//<![CDATA[
   var thisHeader = new Alfresco.Header("${args.htmlid}").setOptions(
   {
      siteId: "${page.url.templateArgs.site!""}",
      searchType: "${page.url.templateArgs.site!'all'}", // default search type
      favouriteSites: {<#list favouriteSites as site>'${site.shortName}': '${site.title?js_string}'<#if site_has_next>,</#if></#list>}
   }).setMessages(
      ${messages}
   );
//]]></script>
</#if>
<div class="header">
   <div class="logo-wrapper">
      <div class="logo">
         <img src="${url.context}/themes/${theme}/images/app-logo.png" alt="Alfresco Share" />
      </div>
   </div>

   <div class="menu-wrapper">
      <#if !isGuest>
      <div class="personal-menu">   
         <span class="menu-item-icon my-dashboard"><a href="${url.context}/page/user/${user.name?url}/dashboard">${msg("link.myDashboard")}</a></span>
         <span class="menu-item-icon my-profile"><a href="${url.context}/page/user/${user.name?url}/profile">${msg("link.myProfile")}</a></span>
         <span id="${args.htmlid}-sites-linkMenuButton" class="link-menu-button">
            <span class="menu-item-icon sites link-menu-button-link"><a href="${url.context}/page/site-finder">${msg("link.sites")}</a></span>
            <input id="${args.htmlid}-sites" type="button"/>
         </span>
         <span class="menu-item-icon people"><a href="${url.context}/page/people-finder">${msg("link.people")}</a></span>
      </div>
      </#if>

      <div class="util-menu" id="${args.htmlid}-searchcontainer">
         <#if user.isAdmin>
         <span class="menu-item"><a href="${url.context}/page/console/admin-console/">${msg("link.console")}</a></span>
         <span class="menu-item-separator">&nbsp;</span>
         </#if>
         <span class="menu-item"><a href="${helpLink}" rel="_blank">${msg("link.help")}</a></span>
         <#if !isGuest>
         <span class="menu-item-separator">&nbsp;</span>
         <#if !context.externalAuthentication>
         <span class="menu-item"><a href="${url.context}/logout" title="${msg("link.logout.tooltip", user.name?html)}">${msg("link.logout")}</a></span>
         <span class="menu-item-separator">&nbsp;</span>
         </#if>
         <span class="menu-item">
            <span class="search-container">
               <label for="${args.htmlid}-searchtext" style="display:none">${msg("header.search.inputlabel")}</label>
               <input type="text" class="search-tinput" name="${args.htmlid}-searchtext" id="${args.htmlid}-searchtext" value="" maxlength="256" />
               <span id="${args.htmlid}-search-tbutton" class="search-site-icon"><a href="#">&nbsp;&nbsp;</a></span>
            </span>
         </span>
         </#if>
      </div>
   </div>

   <div id="${args.htmlid}-sites-menu" class="yui-overlay">
      <div class="bd">
         <#assign favDisplay><#if favouriteSites?size &gt; 0>block<#else>none</#if></#assign>
         <ul id="${args.htmlid}-favouritesContainer" class="favourite-sites" style="display: ${favDisplay}">
            <li class="header">
               ${msg("header.site.favouriteSites")}
            </li>
         </ul>
         <ul id="${args.htmlid}-favouriteSites" class="favourite-sites-list separator" style="display: ${favDisplay}">
         <#list favouriteSites as site>
            <li>
               <a href="${url.context}/page/site/${site.shortName}/dashboard">${site.title?html}</a>
            </li>
         </#list>
         </ul>
         <#if (page.url.templateArgs.site?? && !currentSiteIsFav)>
         <ul class="separator">
            <li>
               <a href="#" onclick="thisHeader.addAsFav('${page.url.templateArgs.site}'); return false;">${msg("link.add-fav",page.url.templateArgs.site!"")}</a>
            </li>
         </ul>
         </#if>
         <ul class="<#if !isGuest>separator</#if>">
            <li>
               <a href="${url.context}/page/site-finder">${msg("header.sites.findSites")}</a>
            </li>
         </ul>
         <#if !isGuest>
         <ul>
            <li>
               <a href="#" onclick="thisHeader.showCreateSite(); return false;">${msg("header.sites.createSite")}</a>
            </li>
         </ul>
         </#if>
      </div>
   </div>

   <#if !isGuest>
   <div id="${args.htmlid}-searchtogglemenu" class="hidden">
      <div class="bd">
         <ul class="last">
            <li>
               <a href="#" <#if siteActive == 'false'>class="disabled"<#else>onclick="thisHeader.doToggleSearchType('site'); return false;"</#if>>${msg("header.search.searchsite", page.url.templateArgs.site!"")}</a>
            </li>
            <li>
               <a href="#" onclick="thisHeader.doToggleSearchType('all'); return false;">${msg("header.search.searchall")}</a>
            </li>
         </ul>            
      </div>
   </div>	
   </#if>
</div>
<script type="text/javascript">//<![CDATA[
(function()
{
   Alfresco.util.relToTarget("${args.htmlid}");
})();
//]]></script>