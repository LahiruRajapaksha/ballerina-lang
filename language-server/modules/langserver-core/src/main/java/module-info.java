module io.ballerina.language.server.core {
    uses org.ballerinalang.langserver.commons.LanguageExtension;
    uses org.ballerinalang.langserver.commons.codeaction.spi.LSCodeActionProvider;
    uses org.ballerinalang.langserver.commons.codelenses.spi.LSCodeLensesProvider;
    uses org.ballerinalang.langserver.commons.command.spi.LSCommandExecutor;
    uses org.ballerinalang.langserver.commons.service.spi.ExtendedLanguageServerService;
    uses org.ballerinalang.langserver.commons.registration.BallerinaServerCapability;
    uses org.ballerinalang.langserver.commons.registration.BallerinaServerCapabilitySetter;
    uses org.ballerinalang.langserver.commons.registration.BallerinaClientCapabilitySetter;
    exports org.ballerinalang.langserver;
    exports org.ballerinalang.langserver.util.references;
    exports org.ballerinalang.langserver.common.utils;
    exports org.ballerinalang.langserver.common.constants;
    exports org.ballerinalang.langserver.codeaction.providers;
    exports org.ballerinalang.langserver.exception;
    exports org.ballerinalang.langserver.extensions;
    exports org.ballerinalang.langserver.config;
    exports org.ballerinalang.langserver.telemetry;
    exports org.ballerinalang.langserver.util to io.ballerina.language.server.simulator;
    requires io.ballerina.diagram.util;
    requires io.ballerina.formatter.core;
    requires org.eclipse.lsp4j;
    requires io.ballerina.language.server.commons;
    requires org.apache.commons.lang3;
    requires org.eclipse.lsp4j.jsonrpc;
    requires io.ballerina.lang;
    requires io.ballerina.runtime;
    requires org.apache.commons.io;
    requires io.ballerina.parser;
    requires io.ballerina.toml;
    requires jsr305;
    requires toml4j;
    requires io.ballerina.tools.api;
    requires com.google.common;
    requires com.google.gson;
    requires io.ballerina.syntaxapicallsgen;
    requires io.ballerina.central.client;
    requires org.ballerinalang.pkg.toml.semntic.analyzer;
}
