package be.vdab.osrsplugin.groupinventory.service;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class GroupInventoryPersistence {
    private final Path storagePath;

    GroupInventoryPersistence(@Value("${groupinventory.persistence.path:data/group-inventory-state.bin}") String storagePath) {
        this.storagePath = Path.of(storagePath);
    }

    LinkedHashMap<String, StoredGroupState> load() {
        if (!Files.exists(storagePath)) {
            return new LinkedHashMap<>();
        }

        try {
            PersistedState persistedState;
            try (var inputStream = new ObjectInputStream(Files.newInputStream(storagePath))) {
                persistedState = (PersistedState) inputStream.readObject();
            }
            if (persistedState == null || persistedState.groups() == null) {
                return new LinkedHashMap<>();
            }
            return new LinkedHashMap<>(persistedState.groups());
        } catch (IOException | ClassNotFoundException | ClassCastException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to load persisted group state", exception);
        }
    }

    void save(Map<String, StoredGroupState> groups) {
        try {
            var parent = storagePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            var temporaryFile = storagePath.resolveSibling(storagePath.getFileName() + ".tmp");
            try (var outputStream = new ObjectOutputStream(Files.newOutputStream(temporaryFile))) {
                outputStream.writeObject(new PersistedState(new LinkedHashMap<>(groups)));
            }
            moveIntoPlace(temporaryFile);
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

    record PersistedState(LinkedHashMap<String, StoredGroupState> groups) implements Serializable {
    }

    record StoredGroupState(
            String groupCode,
            String groupName,
            Instant createdAt,
            LinkedHashMap<String, StoredMemberInventory> members,
            LinkedHashMap<String, StoredNamedQuantity> targetItems,
            LinkedHashMap<String, StoredNamedQuantity> manualAdjustments
    ) implements Serializable {
    }

    record StoredMemberInventory(
            String memberName,
            Instant updatedAt,
            LinkedHashMap<String, StoredNamedQuantity> items
    ) implements Serializable {
    }

    record StoredNamedQuantity(String name, int quantity) implements Serializable {
    }
}
