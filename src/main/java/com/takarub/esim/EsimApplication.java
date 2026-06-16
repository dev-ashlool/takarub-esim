package com.takarub.esim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Application entry point for the Takarub eSIM platform.
 *
 * <p>Component scanning is rooted at {@code com.takarub.esim}, which covers the Identity
 * module and its shared layer. Feature modules are organised package-by-feature beneath
 * this root.
 */
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class
})
public class EsimApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsimApplication.class, args);
    }
}
