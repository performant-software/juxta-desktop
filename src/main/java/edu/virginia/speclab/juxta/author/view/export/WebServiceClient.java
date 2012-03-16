/*
 *  Copyright 2002-2012 The Rector and Visitors of the
 *                      University of Virginia. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package edu.virginia.speclab.juxta.author.view.export;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Client for sending RESTful requests and receiving responses
 * from the juxta web service
 * 
 * @author loufoster
 *
 */
public class WebServiceClient {
    private static final int REQUEST_TIMEOUT = 2 * 60 * 1000;   // 2 secs
    private String baseUrl;
    private String authToken;
    private HttpClient httpClient = newHttpClient();
    
    /**
     * URL that will be used to access juxtacommons
     */
    public static final String DEFAULT_URL = "http://juxta.performantsoftware.com";
    
    public WebServiceClient( final String url ) {
        this.baseUrl = url;
    }
    
    public String getBaseUrl() {
        return this.baseUrl;
    }
    
    /**
     * Contact the webservice at the current base URL and ensure that
     * it returns an ok response. If it does, return true.
     * 
     * @return True if service is alive at the root url; false otherwise
     */
    public boolean isAlive() {
        GetMethod get = new GetMethod(this.baseUrl);
        try {
            execRequest(get);
            get.releaseConnection();
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Create a juxta user account. This will throw if create request failed.
     * 
     * @param email
     * @param pass
     * @param confirm
     * @param confirm2 
     * @throws IOException 
     */
    public void createAccount( final String name, final String email, final String pass, final String confirm ) throws IOException {
        PostMethod post  = new PostMethod(this.baseUrl+"/login/create");        
        String json = "{\"user\": {"
            +"\"name\": \""+name
            +"\", \"email\": \""+email
            +"\", \"password\": \""
            +pass+"\", \"password_confirmation\": \""
            +confirm+"\"} }";
        post.setRequestEntity(new StringRequestEntity(json, "application/json", "utf-8"));
        post.setRequestHeader("accept", "application/json");
        int respCode = execRequest(post, false);
        if ( respCode != 200 ) {
            final String msg = getResponseString(post);
            post.releaseConnection();
            throw new IOException("Unable to create account: "+msg);
        }
        String out = getResponseString(post);
        System.out.println(out);
        post.releaseConnection();
    }
    
    /**
     * Attempt to authenticate a user at the specified base rl
     * @param serviceUrl
     * @param email
     * @param pass
     * @return The workspace or null
     */
    public boolean authenticate(final String email, final String pass) throws IOException {
        this.httpClient = newHttpClient();
        
        PostMethod post  = new PostMethod(this.baseUrl+"/import/authenticate");        
        NameValuePair[] data = {new NameValuePair("email", email),
                                new NameValuePair("password", pass)};
        post.setRequestBody(data);
        
        // exec with a false to prevent a throw on non-200 resp
        int respCode = execRequest(post, false);
        if ( respCode == 200 ) {
            String resp = getResponseString(post);
            JsonParser parser = new JsonParser();
            JsonObject obj = parser.parse(resp).getAsJsonObject();
            this.authToken = obj.get("token").getAsString();
            post.releaseConnection();
            return true;
        } else if ( respCode == 401 ) {
            // bad credentials
            post.releaseConnection();
        } else {
            // smething else bad. just throw it
            post.releaseConnection();
            throw new IOException( "Service Error (code "+respCode+")" );
        }

        return false;
    }
    
    /**
     * Send a cancel request to the webservice to abort the current
     * export operation.
     * @throws IOException 
     */
    public void cancelExport( Long id ) throws IOException {
        PostMethod post = new PostMethod(this.baseUrl+"/import/"+id+"/cancel");
        Part[] parts = {
            new StringPart("token", this.authToken)
        };
        post.setRequestEntity(
            new MultipartRequestEntity(parts, post.getParams())
            );        
        execRequest(post);
        post.releaseConnection();
    }


    /**
     * Start the process of exporting the JXT file to the web service.
     * @param jxtFile The txt file to export
     * @param baseWitness Base witness for the set
     * @param desc 
     * @return The indentifer for the started process. This is used for tracking status
     * @throws IOException 
     */
    public Long beginExport( final File jxtFile, final String name, final String desc )  throws IOException {
        return beginExport( jxtFile, name, desc, false);
    }
    public Long beginExport( final File jxtFile, final String name, final String desc, boolean overwrite ) throws IOException {
        
        PostMethod post = null;
        if ( overwrite ) {
            post = new PostMethod(this.baseUrl+"/import?overwrite");
        } else {
            post = new PostMethod(this.baseUrl+"/import");
        }
              
        Part[] parts = {
            new StringPart("token", this.authToken),
            new StringPart("setName", name),
            new StringPart("description", desc),
            new FilePart("jxtFile", jxtFile)
        };
        
        post.setRequestEntity(
            new MultipartRequestEntity(parts, post.getParams())
            );

        execRequest(post);
        String response = getResponseString(post);
        post.releaseConnection();
        return Long.parseLong(response);
    }
    
    /**
     * Check the status on an initated request
     * 
     * @param requestId
     * @return
     * @throws IOException 
     */
    public RequestStatus getExportStatus( Long requestId ) throws IOException {
        final String url = this.baseUrl+"/import/"+requestId;
        GetMethod get = new GetMethod(url);
        get.setRequestHeader("accept", "application/json");
        execRequest(get);
        String response = getResponseString(get);
        get.releaseConnection();
        return RequestStatus.fromResponse(response);
    }
   
    /**
     * Exec the given request on the juxta web service. The request may be of any type
     * (GET,POST,PUT,DELETE). If a non-succesful response is returned, this
     * method will throw.
     * 
     * @param request
     * @throws IOException
     * @return Response code
     */
    private final int execRequest( HttpMethod request ) throws IOException {
        return execRequest(request, true);
    }
    private final int execRequest( HttpMethod request, boolean shouldThrow ) throws IOException {
        
        int responseCode = this.httpClient.executeMethod(request);
        if (responseCode != 200 && shouldThrow) {
            String resp = getResponseString(request);
            throw new IOException(responseCode + " : " + resp);
        }
        return responseCode;
    }
    
    /**
     * Call after a request is made with the <code>execRequest</code> method. It will return a UTF-8
     * encode string containing the full response body from the service.
     * 
     * @param httpMethod
     * @return
     * @throws IOException
     */
    private final String getResponseString(HttpMethod httpMethod) throws IOException {
        InputStream is = httpMethod.getResponseBodyAsStream();
        return IOUtils.toString(is, "UTF-8");
    }

    private final HttpClient newHttpClient() {
        HttpClient httpClient = new HttpClient();
        httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(
            REQUEST_TIMEOUT);
        httpClient.getHttpConnectionManager().getParams().setIntParameter(
            HttpMethodParams.BUFFER_WARN_TRIGGER_LIMIT, 10000 * 1024); 
        return httpClient;
    }
   
    
    /**
     * Encapsulates the service status message data for presentation
     * in the desktop client
     * 
     * @author loufoster
     *
     */
    public static class RequestStatus {
        public enum Status {PENDING, PROCESSING, COMPLETE, CANCEL_REQUESTED, CANCELED, FAILED};
        private final Status status;
        private final String message;
        private static final JsonParser PARSER = new JsonParser();
        
        public static RequestStatus fromResponse(final String respStr) {
            JsonObject obj = RequestStatus.PARSER.parse(respStr).getAsJsonObject();
            String status = obj.get("status").getAsString();
            String msg = obj.get("note").getAsString();
            return new RequestStatus(Status.valueOf(status.trim().toUpperCase()), msg);
        }
        
        public RequestStatus( Status s, String msg ) {
            this.status = s;
            this.message = msg;
        }
        public final boolean isTerminated() {
            return ( this.status.equals(Status.PENDING) == false && 
                     this.status.equals(Status.PROCESSING) == false );
        }
        public final Status getStatus() {
            return status;
        }

        public final String getMessage() {
            return message;
        }
    }
}
