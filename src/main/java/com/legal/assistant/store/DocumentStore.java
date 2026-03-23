package com.legal.assistant.store;

import com.legal.assistant.models.DocumentRecord;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DocumentStore {

    private final Map<String, DocumentRecord> store = new HashMap<>();

    public void save(DocumentRecord record) {
        store.put(record.getId(), record);
    }

    public Optional<DocumentRecord> find(String id) {
        return Optional.ofNullable(store.get(id));
    }
}