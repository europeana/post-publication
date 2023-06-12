package eu.europeana.postpublication;

import eu.europeana.postpublication.config.SocksProxyActivator;
import eu.europeana.postpublication.config.SocksProxyConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.solr.SolrAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

    private static final Logger logger = LogManager.getLogger(PostPublicationApp.class);

    /**
     * This method is called when starting as a Spring-Boot application (e.g. from your IDE)
     *
     * @param args main application paramaters
     */
    public static void main(String[] args) {

        // Activate socks proxy (if your application requires it)
        SocksProxyActivator.activate(
                new SocksProxyConfig("post-publication.properties", "post-publication.user.properties"));

        if (hasCmdLineParams(args)) {
            logger.info("Running the application with arguments -  args: {}", args);
            validateArguments(args);
            InputStream streamReader = PostPublicationApp.class.getResourceAsStream("/logs/TestLog.log");
            readLogFile(streamReader);
        }
        else {
            // disable web server since we're only running an update task
            ConfigurableApplicationContext context =
                    new SpringApplicationBuilder(PostPublicationApp.class)
                            .web(WebApplicationType.NONE)
                            .run(args);
            System.exit(SpringApplication.exit(context));
        }
    }


    static boolean hasCmdLineParams(String[] args) {
        return args!=null && args.length > 0;
    }

    /** validates the arguments passed */
    private static void validateArguments(String[] args) {
        for (String arg : args) {
            if (!StringUtils.equals(arg, "pushLogs")) {
                logger.error("Unsupported argument '{}'. Supported arguments are '{}'",
                        arg, "pushLogs");
                System.exit(1);
            }
        }
    }


    /**
     * Reads the FailedSetsReport generated at that time
     *
     * @return list of all the failed sets
     * returns "", if file is empty.
     */
    public static List<String> readLogFile(InputStream stream) {
        List<String> failedSets = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            String  nextLine;
            while ((nextLine = reader.readLine()) != null) {
                System.out.println(nextLine);
            }
        } catch (FileNotFoundException e) {
            logger.error("file doesn't exist." );
        } catch (IOException e) {
            logger.error("Error reading the file", e);
        }
        return failedSets;
    }


}
