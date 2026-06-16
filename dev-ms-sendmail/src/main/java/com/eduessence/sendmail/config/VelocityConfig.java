package com.eduessence.sendmail.config;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * Velocity engine configurado para render desde String (los templates viven
 * en S3 y se descargan en runtime).
 */
@Configuration
public class VelocityConfig {

    @Bean
    public VelocityEngine velocityEngine() {
        VelocityEngine engine = new VelocityEngine();
        Properties props = new Properties();
        props.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        props.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        props.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        props.setProperty(RuntimeConstants.RUNTIME_REFERENCES_STRICT, "false");
        props.setProperty(RuntimeConstants.VM_LIBRARY_AUTORELOAD, "false");
        props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE, "true");
        props.setProperty(RuntimeConstants.VM_PERM_INLINE_LOCAL, "true");
        props.setProperty(RuntimeConstants.PARSER_POOL_SIZE, "20");
        engine.init(props);
        return engine;
    }
}
