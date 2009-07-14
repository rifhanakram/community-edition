/**
 * Copyright (C) 2005-2009 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing
 */
 
/**
 * Folder actions component - DOD5015 extensions.
 * 
 * @namespace Alfresco
 * @class Alfresco.RecordsFolderActions
 */
(function()
{
   /**
    * YUI Library aliases
    */
   var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event,
      Element = YAHOO.util.Element;
   
   /**
    * Alfresco Slingshot aliases
    */
   var $html = Alfresco.util.encodeHTML;
   
   /**
    * RecordsFolderActions constructor.
    * 
    * @param {String} htmlId The HTML id of the parent element
    * @return {Alfresco.RecordsFolderActions} The new RecordsFolderActions instance
    * @constructor
    */
   Alfresco.RecordsFolderActions = function(htmlId)
   {
      return Alfresco.RecordsFolderActions.superclass.constructor.call(this, htmlId);
   }
   
   YAHOO.extend(Alfresco.RecordsFolderActions, Alfresco.FolderActions,
   {
      /**
       * Event handler called when the "folderDetailsAvailable" event is received
       */
      onFolderDetailsAvailable: function RFA_onFolderDetailsAvailable(layer, args)
      {
         Alfresco.RecordsFolderActions.superclass.onFolderDetailsAvailable.apply(this, arguments);
         
         var me = this;

         // remember the data for the folder
         this.folderData = args[1];

         // update the href for the edit disposition schedule link
         var editDispositionScheduleUrl = Alfresco.constants.URL_PAGECONTEXT + "site/" + this.options.siteId +
            "/disposition-edit?nodeRef=" + this.folderData.nodeRef;
         try
         {
            Dom.get(this.id + "-edit-dispositionSchedule-action").href = editDispositionScheduleUrl;
         }
         catch (e)
         {
            // Edit disposition schedule action missing from config
         }

         // Hook action events
         var fnActionHandler = function RFA_oFDA_fnActionHandler(layer, args)
         {
            var owner = YAHOO.Bubbling.getOwnerByTagName(args[1].anchor, "div");
            if (owner !== null)
            {
               var action = owner.className;
               var target = args[1].target;
               if (typeof me[action] == "function")
               {
                  args[1].stop = true;
                  me[action].call(me, target.offsetParent, owner);
               }
            }
      		 
            return true;
         };
         
         // Ensure the "force" parameter is set to true to override the existing handler
         YAHOO.Bubbling.addDefaultAction("action-link", fnActionHandler, true);
      },


      /**
       * BUBBLING LIBRARY EVENT HANDLERS FOR ACTIONS
       * Disconnected event handlers for action event notification
       */

      /**
       * Close Record Folder action.
       *
       * @method onActionClose
       * @param obj {object} Event source
       */
      onActionClose: function RFA_onActionClose(obj)
      {
         this._dod5015Action(obj, "closeRecordFolder", "message.close");
      },

      /**
       * Re-open Record Folder action.
       *
       * @method onActionReopen
       * @param obj {object} Event source
       */
      onActionReopen: function RFA_onActionReopen(obj)
      {
         this._dod5015Action(obj, "openRecordFolder", "message.open");
      },

      /**
       * DOD5015 action.
       *
       * @method _dod5015Action
       * @param obj {object} Event source
       * @param actionName {string} Name of repository action to run
       * @param i18n {string} Will be appended with ".success" or ".failure" depending on action outcome
       * @private
       */
      _dod5015Action: function RFA__dod5015Action(obj, actionName, i18n)
      {
         var record = this.folderData,
            displayName = record.displayName,
            nodeRef = record.nodeRef;

         this.modules.actions.genericAction(
         {
            success:
            {
               event:
               {
                  name: "metadataRefresh"
               },
               message: this._msg(i18n + ".success", displayName)
            },
            failure:
            {
               message: this._msg(i18n + ".failure", displayName)
            },
            webscript:
            {
               method: Alfresco.util.Ajax.POST,
               stem: Alfresco.constants.PROXY_URI + "api/rma/actions/",
               name: "ExecutionQueue"
            },
            config:
            {
               requestContentType: Alfresco.util.Ajax.JSON,
               dataObj:
               {
                  name: actionName,
                  nodeRef: nodeRef
               }
            }
         });
      }
   });
})();
