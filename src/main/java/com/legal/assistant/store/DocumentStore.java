package com.legal.assistant.store;

import com.legal.assistant.models.DocumentRecord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DocumentStore {

    private final DocumentRecordRepository repository;

    public DocumentStore(DocumentRecordRepository repository) {
        this.repository = repository;
    }

    public void save(DocumentRecord record) {
        repository.save(record);
    }

    public Optional<DocumentRecord> find(String id) {
        return repository.findById(id);
    }

    public List<DocumentRecord> findAll() {
        return repository.findAll();
    }

    public void delete(String id) {
        repository.deleteById(id);
    }
}
