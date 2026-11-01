package com.editshare;

import com.editshare.dto.CreateClipRequest;
import com.editshare.dto.UpdateClipRequest;
import com.editshare.entity.EditMode;
import com.editshare.entity.ExpirationDuration;
import com.editshare.repository.ClipRepository;
import com.editshare.repository.ClipVersionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ClipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClipRepository clipRepository;

    @Autowired
    private ClipVersionRepository clipVersionRepository;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        clipVersionRepository.deleteAll();
        clipRepository.deleteAll();
    }

    @Test
    void testAllFourExpirationOptionsAreSupported() throws Exception {
        for (ExpirationDuration duration : ExpirationDuration.values()) {
            CreateClipRequest req = new CreateClipRequest();
            req.setTitle("Expiry Test: " + duration);
            req.setContent("Content for " + duration);
            req.setLanguage("java");
            req.setEditMode(EditMode.PRIVATE);
            req.setExpiration(duration);

            MvcResult result = mockMvc.perform(post("/api/v1/clips")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                    .andReturn();

            JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
            String clipId = root.path("data").path("id").asText();
            assertThat(clipId).isNotBlank();
        }
    }

    @Test
    void testMissingExpirationIsRejected() throws Exception {
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("No Expiry Test");
        req.setContent("Content without expiry");
        req.setLanguage("plaintext");
        req.setEditMode(EditMode.PRIVATE);
        req.setExpiration(null); // missing expiration

        mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateGetUpdateWithNewExpirationAndVersionHistoryFlow() throws Exception {
        // 1. Create a Private Clip with ONE_DAY
        CreateClipRequest createReq = new CreateClipRequest();
        createReq.setTitle("Algorithm v1");
        createReq.setContent("int a = 10;");
        createReq.setLanguage("java");
        createReq.setEditMode(EditMode.PRIVATE);
        createReq.setExpiration(ExpirationDuration.ONE_DAY);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.editToken").isNotEmpty())
                .andExpect(jsonPath("$.data.currentVersion").value(1))
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String clipId = root.path("data").path("id").asText();
        String editToken = root.path("data").path("editToken").asText();

        assertThat(clipId).isNotBlank();
        assertThat(editToken).isNotBlank();

        // 2. Fetch Clip publicly (without token)
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("int a = 10;"))
                .andExpect(jsonPath("$.data.canEdit").value(false));

        // 3. Attempt update WITHOUT token on Private Clip -> Expect 403 Forbidden
        UpdateClipRequest unauthorizedUpdate = new UpdateClipRequest();
        unauthorizedUpdate.setTitle("Hacked Title");
        unauthorizedUpdate.setContent("int a = 999;");
        unauthorizedUpdate.setLanguage("java");

        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unauthorizedUpdate)))
                .andExpect(status().isForbidden());

        // 4. Update WITH token AND update expiration to ONE_WEEK -> Expect 200 OK & version 2
        UpdateClipRequest validUpdate = new UpdateClipRequest();
        validUpdate.setTitle("Algorithm v2");
        validUpdate.setContent("int a = 20;");
        validUpdate.setLanguage("java");
        validUpdate.setExpiration(ExpirationDuration.ONE_WEEK);

        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .header("X-Edit-Token", editToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validUpdate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2))
                .andExpect(jsonPath("$.data.content").value("int a = 20;"));

        // 5. Inspect Version History -> should have 2 versions
        mockMvc.perform(get("/api/v1/clips/" + clipId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].versionNumber").value(2))
                .andExpect(jsonPath("$.data[1].versionNumber").value(1));

        // 6. Revert to Version 1 -> Expect new current version = 3 with content of v1
        mockMvc.perform(post("/api/v1/clips/" + clipId + "/versions/1/revert")
                        .header("X-Edit-Token", editToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(3))
                .andExpect(jsonPath("$.data.content").value("int a = 10;"));

        // 7. Verify version history count is now 3
        assertThat(clipVersionRepository.findByClipIdOrderByVersionNumberDesc(clipId)).hasSize(3);

        // 8. Delete Clip -> Both clip and version history must be deleted
        mockMvc.perform(delete("/api/v1/clips/" + clipId)
                        .header("X-Edit-Token", editToken))
                .andExpect(status().isOk());

        // 9. Confirm clip is gone and versions are gone
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isNotFound());
        assertThat(clipVersionRepository.findByClipIdOrderByVersionNumberDesc(clipId)).isEmpty();
    }

    @Test
    void testCollaborativeEditingPermissions() throws Exception {
        // Create collaborative clip with edit password
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("Team Notes");
        req.setContent("Initial notes");
        req.setLanguage("markdown");
        req.setEditMode(EditMode.COLLABORATIVE);
        req.setEditPassword("team-secret-pass");
        req.setExpiration(ExpirationDuration.ONE_DAY);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String clipId = root.path("data").path("id").asText();

        // 1. Without edit password -> should be forbidden
        UpdateClipRequest updateReq = new UpdateClipRequest();
        updateReq.setTitle("Team Notes Updated");
        updateReq.setContent("Updated notes");
        updateReq.setLanguage("markdown");

        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // 2. With wrong edit password -> should be forbidden
        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .header("X-Edit-Password", "wrong-pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isForbidden());

        // 3. With correct edit password -> should succeed
        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .header("X-Edit-Password", "team-secret-pass")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Updated notes"));
    }

    @Test
    void testExpiredClipCannotBeViewedAndIsDeletedWithHistory() throws Exception {
        // Create a clip
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("Expiring Clip");
        req.setContent("Will expire soon");
        req.setLanguage("plaintext");
        req.setEditMode(EditMode.PRIVATE);
        req.setExpiration(ExpirationDuration.TEN_MINUTES);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String clipId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // Manually set expiration in the past in DB to simulate expired clip
        clipRepository.findById(clipId).ifPresent(clip -> {
            clip.setExpiresAt(LocalDateTime.now().minusMinutes(5));
            clipRepository.save(clip);
        });

        // Fetching the expired clip must return 404
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isNotFound());

        // Verify the expired clip and its version history are deleted from MySQL
        assertThat(clipRepository.findById(clipId)).isEmpty();
        assertThat(clipVersionRepository.findByClipIdOrderByVersionNumberDesc(clipId)).isEmpty();
    }

    @Test
    void testDeleteAfterOneViewOption() throws Exception {
        // 1. Create a clip with AFTER_ONE_VIEW expiration
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("Secret Burn Clip");
        req.setContent("This secret message will self-destruct after reading.");
        req.setLanguage("plaintext");
        req.setEditMode(EditMode.PRIVATE);
        req.setExpiration(ExpirationDuration.AFTER_ONE_VIEW);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").isNotEmpty())
                    .andExpect(jsonPath("$.data.deleteAfterOneView").value(true))
                    .andReturn();

        JsonNode root = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String clipId = root.path("data").path("id").asText();
        String editToken = root.path("data").path("editToken").asText();

        // 2. First successful link access (1st view) -> successfully retrieves content & records view
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("This secret message will self-destruct after reading."));

        assertThat(clipRepository.findById(clipId)).isPresent();
        assertThat(clipRepository.findById(clipId).get().getViewCount()).isEqualTo(1);

        // 3. Second access (refresh / reopen) -> returns 404
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isNotFound());

        // 4. Verify records are purged from database
        assertThat(clipRepository.findById(clipId)).isEmpty();
        assertThat(clipVersionRepository.findByClipIdOrderByVersionNumberDesc(clipId)).isEmpty();
    }

    @Test
    void testPasswordProtectedDeleteAfterOneViewDoesNotBurnOnFailedPassword() throws Exception {
        // Create password protected clip with AFTER_ONE_VIEW
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("Locked Burn Clip");
        req.setContent("Secret top confidential text");
        req.setLanguage("plaintext");
        req.setEditMode(EditMode.PRIVATE);
        req.setViewPassword("correct-pass");
        req.setExpiration(ExpirationDuration.AFTER_ONE_VIEW);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String clipId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // 1. Unauthorized attempt (no password) -> 401 Unauthorized, must NOT burn
        mockMvc.perform(get("/api/v1/clips/" + clipId))
                .andExpect(status().isUnauthorized());
        assertThat(clipRepository.findById(clipId)).isPresent();

        // 2. Failed attempt (wrong password) -> 401 Unauthorized, must NOT burn
        mockMvc.perform(get("/api/v1/clips/" + clipId)
                        .header("X-View-Password", "wrong-pass"))
                .andExpect(status().isUnauthorized());
        assertThat(clipRepository.findById(clipId)).isPresent();

        // 3. Successful attempt (correct password) -> 200 OK
        mockMvc.perform(get("/api/v1/clips/" + clipId)
                        .header("X-View-Password", "correct-pass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Secret top confidential text"));

        // 4. Second attempt with correct password -> 404 Not Found (burned!)
        mockMvc.perform(get("/api/v1/clips/" + clipId)
                        .header("X-View-Password", "correct-pass"))
                .andExpect(status().isNotFound());
        assertThat(clipRepository.findById(clipId)).isEmpty();
    }

    @Test
    void testConcurrentCollaboratorVersionConflictDetection() throws Exception {
        // Create collaborative clip
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle("Collab Code");
        req.setContent("Initial shared code");
        req.setLanguage("javascript");
        req.setEditMode(EditMode.COLLABORATIVE);
        req.setExpiration(ExpirationDuration.ONE_DAY);

        MvcResult createResult = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn();

        String clipId = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText();

        // Collaborator 1 saves change based on version 1 -> succeeds
        UpdateClipRequest update1 = new UpdateClipRequest();
        update1.setTitle("Collab Code v2");
        update1.setContent("Code by User A");
        update1.setLanguage("javascript");
        update1.setExpectedVersion(1);

        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentVersion").value(2));

        // Collaborator 2 attempts to save based on stale version 1 -> 409 Conflict
        UpdateClipRequest update2 = new UpdateClipRequest();
        update2.setTitle("Collab Code v2 Overwrite");
        update2.setContent("Conflicting code by User B");
        update2.setLanguage("javascript");
        update2.setExpectedVersion(1); // stale version

        mockMvc.perform(put("/api/v1/clips/" + clipId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("modified by another collaborator")));
    }

    @Test
    void testConcurrent10UsersCreatingClipsSimultaneously() throws Exception {
        int userCount = 10;
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(userCount);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(userCount);
        java.util.Set<String> generatedClipIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

        for (int i = 0; i < userCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    CreateClipRequest req = new CreateClipRequest();
                    req.setTitle("User " + index + " Clip");
                    req.setContent("Content from concurrent user " + index);
                    req.setLanguage("plaintext");
                    req.setEditMode(EditMode.PRIVATE);
                    req.setExpiration(ExpirationDuration.ONE_HOUR);

                    MvcResult result = mockMvc.perform(post("/api/v1/clips")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andExpect(status().isCreated())
                            .andReturn();

                    String id = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asText();
                    generatedClipIds.add(id);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(generatedClipIds).hasSize(userCount);
    }

    @Test
    void testTitleBasedSlugAndDuplicateConflictAndReuseFlow() throws Exception {
        // 1. Create first clip "Python Algorithms"
        CreateClipRequest req1 = new CreateClipRequest();
        req1.setTitle("Python Algorithms");
        req1.setContent("print('Hello Python');");
        req1.setLanguage("python");
        req1.setEditMode(EditMode.PRIVATE);
        req1.setExpiration(ExpirationDuration.ONE_HOUR);

        MvcResult res1 = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("python-algorithms"))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8085/python-algorithms"))
                .andReturn();

        JsonNode root1 = objectMapper.readTree(res1.getResponse().getContentAsString());
        String id1 = root1.path("data").path("id").asText();
        String slug1 = root1.path("data").path("slug").asText();
        String editToken1 = root1.path("data").path("editToken").asText();
        assertThat(slug1).isEqualTo("python-algorithms");

        // 2. Create second clip with same title "Python Algorithms" -> MUST return 409 Conflict
        CreateClipRequest req2 = new CreateClipRequest();
        req2.setTitle("Python Algorithms");
        req2.setContent("print('Duplicate Attempt');");
        req2.setLanguage("python");
        req2.setEditMode(EditMode.PRIVATE);
        req2.setExpiration(ExpirationDuration.ONE_HOUR);

        mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Clip name already exists. Please choose another name."));

        // 3. Delete the first clip manually
        mockMvc.perform(delete("/api/v1/clips/" + id1)
                        .header("X-Edit-Token", editToken1))
                .andExpect(status().isOk());

        // 4. Now create "Python Algorithms" again -> MUST succeed with exact same slug
        MvcResult res3 = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value("python-algorithms"))
                .andReturn();

        // 5. Test special characters title
        CreateClipRequest reqSpecial = new CreateClipRequest();
        reqSpecial.setTitle("My First Project: (2026 Edition)!");
        reqSpecial.setContent("Project details");
        reqSpecial.setLanguage("plaintext");
        reqSpecial.setEditMode(EditMode.PRIVATE);
        reqSpecial.setExpiration(ExpirationDuration.ONE_HOUR);

        mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqSpecial)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("my-first-project-2026-edition"))
                .andExpect(jsonPath("$.data.url").value("http://localhost:8085/my-first-project-2026-edition"));
    }

    @Test
    void testSlugReuseAfterExpiration() throws Exception {
        String testTitle = "Temporary Notes Expiry Test";
        String expectedSlug = "temporary-notes-expiry-test";

        // Create a clip and manually expire it
        CreateClipRequest req = new CreateClipRequest();
        req.setTitle(testTitle);
        req.setContent("Expiring soon content");
        req.setLanguage("plaintext");
        req.setEditMode(EditMode.PRIVATE);
        req.setExpiration(ExpirationDuration.TEN_MINUTES);

        MvcResult res = mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value(expectedSlug))
                .andReturn();

        String clipId = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("id").asText();

        // Set expiresAt to the past to simulate elapsed expiration time
        com.editshare.entity.Clip clip = clipRepository.findById(clipId).orElseThrow();
        clip.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        clipRepository.saveAndFlush(clip);

        // Creating a new clip with the same title should purge the expired one and succeed
        CreateClipRequest reqNew = new CreateClipRequest();
        reqNew.setTitle(testTitle);
        reqNew.setContent("New clip reusing the expired title");
        reqNew.setLanguage("plaintext");
        reqNew.setEditMode(EditMode.PRIVATE);
        reqNew.setExpiration(ExpirationDuration.ONE_DAY);

        mockMvc.perform(post("/api/v1/clips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqNew)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.slug").value(expectedSlug));
    }

    @Test
    void testConcurrentSlugCreationOnlyOneSucceeds() throws Exception {
        String concurrentTitle = "Simultaneous Creation Test";
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(2);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger conflictCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < 2; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    CreateClipRequest req = new CreateClipRequest();
                    req.setTitle(concurrentTitle);
                    req.setContent("Content from worker " + index);
                    req.setLanguage("plaintext");
                    req.setEditMode(EditMode.PRIVATE);
                    req.setExpiration(ExpirationDuration.ONE_HOUR);

                    MvcResult result = mockMvc.perform(post("/api/v1/clips")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(req)))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 201) {
                        successCount.incrementAndGet();
                    } else if (status == 409) {
                        conflictCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(10, java.util.concurrent.TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
    }
}
