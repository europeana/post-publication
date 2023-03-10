package eu.europeana.postpublication;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.apache.logging.log4j.LogManager;
/**
 * Main application
 *
 * @author Srishti Singh
 * Created on 06-02-2023
 */
@SpringBootApplication(
        scanBasePackages = {"eu.europeana.postpublication"},
        exclude = {
                // Remove these exclusions to re-enable security
                SecurityAutoConfiguration.class,
                ManagementWebSecurityAutoConfiguration.class,
                // DataSources are manually configured
                DataSourceAutoConfiguration.class
        })
@EnableBatchProcessing
public class PostPublicationApp extends SpringBootServletInitializer {

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application paramaters
     */
    public static void main(String[] args) {
        // When deploying to Cloud Foundry, this will log the instance index number, IP and GUID
        LogManager.getLogger(PostPublicationApp.class)
                .info("CF_INSTANCE_INDEX  = {}, CF_INSTANCE_GUID = {}, CF_INSTANCE_IP  = {}",
                        System.getenv("CF_INSTANCE_INDEX"),
                        System.getenv("CF_INSTANCE_GUID"),
                        System.getenv("CF_INSTANCE_IP"));

        SpringApplication.run(PostPublicationApp.class, args);
    }

}
