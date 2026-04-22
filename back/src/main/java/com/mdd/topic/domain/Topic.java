package com.mdd.topic.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.Locale;
import java.util.UUID;

/**
 * Topic that groups articles and can be followed by users.
 */
@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String slug;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column
    private String description;

    protected Topic() {
    }

    /**
     * Creates a topic and derives its stable slug from the display name.
     */
    public Topic(String name, String description) {
        this.slug = slugify(name);
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Converts a display name into the slug stored in the database.
     */
    private static String slugify(String value) {
        return value == null
                ? null
                : value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof Topic topic)) {
            return false;
        }
        // Generated ids are null before persistence, so transient entities must not compare equal.
        return id != null && Objects.equals(id, topic.id);
    }

    @Override
    public int hashCode() {
        // Keep the hash stable while Hibernate assigns the generated id.
        return getClass().hashCode();
    }
}
