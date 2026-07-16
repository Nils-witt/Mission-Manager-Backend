package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "log_book_entry")
public class LogBookEntry extends AbstractEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Lob
    @Column(name = "text", nullable = false)
    private String text;

    @Column(name = "sender")
    private String sender;

    @Column(name = "recipient")
    private String recipient;

    @Column(name = "author")
    private String author;

    @OneToMany(orphanRemoval = true)
    private Set<StoredFile> attachments = new LinkedHashSet<>();

}