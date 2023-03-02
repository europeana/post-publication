package eu.europeana.postpublication;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.apache.logging.log4j.LogManager;
import org.springframework.context.ConfigurableApplicationContext;

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
                DataSourceAutoConfiguration.class,
                // disable Spring Mongo auto config
                MongoAutoConfiguration.class,
                MongoDataAutoConfiguration.class,
                //disable solr auto configuration health checks
                SolrAutoConfiguration.class,
                // disable embedded Mongo
                EmbeddedMongoAutoConfiguration.class
        })
@EnableBatchProcessing
public class PostPublicationApp {

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application paramaters
     */
    public static void main(String[] args) {
        ConfigurableApplicationContext context =  SpringApplication.run(PostPublicationApp.class, args);
        System.exit(SpringApplication.exit(context));
    }
}
