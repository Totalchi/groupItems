package be.vdab.osrsplugin.groupinventory.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class GroupInventoryPersistence {
    private static final Logger log = LoggerFactory.getLogger(GroupInventoryPersistence.class);

    private final Path storagePath;
    private final Path legacyBinaryPath;
    private final ObjectMapper objectMapper;

    GroupInventoryPersistence(
            @Value("${groupinventory.persistence.path:data/group-inventory-state.json}") String storagePath,
            ObjectMapper objectMapper
    ) {
        this.storagePath = Path.of(storagePath);
        String legacyPath = storagePath.endsWith(".json")
                ? storagePath.substring(0, storagePath.length() - 5) + ".bin"
                : storagePath + ".bin";
        this.legacyBinaryPath = Path.of(legacyPath);
        this.objectMapper = objectMapper;
    }

    LinkedHashMap<String, StoredGroupState> load() {
        if (Files.exists(storagePath)) {
            return loadJson();
        }
        if (Files.exists(legacyBinaryPath)) {
            log.info("Migrating group state from binary format to JSON");
            return loadLegacyBinary();
        }
        return new LinkedHashMap<>();
    }

    private LinkedHashMap<String, StoredGroupState> loadJson() {
        try {
            PersistedState persistedState = objectMapper.readValue(storagePath.toFile(), PersistedState.class);
            if (persistedState == null || persistedState.groups() == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(persistedState.groups());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load persisted group state", exception);
        }
    }

    private LinkedHashMap<String, StoredGroupState> loadLegacyBinary() {
        try {
            Object raw;
            try (var inputStream = new ObjectInputStream(Files.newInputStream(legacyBinaryPath))) {
                raw = inputStream.readObject();
            }
            if (raw == null) {
                return new LinkedHashMap<>();
            }
            // Round-trip through JSON to convert legacy Serializable records to the new model
            byte[] json = objectMapper.writeValueAsBytes(raw);
            PersistedState persistedState = objectMapper.readValue(json, PersistedState.class);
            if (persistedState == null || persistedState.groups() == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(persistedState.groups());
        } catch (Exception exception) {
            log.warn("Could not migrate legacy binary state, starting fresh: {}", exception.getMessage());
            return new LinkedHashMap<>();
        }
    }

    void save(Map<String, StoredGroupState> groups) {
        try {
            var parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            var temporaryFile = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            try {
                objectMapper.writeValue(temporaryFile.toFile(), new PersistedState(new LinkedHashMap<>(groups)));
                moveIntoPlace(temporaryFile);
            } catch (IOException exception) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                }
                throw exception;
            }
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to persist group state", exception);
        }
    }

    private void moveIntoPlace(Path temporaryFile) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    storagePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporaryFile, storagePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record PersistedState(LinkedHashMap<String, StoredGroupState> groups) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StoredGroupState(
            String groupCode,
            String groupName,
            Instant createdAt,
            LinkedHashMap<String, StoredMemberInventory> members,
            LinkedHashMap<String, StoredNamedQuantity> targetItems,
            LinkedHashMap<String, StoredNamedQuantity> manualAdjustments
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record StoredMemberInventory(
            String memberName,
            Instant updatedAt,
            LinkedHashMap<String, StoredNamedQuantity> items
    ) {
    }

    record StoredNamedQuantity(String name, int quantity) {
    }
}
