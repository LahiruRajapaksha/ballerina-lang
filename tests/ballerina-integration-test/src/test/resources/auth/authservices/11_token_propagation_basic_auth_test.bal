// Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/auth;
import ballerina/http;

auth:ConfigAuthStoreProvider basicAuthProvider11 = new;
http:BasicAuthnHandler basicAuthnHandler11 = new(basicAuthProvider11);

listener http:Listener listener11 = new(9192, config = {
    auth: {
        authnHandlers: [basicAuthnHandler11]
    },
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

http:Client nyseEP03 = new("https://localhost:9193", config = {
    auth: {
        scheme: http:JWT_AUTH,
        config: {
            inferredJwtIssuerConfig: {
                issuer: "ballerina",
                audience: ["ballerina"],
                keyAlias: "ballerina",
                keyPassword: "ballerina",
                keyStore: {
                    path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
                    password: "ballerina"
                }
            }
        }
    }
});

@http:ServiceConfig { basePath: "/passthrough" }
service passthroughService03 on listener11 {

    @http:ResourceConfig {
        methods: ["GET"],
        path: "/"
    }
    resource function passthrough(http:Caller caller, http:Request clientRequest) {
        var response = nyseEP03->get("/nyseStock/stocks", message = untaint clientRequest);
        if (response is http:Response) {
            checkpanic caller->respond(response);
        } else {
            http:Response resp = new;
            json errMsg = { "error": "error occurred while invoking the service: " + response.reason() };
            resp.statusCode = 500;
            resp.setPayload(errMsg);
            checkpanic caller->respond(resp);
        }
    }
}

auth:JWTAuthProvider jwtAuthProvider11 = new({
    issuer: "ballerina",
    audience: ["ballerina"],
    certificateAlias: "ballerina",
    trustStore: {
        path: "${ballerina.home}/bre/security/ballerinaTruststore.p12",
        password: "ballerina"
    }
});

http:JwtAuthnHandler jwtAuthnHandler11 = new(jwtAuthProvider11);

listener http:Listener listener2 = new(9193, config = {
    auth: {
        authnHandlers: [jwtAuthnHandler11]
    },
    secureSocket: {
        keyStore: {
            path: "${ballerina.home}/bre/security/ballerinaKeystore.p12",
            password: "ballerina"
        }
    }
});

@http:ServiceConfig { basePath: "/nyseStock" }
service nyseStockQuote03 on listener2 {

    @http:ResourceConfig {
        methods: ["GET"],
        path: "/stocks"
    }
    resource function stocks(http:Caller caller, http:Request clientRequest) {
        json payload = { "exchange": "nyse", "name": "IBM", "value": "127.50" };
        checkpanic caller->respond(payload);
    }
}
