package com.legal.assistant.store;

import com.legal.assistant.models.DocumentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRecordRepository extends JpaRepository<DocumentRecord, String> {
}
