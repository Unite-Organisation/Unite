package com.app.prod.storage;

import com.app.prod.config.StorageProperties;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.storage.dto.FileUploadRequest;
import com.app.prod.storage.dto.PresignedUpload;
import com.app.prod.storage.dto.StoredFile;
import com.app.prod.storage.dto.StoredObject;
import com.app.prod.storage.file.FileService;
import com.app.prod.utils.json.JsonbService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.app.prod.storage.ContextStoragePrefix.ANNOUNCEMENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OTHER_USER_ID = UUID.randomUUID();
    private static final long ONE_MEGABYTE = 1024 * 1024;

    @Mock
    private AbstractStorage storage;

    private FileService fileService;
    private JsonbService jsonbService;
    private Clock clock;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        jsonbService = new JsonbService(objectMapper);
        clock = Clock.fixed(Instant.parse("2026-08-02T10:00:00Z"), ZoneId.of("UTC"));
        fileService = new FileService(storage, jsonbService, new StorageProperties(), clock);

        when(storage.createUploadUrl(anyString(), anyString(), any())).thenAnswer(invocation -> new PresignedUpload(
                invocation.getArgument(0),
                "https://upload/" + invocation.getArgument(0, String.class),
                "PUT",
                Map.of("Content-Type", invocation.getArgument(1, String.class)),
                LocalDateTime.now(clock).plusMinutes(15)
        ));
        when(storage.getPrivateFileUrl(anyString())).thenAnswer(invocation -> "https://download/" + invocation.getArgument(0, String.class));
    }

    @Test
    void shouldCreateOneUploadUrlPerFile() {
        var files = List.of(photoRequest("cat.jpg"), photoRequest("dog.png"));

        var uploads = fileService.createUploadUrls(ANNOUNCEMENT, USER_ID, files);

        assertThat(uploads).hasSize(2);
        assertThat(uploads).allSatisfy(upload -> {
            assertThat(upload.key()).startsWith("announcement/" + USER_ID + "/");
            assertThat(upload.method()).isEqualTo("PUT");
        });
    }

    @Test
    void shouldRejectMoreFilesThanContextAllows() {
        var files = List.of(photoRequest("1.jpg"), photoRequest("2.jpg"), photoRequest("3.jpg"),
                photoRequest("4.jpg"), photoRequest("5.jpg"), photoRequest("6.jpg"));

        assertThatThrownBy(() -> fileService.createUploadUrls(ANNOUNCEMENT, USER_ID, files))
                .isInstanceOf(BadRequestException.class);
        verify(storage, never()).createUploadUrl(anyString(), anyString(), any());
    }

    @Test
    void shouldRejectUnsupportedMimeType() {
        var files = List.of(new FileUploadRequest("virus.exe", "application/octet-stream", ONE_MEGABYTE));

        assertThatThrownBy(() -> fileService.createUploadUrls(ANNOUNCEMENT, USER_ID, files))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectTypeNotAllowedForContext() {
        var files = List.of(new FileUploadRequest("clip.mp4", "video/mp4", ONE_MEGABYTE));

        assertThatThrownBy(() -> fileService.createUploadUrls(ANNOUNCEMENT, USER_ID, files))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectFileLargerThanLimit() {
        var files = List.of(new FileUploadRequest("huge.jpg", "image/jpeg", 51 * ONE_MEGABYTE));

        assertThatThrownBy(() -> fileService.createUploadUrls(ANNOUNCEMENT, USER_ID, files))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldConfirmUploadedFiles() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");
        when(storage.find(key)).thenReturn(Optional.of(new StoredObject(key, "image/jpeg", ONE_MEGABYTE)));

        var attachments = fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of(key));

        var stored = jsonbService.listFromJsonb(attachments, StoredFile.class);
        assertThat(stored).hasSize(1);
        assertThat(stored.getFirst().key()).isEqualTo(key);
        assertThat(stored.getFirst().contentType()).isEqualTo("image/jpeg");
        assertThat(stored.getFirst().size()).isEqualTo(ONE_MEGABYTE);
        assertThat(stored.getFirst().uploadedAt()).isEqualTo(LocalDateTime.now(clock));
    }

    @Test
    void shouldReturnEmptyAttachmentsWhenNoKeysGiven() {
        assertThat(jsonbService.listFromJsonb(fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, null), StoredFile.class)).isEmpty();
        assertThat(jsonbService.listFromJsonb(fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of()), StoredFile.class)).isEmpty();
    }

    @Test
    void shouldRejectKeyBelongingToAnotherUser() {
        String key = StorageKeys.build(ANNOUNCEMENT, OTHER_USER_ID, "cat.jpg");
        when(storage.find(key)).thenReturn(Optional.of(new StoredObject(key, "image/jpeg", ONE_MEGABYTE)));

        assertThatThrownBy(() -> fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of(key)))
                .isInstanceOf(BadRequestException.class);
        verify(storage, never()).find(anyString());
    }

    @Test
    void shouldRejectKeyThatWasNeverUploaded() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");
        when(storage.find(key)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of(key)))
                .isInstanceOf(EntityNotPresentException.class);
    }

    @Test
    void shouldRejectFileWhoseRealTypeDiffersFromDeclaredOne() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");
        when(storage.find(key)).thenReturn(Optional.of(new StoredObject(key, "application/zip", ONE_MEGABYTE)));

        assertThatThrownBy(() -> fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of(key)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldRejectDuplicatedKeys() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");
        when(storage.find(key)).thenReturn(Optional.of(new StoredObject(key, "image/jpeg", ONE_MEGABYTE)));

        assertThatThrownBy(() -> fileService.confirmUploaded(ANNOUNCEMENT, USER_ID, List.of(key, key)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldBuildSignedUrlForEveryStoredAttachment() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");
        var attachments = jsonbService.toJsonb(List.of(new StoredFile(key, "image/jpeg", ONE_MEGABYTE, LocalDateTime.now(clock))));

        var responses = fileService.toResponses(attachments);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().key()).isEqualTo(key);
        assertThat(responses.getFirst().url()).isEqualTo("https://download/" + key);
        assertThat(responses.getFirst().contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void shouldReturnNoResponsesForEmptyAttachments() {
        assertThat(fileService.toResponses(null)).isEmpty();
    }

    private FileUploadRequest photoRequest(String fileName) {
        return new FileUploadRequest(fileName, "image/jpeg", ONE_MEGABYTE);
    }
}
