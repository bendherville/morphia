package dev.morphia.test.models;

import java.util.List;
import java.util.Objects;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Field;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Index;
import dev.morphia.annotations.Indexes;
import dev.morphia.annotations.Property;
import dev.morphia.mapping.IndexType;
import dev.morphia.mapping.experimental.MorphiaReference;

import org.bson.types.ObjectId;

import static java.lang.String.format;
import static java.util.Arrays.asList;

@Entity(value = "books", useDiscriminator = false)
@Indexes(@Index(fields = @Field(value = "$**", type = IndexType.TEXT)))
public final class Book {
    @Id
    public ObjectId id;
    @Property("name")
    public String title;
    public MorphiaReference<Author> author;
    public String authorString;
    public Integer copies;
    public List<String> tags;

    public Book() {
    }

    public Book(String title, String authorString) {
        this.title = title;
        this.authorString = authorString;
    }

    public Book(String title, MorphiaReference<Author> author) {
        this.title = title;
        this.author = author;
    }

    public Book(String title, MorphiaReference<Author> author, Integer copies, String... tags) {
        this.title = title;
        this.author = author;
        this.copies = copies;
        this.tags = asList(tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, author, copies, tags);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Book)) {
            return false;
        }
        Book book = (Book) o;
        return Objects.equals(id, book.id) && Objects.equals(title, book.title) &&
                Objects.equals(author, book.author) && Objects.equals(copies, book.copies) &&
                Objects.equals(tags, book.tags);
    }

    @Override
    public String toString() {
        return format("Book{title='%s', author='%s', copies=%d, tags=%s}", title, author, copies, tags);
    }
}
