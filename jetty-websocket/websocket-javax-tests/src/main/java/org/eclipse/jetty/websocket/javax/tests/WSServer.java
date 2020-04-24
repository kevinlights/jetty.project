//
// ========================================================================
// Copyright (c) 1995-2020 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under
// the terms of the Eclipse Public License 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0
//
// This Source Code may also be made available under the following
// Secondary Licenses when the conditions for such availability set
// forth in the Eclipse Public License, v. 2.0 are satisfied:
// the Apache License v2.0 which is available at
// https://www.apache.org/licenses/LICENSE-2.0
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.websocket.javax.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.toolchain.test.FS;
import org.eclipse.jetty.toolchain.test.IO;
import org.eclipse.jetty.toolchain.test.MavenTestingUtils;
import org.eclipse.jetty.util.TypeUtil;
import org.eclipse.jetty.util.resource.PathResource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.javax.server.config.JavaxWebSocketConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Utility to build out exploded directory WebApps, in the /target/tests/ directory, for testing out servers that use javax.websocket endpoints.
 * <p>
 * This is particularly useful when the WebSocket endpoints are discovered via the javax.websocket annotation scanning.
 */
public class WSServer extends LocalServer implements LocalFuzzer.Provider
{
    private static final Logger LOG = LoggerFactory.getLogger(WSServer.class);
    private final Path testDir;
    private ContextHandlerCollection contexts = new ContextHandlerCollection();

    public WSServer(Path testDir)
    {
        this.testDir = testDir;
    }

    public WebApp createWebApp(String contextName)
    {
        return new WebApp(contextName);
    }

    @Override
    protected Handler createRootHandler(Server server)
    {
        return contexts;
    }

    public class WebApp
    {
        private final WebAppContext context;
        private final Path contextDir;
        private Path classesDir;
        private final Path webInf;

        private WebApp(String contextName)
        {
            // Ensure context directory.
            contextDir = testDir.resolve(contextName);
            FS.ensureEmpty(contextDir);

            // Ensure WEB-INF.
            webInf = contextDir.resolve("WEB-INF");
            FS.ensureDirExists(webInf);
            classesDir = webInf.resolve("classes");
            FS.ensureDirExists(classesDir);

            // Configure the WebAppContext.
            context = new WebAppContext();
            context.setContextPath("/" + contextName);
            context.setBaseResource(new PathResource(contextDir));
            context.setAttribute("org.eclipse.jetty.websocket.javax", Boolean.TRUE);
            context.addConfiguration(new JavaxWebSocketConfiguration());
        }

        public WebAppContext getWebAppContext()
        {
            return context;
        }

        public String getContextPath()
        {
            return context.getContextPath();
        }

        public Path getContextDir()
        {
            return contextDir;
        }

        public void createWebInf() throws IOException
        {
            copyWebInf("empty-web.xml");
        }

        public void copyWebInf(String testResourceName) throws IOException
        {
            File testWebXml = MavenTestingUtils.getTestResourceFile(testResourceName);
            Path webXml = webInf.resolve("web.xml");
            IO.copy(testWebXml, webXml.toFile());
        }

        public void copyClass(Class<?> clazz) throws Exception
        {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            String endpointPath = TypeUtil.toClassReference(clazz);
            URL classUrl = cl.getResource(endpointPath);
            assertThat("Class URL for: " + clazz, classUrl, notNullValue());
            Path destFile = classesDir.resolve(endpointPath);
            FS.ensureDirExists(destFile.getParent());
            File srcFile = new File(classUrl.toURI());
            IO.copy(srcFile, destFile.toFile());
        }

        public void deploy()
        {
            contexts.addHandler(context);
            contexts.manage(context);
            context.setThrowUnavailableOnStartupException(true);
            if (LOG.isDebugEnabled())
                LOG.debug("{}", context.dump());
        }
    }
}
