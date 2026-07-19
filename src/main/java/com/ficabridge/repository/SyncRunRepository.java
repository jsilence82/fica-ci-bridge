package com.ficabridge.repository;

import com.ficabridge.model.entity.SyncRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncRunRepository extends JpaRepository<SyncRunEntity, Long> {
}
