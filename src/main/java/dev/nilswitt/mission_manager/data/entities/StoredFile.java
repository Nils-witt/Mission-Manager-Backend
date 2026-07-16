package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "stored_file")
public class StoredFile extends AbstractEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "filepath", nullable = false, unique = true)
    private String filepath;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

}