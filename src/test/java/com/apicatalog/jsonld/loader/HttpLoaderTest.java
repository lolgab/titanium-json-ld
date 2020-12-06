/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.apicatalog.jsonld.loader;

import java.net.URISyntaxException;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.test.JsonLdManifestLoader;
import com.apicatalog.jsonld.test.JsonLdMockServer;
import com.apicatalog.jsonld.test.JsonLdTestCase;
import com.apicatalog.jsonld.test.JsonLdTestRunnerJunit;
import com.apicatalog.jsonld.test.loader.ClasspathLoader;
import com.apicatalog.jsonld.test.loader.UriBaseRewriter;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

public class HttpLoaderTest {

    @Rule
    public final WireMockRule wireMockRule = new WireMockRule();
    
    @Test
    public void testMissingContentType() throws URISyntaxException, JsonLdError {
     
        final JsonLdTestCase testCase = JsonLdManifestLoader
                .load("/com/apicatalog/jsonld/test/", "manifest.json", new ClasspathLoader())
                .stream()
                .filter(o -> "#t0002".equals(o.id))
                .findFirst().orElseThrow();

        testCase.contentType = null;
        
        execute(testCase);
    }

    @Test
    public void testPlainTextContentType() throws URISyntaxException, JsonLdError {
     
        final JsonLdTestCase testCase = JsonLdManifestLoader
                .load("/com/apicatalog/jsonld/test/", "manifest.json", new ClasspathLoader())
                .stream()
                .filter(o -> "#t0008".equals(o.id))
                .findFirst().orElseThrow();

        execute(testCase);
    }

    void execute(JsonLdTestCase testCase) {
        JsonLdMockServer server = new JsonLdMockServer(testCase, testCase.baseUri.substring(0, testCase.baseUri.length() - 1), "/com/apicatalog/jsonld/test/", new ClasspathLoader());

        try {
            
            server.start();
            
            (new JsonLdTestRunnerJunit(testCase)).execute(options -> {
                
                JsonLdOptions expandOptions = new JsonLdOptions(options);

                expandOptions.setDocumentLoader(
                                    new UriBaseRewriter(
                                                testCase.baseUri, 
                                                wireMockRule.baseUrl() + "/",
                                                SchemeRouter.defaultInstance()));
                
                return JsonDocument.of(JsonLd.expand(testCase.input).options(expandOptions).get());
            });

            server.stop();
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
}
