package org.example.tasktracker.model;

import java.util.Objects;
import java.util.Set;

public class Role {

    private static long nextId = 1; // counter for auto-generated IDs
    private Long id;
    private String name;
    private Set<User> users;


    public Role(String name) {
        this.id = nextId++;   // auto-increment ID
        this.name = name;
    }



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    // --- toString() ---
    @Override
    public String toString() {
        return name; // role name only
    }

    // --- equals() and hashCode() → based on id ---
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Role)) return false;
        Role role = (Role) o;
        return Objects.equals(id, role.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

}
