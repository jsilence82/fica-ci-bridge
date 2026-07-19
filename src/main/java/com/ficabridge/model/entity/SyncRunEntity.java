package com.ficabridge.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * One row per {@code DocumentSyncScheduler} run — interview/demo evidence today,
 * a Prometheus source (staleness, failure rate) later.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "sync_run")
public class SyncRunEntity {

    public enum Status {
        RUNNING, COMPLETED, FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Integer documentsChecked;

    private Integer documentsChanged;

    private String failureReason;

    public static SyncRunEntity started() {
        SyncRunEntity run = new SyncRunEntity();
        run.startedAt = Instant.now();
        run.status = Status.RUNNING;
        return run;
    }

    public void completed(int documentsChecked, int documentsChanged) {
        this.completedAt = Instant.now();
        this.status = Status.COMPLETED;
        this.documentsChecked = documentsChecked;
        this.documentsChanged = documentsChanged;
    }

    public void failed(String failureReason) {
        this.completedAt = Instant.now();
        this.status = Status.FAILED;
        this.failureReason = failureReason;
    }
}
