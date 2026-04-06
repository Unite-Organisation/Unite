package com.app.prod.post.service;

import com.app.prod.post.dto.AnnouncementRequest;
import com.app.prod.post.dto.PostResponse;
import com.app.prod.post.mappers.AnnouncementMapper;
import com.app.prod.post.repository.PostRepository;
import com.app.prod.utils.Pagination;
import com.app.prod.utils.filters.PostFilter;
import com.app.prod.utils.uploads.PhotosUploadService;
import com.app.prod.utils.validators.Validate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.app.prod.utils.uploads.UploadsManager.ANNOUNCEMENTS_PATH;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PhotosUploadService photosUploadService;
    private final Validate validate;

    public List<PostResponse> getPosts(Pagination pagination, UUID userId, PostFilter filter) {
        return postRepository.findForUser(userId, pagination, filter);
    }

    public Pair<byte[], MediaType> getPostPhoto(UUID id){
        validate.post(id);
        var announcement = postRepository.findById(id).get();

        log.info("Reading photo from disk for {}", announcement.getName());
        return readAndFetchPhoto(announcement.getImageReference());
    }

    public void addImageForPost(MultipartFile photo, UUID announcementId){
        validate.post(announcementId);
        String photoPath = photosUploadService.uploadFile(ANNOUNCEMENTS_PATH, photo);
        postRepository.updatePhotoPath(announcementId, photoPath);
    }

    private Pair<byte[], MediaType> readAndFetchPhoto(String photoPath){
        var photoData = photosUploadService.readFile(photoPath);
        byte[] file = photoData.getLeft();
        MediaType mediaType = photoData.getRight();

        return Pair.of(file, mediaType);
    }
}
