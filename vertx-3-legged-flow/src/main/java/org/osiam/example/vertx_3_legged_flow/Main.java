/*
 * Copyright (C) 2013 tarent AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.osiam.example.vertx_3_legged_flow;

import java.io.IOException;
import java.net.URI;

import org.osiam.client.OsiamConnector;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.oauth.Scope;
import org.osiam.resources.scim.User;
import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.VertxFactory;
import org.vertx.java.core.http.HttpServerRequest;

public class Main {

    public static void main(String[] args) throws IOException {
        Vertx vertx = VertxFactory.newVertx();
        vertx.createHttpServer().requestHandler(new Handler<HttpServerRequest>() {

            OsiamConnector oConnector = new OsiamConnector.Builder()
                    .setEndpoint("http://localhost:8080/osiam")
                    .setClientId("example-client")
                    .setClientSecret("secret")
                    .setClientRedirectUri("http://localhost:5000/oauth2")
                    .build();

            @Override
            public void handle(HttpServerRequest req) {
                String path = req.path();
                if (path.equals("/login")) {
                    URI uri = oConnector.getAuthorizationUri(Scope.ME);
                    req.response().setStatusCode(301).putHeader("Location", uri.toString()).end();
                }
                if (path.equals("/oauth2")) {
                    MultiMap multiMap = req.params();
                    if (multiMap.get("error") != null) {
                        req.response().setChunked(true).write("The user has denied your request").end();
                    } else {
                        // the User has granted your rights to his resources
                        String code = multiMap.get("code");
                        AccessToken accessToken;
                        User user;
                        try {
                            accessToken = oConnector.retrieveAccessToken(code);
                            user = oConnector.getCurrentUser(accessToken);
                        } catch (Exception e) {
                            req.response().setChunked(true).write("An error happened: " + e).end();
                            return;
                        }
                        req.response().setChunked(true);
                        req.response().headers().add("Content-Type", "text/html; charset=UTF-8");
                        req.response()
                                .write("<html><head><link rel='stylesheet' type='text/css' href='bootstrap.min.css'/>"
                                        + "<link rel='stylesheet' type='text/css' href='style.css'/></head>"
                                        + "<body><div class='container'><h1>Hello " + user.getName().getGivenName()
                                        + " " + user.getName().getFamilyName() + "</h1><br />Your AccessToken: "
                                        + accessToken.getToken() + "</div></body></html>", "UTF-8").end();
                    }
                }
                else {
                    String file = path.equals("/") ? "index.html" : path;
                    req.response().sendFile("webroot/" + file);
                }
            }
        }).listen(5000);

        System.out.println("Server running at http://localhost:5000/");
        System.out.println("Hit <Return> to quit.");
        System.in.read();
    }
}
