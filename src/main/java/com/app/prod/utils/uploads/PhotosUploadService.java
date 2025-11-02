package com.app.prod.utils.uploads;

import com.app.prod.exceptions.exceptions.EmptyFileException;
import com.app.prod.exceptions.exceptions.FileNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static com.app.prod.utils.uploads.UploadsManager.UPLOADS_PATH;

@RequiredArgsConstructor
@Service
@Slf4j
public class PhotosUploadService {

    private final UploadsManager uploadsPath;

    public String uploadFile(Path folder, MultipartFile file){
        checkIfFileIsNotEmpty(file);

        //TODO: check user input and handle extensions - accept only few
        Path fileName = getFileNameWithFileExtension(file);
        Path folderPath = uploadsPath.resolvePaths(UPLOADS_PATH, folder);
        Path filePath = uploadsPath.resolvePaths(folderPath, fileName);

        try{
            log.info("Saving uploaded file to destination: {} with name {}", folderPath, fileName);
            copyFileToDestination(file, filePath);
            log.info("Successfully saved file in destination {}", filePath);
        }catch (IOException e){
            log.error("Error while copying file to {}", filePath);
        }catch (Exception e){
            log.error("Unknown error while copying file to {}", filePath);
        }

        return filePath.toString();
    }

    public Pair<byte[], MediaType> readFile(String filePath){
        Path file = Paths.get(filePath);
        checkIfFileExists(file);

        try{

            byte[] fileBytes = Files.readAllBytes(file);
            String mimeType = Files.probeContentType(file);
            MediaType mediatype = MediaType.parseMediaType(mimeType);
            return Pair.of(fileBytes, mediatype);

        } catch (IOException e) {
            log.error("Error while reading file from {}", filePath);
            throw new RuntimeException(String.format("Error while reading file from %s", filePath));
        } catch (Exception e){
            log.error("Unknown error while reading file from {}", filePath);
            throw new RuntimeException(String.format("Error while reading file from %s", filePath));
        }

    }

    private void copyFileToDestination(MultipartFile file, Path where) throws IOException{
        Files.copy(file.getInputStream(), where, StandardCopyOption.REPLACE_EXISTING);
    }

    private void checkIfFileIsNotEmpty(MultipartFile file){
        if(file.isEmpty()){
            throw new EmptyFileException("File is empty");
        }
    }

    private void checkIfFileExists(Path filePath){
        if (!Files.exists(filePath)) {
            throw new FileNotFoundException(String.format("File with path %s was not found", filePath));
        }
    }

    private Path getFileNameWithFileExtension(MultipartFile file){
        String fileName = UUID.randomUUID() + file.getOriginalFilename();
        return Path.of(fileName);
    }

}
