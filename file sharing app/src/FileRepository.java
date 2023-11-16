import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class FileRepository {
    public Map<String, byte[]> files;  
    public int size;

    public FileRepository() {
        files = new HashMap<>();
        size = 0;
    }

    public void publish(String fileName, String filePath) {
        try {
            // Read the content of the file from the local file system in chunks
            byte[] content = readFile(filePath);
            files.put(fileName, content);
            size++;
            System.out.println("File published successfully.");
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }

    public byte[] readFile(String filePath) throws IOException {

        try (var inputStream = Files.newInputStream(Path.of(filePath))) {
            byte[] buffer = new byte[4096];  
            int bytesRead;
            var byteArrayOutputStream = new java.io.ByteArrayOutputStream();

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }

            return byteArrayOutputStream.toByteArray();
        }
    }

    public byte[] getFileContent(String fileName) {
        return files.get(fileName);
    }
}
