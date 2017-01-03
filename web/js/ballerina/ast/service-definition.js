/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
define(['lodash', './node'], function (_, ASTNode) {

    var ServiceDefinition = function (args) {
        this._serviceName = _.get(args, 'serviceName');
        this._annotations = _.get(args, 'annotations', []);
        this._resourceDefinitions = _.get(args, 'resourceDefinitions', []);
        this._variableDeclarations = _.get(args, 'variableDeclarations', []);
        this._connectionDeclarations = _.get(args, 'connectionDeclarations', []);

        // Adding available annotations and their default values.
        if (_.isNil(_.find(this._annotations, function (annotation) {
                return annotation.key == "BasePath";
            }))) {
            this._annotations.push({
                key: "BasePath",
                value: "/"
            });
        }

        if (_.isNil(_.find(this._annotations, function (annotation) {
                return annotation.key == "Source:interface";
            }))) {
            this._annotations.push({
                key: "Source:interface",
                value: ""
            });
        }

        if (_.isNil(_.find(this._annotations, function (annotation) {
                return annotation.key == "Service:description";
            }))) {
            this._annotations.push({
                key: "Service:description",
                value: ""
            });
        }

        // TODO: All the types should be referred from the global constants
        ASTNode.call(this, 'Service');
    };

    ServiceDefinition.prototype = Object.create(ASTNode.prototype);
    ServiceDefinition.prototype.constructor = ServiceDefinition;

    ServiceDefinition.prototype.setServiceName = function (serviceName) {
        if(!_.isNil(serviceName)){
            this._serviceName = serviceName;
        }
    };

    ServiceDefinition.prototype.setResourceDefinitions = function (resourceDefinitions) {
        if (!_.isNil(resourceDefinitions)) {
            this._resourceDefinitions = resourceDefinitions;
        }
    };

    ServiceDefinition.prototype.setVariableDeclarations = function (variableDeclarations) {
        if (!_.isNil(variableDeclarations)) {
            this._variableDeclarations = variableDeclarations;
        }
    };

    ServiceDefinition.prototype.setConnectionDeclarations = function (connectionDeclarations) {
        if (!_.isNil(connectionDeclarations)) {
            this._connectionDeclarations = connectionDeclarations;
        }
    };

    ServiceDefinition.prototype.getServiceName = function () {
        return this._serviceName;
    };

    ServiceDefinition.prototype.getAnnotations = function () {
        return this._annotations;
    };

    ServiceDefinition.prototype.getResourceDefinitions = function () {
        return this._resourceDefinitions;
    };

    ServiceDefinition.prototype.getVariableDeclarations = function () {
        return this._variableDeclarations;
    };

    ServiceDefinition.prototype.getConnectionDeclarations = function () {
        return this._connectionDeclarations;
    };

    /**
     * Adding/Updating an annotation.
     * @param key - Annotation key
     * @param value - Value for the annotation.
     */
    ServiceDefinition.prototype.addAnnotation = function (key, value) {
        var existingAnnotation = _.find(this._annotations, function (annotation) {
            return annotation.key == key;
        });
        if (_.isNil(existingAnnotation)) {
            // If such annotation does not exists, then add a new one.
            this._annotations.push({
                key: key,
                value: value
            });
        } else {
            // Updating existing annotation.
            existingAnnotation.value = value;
        }
    };

    /**
     * Validates possible immediate child types.
     * @override
     * @param node
     * @return {boolean}
     */
    ServiceDefinition.prototype.canBeParentOf = function (node) {
        var BallerinaASTFactory = this.getFactory();
        return BallerinaASTFactory.isResourceDefinition(node)
            || BallerinaASTFactory.isVariableDeclaration(node)
            || BallerinaASTFactory.isConnectorDeclaration(node);
    };

    return ServiceDefinition;

});
