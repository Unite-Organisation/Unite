package com.app.prod.storage;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.app.prod.storage.ContextStoragePrefix.ANNOUNCEMENT;
import static com.app.prod.storage.ContextStoragePrefix.EVENT;
import static org.assertj.core.api.Assertions.assertThat;

class StorageKeysTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Test
    void shouldBuildKeyWithContextAndOwner() {
        String key = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");

        assertThat(key).startsWith("announcement/" + USER_ID + "/");
        assertThat(key).endsWith("-cat.jpg");
        assertThat(StorageKeys.belongsTo(key, ANNOUNCEMENT, USER_ID)).isTrue();
    }

    @Test
    void shouldStripPathAndUnsafeCharactersFromFileName() {
        assertThat(StorageKeys.sanitize("../../etc/passwd")).isEqualTo("passwd");
        assertThat(StorageKeys.sanitize("my photo (1).jpg")).isEqualTo("my_photo__1_.jpg");
        assertThat(StorageKeys.sanitize("C:\\photos\\cat.jpg")).isEqualTo("cat.jpg");
        assertThat(StorageKeys.sanitize("   ")).isEqualTo("file");
        assertThat(StorageKeys.sanitize("..")).isEqualTo("file");
    }

    @Test
    void shouldRejectKeyOfAnotherUser() {
        String key = StorageKeys.build(ANNOUNCEMENT, UUID.randomUUID(), "cat.jpg");

        assertThat(StorageKeys.belongsTo(key, ANNOUNCEMENT, USER_ID)).isFalse();
    }

    @Test
    void shouldRejectKeyFromAnotherContext() {
        String key = StorageKeys.build(EVENT, USER_ID, "cat.jpg");

        assertThat(StorageKeys.belongsTo(key, ANNOUNCEMENT, USER_ID)).isFalse();
    }

    @Test
    void shouldBuildPendingKeyUnderTemporaryPrefix() {
        String key = StorageKeys.buildPending(ANNOUNCEMENT, USER_ID, "cat.jpg");

        assertThat(key).startsWith("tmp/announcement/" + USER_ID + "/");
        assertThat(StorageKeys.isPending(key)).isTrue();
        assertThat(StorageKeys.pendingBelongsTo(key, ANNOUNCEMENT, USER_ID)).isTrue();
    }

    @Test
    void shouldStripTemporaryPrefixWhenConfirming() {
        String pendingKey = StorageKeys.buildPending(ANNOUNCEMENT, USER_ID, "cat.jpg");

        String confirmedKey = StorageKeys.confirmedKeyOf(pendingKey);

        assertThat(confirmedKey).isEqualTo(pendingKey.substring("tmp/".length()));
        assertThat(StorageKeys.isPending(confirmedKey)).isFalse();
        assertThat(StorageKeys.belongsTo(confirmedKey, ANNOUNCEMENT, USER_ID)).isTrue();
    }

    @Test
    void shouldRejectConfirmedKeyComingFromClient() {
        String confirmedKey = StorageKeys.build(ANNOUNCEMENT, USER_ID, "cat.jpg");

        assertThat(StorageKeys.pendingBelongsTo(confirmedKey, ANNOUNCEMENT, USER_ID)).isFalse();
        assertThat(StorageKeys.pendingBelongsTo(null, ANNOUNCEMENT, USER_ID)).isFalse();
    }

    @Test
    void shouldRejectPendingKeyOfAnotherUserOrContext() {
        assertThat(StorageKeys.pendingBelongsTo(
                StorageKeys.buildPending(ANNOUNCEMENT, UUID.randomUUID(), "cat.jpg"), ANNOUNCEMENT, USER_ID)).isFalse();
        assertThat(StorageKeys.pendingBelongsTo(
                StorageKeys.buildPending(EVENT, USER_ID, "cat.jpg"), ANNOUNCEMENT, USER_ID)).isFalse();
    }

    @Test
    void shouldRejectKeysWithExtraSegmentsOrTraversal() {
        assertThat(StorageKeys.belongsTo("announcement/" + USER_ID + "/nested/cat.jpg", ANNOUNCEMENT, USER_ID)).isFalse();
        assertThat(StorageKeys.belongsTo("announcement/" + USER_ID + "/../other/cat.jpg", ANNOUNCEMENT, USER_ID)).isFalse();
        assertThat(StorageKeys.belongsTo("announcement/" + USER_ID + "/", ANNOUNCEMENT, USER_ID)).isFalse();
        assertThat(StorageKeys.belongsTo(null, ANNOUNCEMENT, USER_ID)).isFalse();
    }
}
