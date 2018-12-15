package com.lee.mvc.server;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.conf.ServerConfiguration;
import com.lee.mvc.dispatcher.DispatcherServlet;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.jasper.servlet.JspServlet;
import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/15 10:36
 */
@Slf4j
public class TomcatServer implements Server {

    private Tomcat tomcat;

    public TomcatServer(ServerConfiguration configuration) {
        try {
            this.tomcat = new Tomcat();
            tomcat.setBaseDir(configuration.getDocBase());
            tomcat.setPort(configuration.getServerPort());
            File root = getRootFolder();
            File webContentFolder = new File(root.getAbsolutePath(), configuration.getResourcePath());
            if (!webContentFolder.exists()) {
                webContentFolder = Files.createTempDirectory("default-doc-base").toFile();
            }
            log.info("Tomcat:configuring app with basedir: [{}]", webContentFolder.getAbsolutePath());
            StandardContext ctx = (StandardContext) tomcat.addWebapp(configuration.getContextPath(), webContentFolder.getAbsolutePath());
            ctx.setParentClassLoader(this.getClass().getClassLoader());
            WebResourceRoot resources = new StandardRoot(ctx);
            ctx.setResources(resources);
            // 去除了JspHandler和SimpleUrlHandler这两个servlet的注册
            tomcat.addServlet(configuration.getContextPath(), "dispatcherServlet", new DispatcherServlet()).setLoadOnStartup(0);
            ctx.addServletMappingDecoded("/*", "dispatcherServlet");
        } catch (Exception e) {
            log.error("初始化Tomcat失败", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void startServer() throws Exception {
        tomcat.start();
        String address = tomcat.getServer().getAddress();
        int port = tomcat.getConnector().getPort();
        log.info("local address: http://{}:{}", address, port);
        tomcat.getServer().await();
    }

    @Override
    public void stopServer() throws Exception {
        tomcat.stop();
    }

    private File getRootFolder() {
        try {
            String runningJarPath = this.getClass().getProtectionDomain().getCodeSource()
                    .getLocation().toURI().getPath().replaceAll("\\\\", "/");
            File root = Optional.of(runningJarPath.lastIndexOf("/target/"))
                    .filter(it -> it >= 0)
                    .map(it -> new File(runningJarPath.substring(0, it)))
                    .orElseGet(() -> new File(""));
            log.info("Tomcat:application resolved root folder: [{}]", root.getAbsolutePath());
            return root;
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
}
