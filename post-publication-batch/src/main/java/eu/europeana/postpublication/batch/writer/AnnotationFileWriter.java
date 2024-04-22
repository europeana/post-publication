package eu.europeana.postpublication.batch.writer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.europeana.annotation.definitions.model.Annotation;
import eu.europeana.postpublication.batch.config.PostPublicationSettings;
import eu.europeana.postpublication.debias.utils.SerialisationUtils;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.file.transform.LineAggregator;
import org.springframework.core.io.FileSystemResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * This will write the list of annotations into a file
 * @author srishti singh
 * @since 19 April 2024
 */
public class AnnotationFileWriter extends FlatFileItemWriterBuilder<List<Annotation>>  {

    private final ObjectMapper mapper = new ObjectMapper();

    private static final String[] FIELDS_TO_WRITE = new String[]{"annotations"};
    private static final char DELIMITER = ';';
    private PostPublicationSettings settings;


    public AnnotationFileWriter(PostPublicationSettings settings) {
        this.settings = settings;
    }

    @Override
    public FlatFileItemWriter<List<Annotation>> build() {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.name("Annotation File Writer")
                .headerCallback(createHeaderCallBack())
                .lineAggregator(createLineAggregator())
                .resource(new FileSystemResource(settings.getAnnotationsFileName()))
                .shouldDeleteIfExists(true);
        return super.build();
    }

    private FlatFileHeaderCallback createHeaderCallBack() {
        return writer -> writer.write(String.join(String.valueOf(DELIMITER), FIELDS_TO_WRITE));
    }


    @SuppressWarnings("java:S109")
    private LineAggregator<List<Annotation>> createLineAggregator() {
        return annotations -> {
            try (OutputStream stream = new ByteArrayOutputStream()) {
            for (Annotation annotation : annotations) {
                SerialisationUtils.serialiseAnnotation(mapper, annotation, stream);
                stream.write('\n');
                }
                return stream.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
    }
}
