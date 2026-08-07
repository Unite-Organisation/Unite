package com.app.prod.post.service;

import com.app.prod.exceptions.AppError;
import com.app.prod.exceptions.Code;
import com.app.prod.exceptions.exceptions.BadRequestException;
import com.app.prod.exceptions.exceptions.EntityNotPresentException;
import com.app.prod.interaction.enums.InteractionEntityType;
import com.app.prod.interaction.enums.InteractionType;
import com.app.prod.interaction.repository.InteractionRepository;
import com.app.prod.interaction.service.InteractionTarget;
import com.app.prod.post.enums.PostType;
import com.app.prod.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.sources.tables.records.PostRecord;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PostInteractionTarget implements InteractionTarget {

    private final PostRepository postRepository;
    private final InteractionRepository interactionRepository;

    @Override
    public InteractionEntityType entityType() {
        return InteractionEntityType.POST;
    }

    @Override
    public void assertVisible(UUID userId, UUID entityId) {
        findVisiblePostType(userId, entityId);
    }

    @Override
    public void assertInteractionAllowed(UUID userId, UUID entityId, InteractionType interactionType) {
        if (interactionType != InteractionType.ATTENDING) {
            assertVisible(userId, entityId);
            return;
        }

        assertCanAttend(userId, entityId);
    }

    private void assertCanAttend(UUID userId, UUID eventId) {
        PostRecord event = postRepository.findVisibleForUpdate(userId, eventId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));

        PostType postType = PostType.valueOf(event.getPostType());
        if (postType != PostType.EVENT) {
            throw new BadRequestException(AppError.of(
                    Code.INTERACTION_NOT_SUPPORTED,
                    String.format("%s is not available for %s", InteractionType.ATTENDING, postType)
            ));
        }

        Integer maxAttendees = event.getMaxAttendees();
        if (maxAttendees == null) {
            return;
        }

        int attendeesCount = interactionRepository.countInteractions(entityType(), eventId, InteractionType.ATTENDING);
        if (attendeesCount >= maxAttendees) {
            throw new BadRequestException(AppError.of(Code.EVENT_MAX_ATTENDEES));
        }
    }

    private PostType findVisiblePostType(UUID userId, UUID postId) {
        return postRepository.findVisiblePostType(userId, postId)
                .orElseThrow(() -> new EntityNotPresentException(AppError.of(Code.POST_NOT_FOUND)));
    }
}
