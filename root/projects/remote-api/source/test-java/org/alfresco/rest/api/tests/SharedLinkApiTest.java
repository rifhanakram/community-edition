/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.rest.api.tests;

import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.rest.api.People;
import org.alfresco.rest.api.QuickShareLinks;
import org.alfresco.rest.api.impl.QuickShareLinksImpl;
import org.alfresco.rest.api.model.QuickShareLink;
import org.alfresco.rest.api.nodes.NodesEntityResource;
import org.alfresco.rest.api.quicksharelinks.QuickShareLinkEntityResource;
import org.alfresco.rest.api.tests.client.HttpResponse;
import org.alfresco.rest.api.tests.client.PublicApiClient.Paging;
import org.alfresco.rest.api.tests.client.data.Document;
import org.alfresco.rest.api.tests.client.data.Node;
import org.alfresco.rest.api.tests.client.data.Rendition;
import org.alfresco.rest.api.tests.util.RestApiUtil;
import org.alfresco.service.cmr.security.MutableAuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.alfresco.rest.api.tests.util.RestApiUtil.toJsonAsStringNonNull;
import static org.junit.Assert.*;

/**
 * API tests for:
 * <ul>
 * <li> {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links} </li>
 * <li> {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links/<sharedId>} </li>
 * </ul>
 *
 * @author janv
 */
public class SharedLinkApiTest extends AbstractBaseApiTest
{
    private static final String URL_SHARED_LINKS = "shared-links";

    private String user1;
    private String user2;
    private List<String> users = new ArrayList<>();

    protected MutableAuthenticationService authenticationService;
    protected PersonService personService;

    private final String RUNID = System.currentTimeMillis()+"";

    @Before
    public void setup() throws Exception
    {
        authenticationService = applicationContext.getBean("authenticationService", MutableAuthenticationService.class);
        personService = applicationContext.getBean("personService", PersonService.class);

        // note: createUser currently relies on repoService
        user1 = createUser("user1-" + RUNID);
        user2 = createUser("user2-" + RUNID);

        // We just need to clean the on-premise-users,
        // so the tests for the specific network would work.
        users.add(user1);
        users.add(user2);
    }

    @After
    public void tearDown() throws Exception
    {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
        for (final String user : users)
        {
            transactionHelper.doInTransaction(new RetryingTransactionCallback<Void>()
            {
                @Override
                public Void execute() throws Throwable
                {
                    if (personService.personExists(user))
                    {
                        authenticationService.deleteAuthentication(user);
                        personService.deletePerson(user);
                    }
                    return null;
                }
            });
        }
        users.clear();
        AuthenticationUtil.clearCurrentSecurityContext();
    }

    /**
     * Tests shared links to file (content)
     *
     * <p>POST:</p>
     * {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links}
     *
     * <p>GET:</p>
     * {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links/<sharedId>}
     * {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links/<sharedId>/content}
     *
     * <p>DELETE:</p>
     * {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links/<sharedId>}
     */
    @Test
    public void testSharedLinkCreateGetDelete() throws Exception
    {
        // As user 1 ...

        // create doc d1
        String sharedFolderNodeId = getSharedNodeId(user1);
        String content1Text = "The quick brown fox jumps over the lazy dog 1.";
        String docName1 = "content" + RUNID + "_1.txt";
        Document doc1 = createTextFile(user1, sharedFolderNodeId, docName1, content1Text);
        String d1Id = doc1.getId();

        // create doc d2
        String myFolderNodeId = getMyNodeId(user1);
        String content2Text = "The quick brown fox jumps over the lazy dog 1.";
        String docName2 = "content" + RUNID + "_2.txt";
        Document doc2 = createTextFile(user1, myFolderNodeId, docName2, content2Text);
        String d2Id = doc2.getId();

        // As user 2 ...

        HttpResponse response = getSingle(NodesEntityResource.class, user2, d1Id, null, 200);
        Node nodeResp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), Node.class);
        Date docModifiedAt = nodeResp.getModifiedAt();
        String docModifiedBy = nodeResp.getModifiedByUser().getId();
        assertEquals(user1, docModifiedBy);

        // create shared link
        Map<String, String> body = new HashMap<>();
        body.put("nodeId", d1Id);

        response = post(URL_SHARED_LINKS, user2, toJsonAsStringNonNull(body), 201);
        QuickShareLink resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);

        String sharedId = resp.getId();
        assertNotNull(sharedId);

        assertEquals(d1Id, resp.getNodeId());
        assertEquals(docName1, resp.getName());

        String expectedMimeType = MimetypeMap.MIMETYPE_TEXT_PLAIN;
        assertEquals(expectedMimeType, resp.getContent().getMimeType());
        assertEquals("Plain Text", resp.getContent().getMimeTypeName());

        assertEquals(new Long(content1Text.length()), resp.getContent().getSizeInBytes());
        assertEquals("UTF-8", resp.getContent().getEncoding());

        assertEquals(docModifiedAt.getTime(), resp.getModifiedAt().getTime()); // not changed
        assertEquals(docModifiedBy, resp.getModifiedByUser().getId()); // not changed (ie. not user2)
        assertEquals(docModifiedBy+" "+docModifiedBy, resp.getModifiedByUser().getDisplayName());

        assertEquals(user2, resp.getSharedByUser().getId());
        assertEquals(user2+" "+user2, resp.getSharedByUser().getDisplayName());

        // -ve test - try to create again (same user) - already exists
        post(URL_SHARED_LINKS, user2, toJsonAsStringNonNull(body), 409);


        // auth access to get shared link info - as user1
        Map<String, String> params = Collections.singletonMap("select", "allowableOperations");
        response = getSingle(QuickShareLinkEntityResource.class, user1, sharedId, params, 200);
        resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);

        assertEquals(sharedId, resp.getId());
        assertEquals(docName1, resp.getName());
        assertEquals(d1Id, resp.getNodeId());

        assertEquals(user1, resp.getModifiedByUser().getId()); // returned if authenticated
        assertEquals(user2, resp.getSharedByUser().getId()); // returned if authenticated

        assertNull(resp.getAllowableOperations());

        // auth access to get shared link info - as user2
        params = Collections.singletonMap("select", "allowableOperations");
        response = getSingle(QuickShareLinkEntityResource.class, user2, sharedId, params, 200);
        resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);

        assertEquals(sharedId, resp.getId());
        assertEquals(docName1, resp.getName());
        assertEquals(d1Id, resp.getNodeId());

        assertEquals(user1, resp.getModifiedByUser().getId()); // returned if authenticated
        assertEquals(user2, resp.getSharedByUser().getId()); // returned if authenticated

        assertEquals(1, resp.getAllowableOperations().size());
        assertEquals("delete", resp.getAllowableOperations().get(0));

        // allowable operations not selected
        response = getSingle(QuickShareLinkEntityResource.class, user2, sharedId, null, 200);
        resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);
        assertNull(resp.getAllowableOperations());


        // unauth access to get shared link info
        params = Collections.singletonMap("select", "allowableOperations"); // note: this will be ignore for unauth access
        response = getSingle(QuickShareLinkEntityResource.class, null, sharedId, params, 200);
        resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);

        assertEquals(sharedId, resp.getId());
        assertEquals(docName1, resp.getName());
        assertNull(resp.getNodeId()); // nodeId not returned
        assertNull(resp.getAllowableOperations()); // select is ignored

        assertNull(resp.getModifiedByUser().getId()); // userId not returned
        assertEquals(user1+" "+user1, resp.getModifiedByUser().getDisplayName());
        assertNull(resp.getSharedByUser().getId()); // userId not returned
        assertEquals(user2+" "+user2, resp.getSharedByUser().getDisplayName());


        // unauth access to get shared link file content
        response = getSingle(QuickShareLinkEntityResource.class, null, sharedId + "/content", null, 200);

        assertEquals(content1Text, response.getResponse());
        assertEquals(expectedMimeType+";charset=UTF-8", response.getHeaders().get("Content-Type"));
        assertEquals("attachment; filename=\"" + docName1 + "\"; filename*=UTF-8''" + docName1 + "", response.getHeaders().get("Content-Disposition"));


        // -ve test - try to get non-existent rendition content
        getSingle(QuickShareLinkEntityResource.class, null, sharedId + "/renditions/doclib/content", null, 404);

        // create rendition
        Rendition rendition = createAndGetRendition(user2, d1Id, "doclib");
        assertNotNull(rendition);
        assertEquals(Rendition.RenditionStatus.CREATED, rendition.getStatus());

        // unauth access to get shared link file rendition content
        response = getSingle(QuickShareLinkEntityResource.class, null, sharedId + "/renditions/doclib/content", null, 200);

        String docName = "doclib";
        assertEquals(MimetypeMap.MIMETYPE_IMAGE_PNG+";charset=UTF-8", response.getHeaders().get("Content-Type"));
        assertEquals("attachment; filename=\"" + docName + "\"; filename*=UTF-8''" + docName + "", response.getHeaders().get("Content-Disposition"));


        // -ve delete tests
        {
            // -ve test - user1 cannot delete shared link
            delete(URL_SHARED_LINKS, user1, sharedId, 403);

            // -ve test - delete - cannot delete non-existent link
            delete(URL_SHARED_LINKS, user1, "dummy", 404);
        }


        // -ve create tests
        {
            // As user 1 ...

            // -ve test - try to create again (different user, that has read permission) - already exists
            post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 409);

            // -ve - create - missing nodeId
            body = new HashMap<>();
            post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 400);

            // -ve - create - unknown nodeId
            body = new HashMap<>();
            body.put("nodeId", "dummy");
            post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 404);

            // -ve - create - try to link to folder (ie. not a file)
            String f1Id = createFolder(user1, myFolderNodeId, "f1 " + RUNID).getId();
            body = new HashMap<>();
            body.put("nodeId", f1Id);
            post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 400);

            // -ve test - cannot create if user does not have permission to read
            body = new HashMap<>();
            body.put("nodeId", d2Id);
            post(URL_SHARED_LINKS, user2, toJsonAsStringNonNull(body), 403);
        }


        // delete shared link
        delete(URL_SHARED_LINKS, user2, sharedId, 204);

        // -ve test - delete - cannot delete non-existent link
        delete(URL_SHARED_LINKS, user1, sharedId, 404);


        response = getSingle(NodesEntityResource.class, user2, d1Id, null, 200);
        nodeResp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), Node.class);

        assertEquals(docModifiedAt.getTime(), nodeResp.getModifiedAt().getTime()); // not changed
        assertEquals(docModifiedBy, nodeResp.getModifiedByUser().getId()); // not changed (ie. not user2)


        // -ve get tests
        {
            // try to get link that has been deleted (see above)
            getSingle(QuickShareLinkEntityResource.class, null, sharedId, null, 404);
            getSingle(QuickShareLinkEntityResource.class, null, sharedId + "/content", null, 404);

            // try to get non-existent link
            getSingle(QuickShareLinkEntityResource.class, null, "dummy", null, 404);
            getSingle(QuickShareLinkEntityResource.class, null, "dummy/content", null, 404);
        }

        // TODO if and when these tests are optionally runnable via remote env then we could skip this part of the test
        // (else need to verify test mechanism for enterprise admin via jmx ... etc)

        QuickShareLinksImpl quickShareLinks = applicationContext.getBean("quickShareLinks", QuickShareLinksImpl.class);
        try
        {
            quickShareLinks.setEnabled(false);

            // -ve - disabled service tests
            body.put("nodeId", "dummy");
            post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 501);

            getSingle(QuickShareLinkEntityResource.class, null, "dummy", null, 501);
            getSingle(QuickShareLinkEntityResource.class, null, "dummy/content", null, 501);
            delete(URL_SHARED_LINKS, user1, "dummy", 501);
        }
        finally
        {
            quickShareLinks.setEnabled(true);
        }
    }

    /**
     * Tests find shared links to file (content)
     *
     * Note: relies on search service
     *
     * <p>GET:</p>
     * {@literal <host>:<port>/alfresco/api/<networkId>/public/alfresco/versions/1/shared-links}
     */
    @Test
    public void testSharedLinkFind() throws Exception
    {
        Paging paging = getPaging(0, 100);

        // Get all shared links visible to user 1 (note: for now assumes clean repo)
        HttpResponse response = getAll(URL_SHARED_LINKS, user1, paging, 200);
        List<QuickShareLink> sharedLinks = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), QuickShareLink.class);
        assertEquals(0, sharedLinks.size());

        // As user 1 ...

        // create doc d1 - in "My" folder
        String myFolderNodeId = getMyNodeId(user1);
        String content1Text = "The quick brown fox jumps over the lazy dog 1.";
        String docName1 = "content" + RUNID + "_1.txt";
        Document doc1 = createTextFile(user1, myFolderNodeId, docName1, content1Text);
        String d1Id = doc1.getId();

        // create doc d2 - in "Shared" folder
        String sharedFolderNodeId = getSharedNodeId(user1);
        String content2Text = "The quick brown fox jumps over the lazy dog 2.";
        String docName2 = "content" + RUNID + "_2.txt";
        Document doc2 = createTextFile(user1, sharedFolderNodeId, docName2, content1Text);
        String d2Id = doc2.getId();


        // As user 1 ...

        // create shared link to doc 1
        Map<String, String> body = new HashMap<>();
        body.put("nodeId", d1Id);
        response = post(URL_SHARED_LINKS, user1, toJsonAsStringNonNull(body), 201);
        QuickShareLink resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);
        String shared1Id = resp.getId();

        // As user 2 ...

        // create shared link to doc 2
        body = new HashMap<>();
        body.put("nodeId", d2Id);
        response = post(URL_SHARED_LINKS, user2, toJsonAsStringNonNull(body), 201);
        resp = RestApiUtil.parseRestApiEntry(response.getJsonResponse(), QuickShareLink.class);
        String shared2Id = resp.getId();


        //
        // find links
        //

        response = getAll(URL_SHARED_LINKS, user1, paging, 200);
        sharedLinks = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), QuickShareLink.class);
        assertEquals(2, sharedLinks.size());
        assertEquals(shared2Id, sharedLinks.get(0).getId());
        assertEquals(d2Id, sharedLinks.get(0).getNodeId());
        assertEquals(shared1Id, sharedLinks.get(1).getId());
        assertEquals(d1Id, sharedLinks.get(1).getNodeId());

        response = getAll(URL_SHARED_LINKS, user2, paging, 200);
        sharedLinks = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), QuickShareLink.class);
        assertEquals(1, sharedLinks.size());
        assertEquals(shared2Id, sharedLinks.get(0).getId());
        assertEquals(d2Id, sharedLinks.get(0).getNodeId());

        // find my links
        Map<String, String> params = new HashMap<>();
        params.put("where", "("+ QuickShareLinks.PARAM_SHAREDBY+"='"+People.DEFAULT_USER+"')");

        response = getAll(URL_SHARED_LINKS, user1, paging, params, 200);
        sharedLinks = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), QuickShareLink.class);
        assertEquals(1, sharedLinks.size());
        assertEquals(shared1Id, sharedLinks.get(0).getId());
        assertEquals(d1Id, sharedLinks.get(0).getNodeId());

        // find links shared by a given user
        params = new HashMap<>();
        params.put("where", "("+ QuickShareLinks.PARAM_SHAREDBY+"='"+user2+"')");

        response = getAll(URL_SHARED_LINKS, user1, paging, params, 200);
        sharedLinks = RestApiUtil.parseRestApiEntries(response.getJsonResponse(), QuickShareLink.class);
        assertEquals(1, sharedLinks.size());
        assertEquals(shared2Id, sharedLinks.get(0).getId());
        assertEquals(d2Id, sharedLinks.get(0).getNodeId());


        // delete the shared links
        delete(URL_SHARED_LINKS, user1, shared1Id, 204);
        delete(URL_SHARED_LINKS, user2, shared2Id, 204);


        // TODO if and when these tests are optionally runnable via remote env then we could skip this part of the test
        // (else need to verify test mechanism for enterprise admin via jmx ... etc)

        QuickShareLinksImpl quickShareLinks = applicationContext.getBean("quickShareLinks", QuickShareLinksImpl.class);
        try
        {
            quickShareLinks.setEnabled(false);

            // -ve - disabled service tests
            getAll(URL_SHARED_LINKS, user1, paging, 501);
        }
        finally
        {
            quickShareLinks.setEnabled(true);
        }
    }

    @Override
    public String getScope()
    {
        return "public";
    }
}
