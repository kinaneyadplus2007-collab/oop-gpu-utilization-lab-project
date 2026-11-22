package storage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import model.RuleException;

public class FileHelper {

    public static void writeLines(Path file, List<String> lines)
            throws RuleException {

        try {
            Files.write(file, lines, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuleException(
                    "Could not write " + file + ": " + e.getMessage()
            );
        }
    }

    public static List<String> readLines(Path file)
            throws RuleException {

        if (!Files.exists(file)) {
            return new ArrayList<>();
        }

        try {
            return Files.readAllLines(file, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new RuleException(
                    "Could not read " + file + ": " + e.getMessage()
            );
        }
    }
}