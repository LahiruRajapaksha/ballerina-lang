// Copyright (c) 2020 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

# Represents a POP Client, which interacts with a POP Server.
public type PopClient client object {

    # Gets invoked during object initialization.
    #
    # + clientConfig - Configurations for the POP Client
    # + return - An `error` if failed while creating the client
    public function __init(PopConfig clientConfig) returns error? {
        return initPopClientEndpoint(self, clientConfig, true);
    }

    # Used to read a message.
    #
    # + filter - Filter parameters to read an email
    # + return - An `error` if failed to send the message to the recipient
    public remote function read(Filter filter = { folder: "INBOX" }) returns Email|error? {
        return popRead(self, filter);
    }

};

# Configuration of the POP Endpoint.
#
# + host - Host address of the POP server
# + port - Port number of the POP server
# + ssl - SSL enable
# + username - Username to access the POP server
# + password - Password to access the POP server
public type PopConfig record {|
    string host;
    int port = 995;
    boolean ssl = true;
    string username;
    string password;
|};
