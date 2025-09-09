package org.example.tasktracker.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class User {
        private Long id;
        private String name;
        private String email;
        private String password; // encrypted
        private Set<Role> roles;
        private List<Task> tasks;
        private static long nextId = 1;


        public User(String name, String email, String password) {
                this.password = password;
                this.email = email;
                this.name = name;
                this.roles = new HashSet<>();
                this.id = nextId++;
        }

        public static void setNextId(long nextId) {
                User.nextId = nextId;
        }

        // --- Getters and Setters ---
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

        public String getEmail() {
                return email;
        }

        public void setEmail(String email) {
                this.email = email;
        }

        public String getPassword() {
                return password;
        }

        public void setPassword(String password) {
                this.password = password;
        }

        public Set<Role> getRoles() {
                return roles;
        }

        public void setRoles(Set<Role> roles) {
                this.roles = roles;
        }

        public List<Task> getTasks() {
                return tasks;
        }

        public void setTasks(List<Task> tasks) {
                this.tasks = tasks;
        }

        // --- toString() (exclude password) ---
        @Override
        public String toString() {
                return id + " " + name + " " + email;
        }

        // --- equals() and hashCode() based on id ---
        @Override
        public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof User)) return false;
                User user = (User) o;
                return Objects.equals(id, user.id);
        }

        @Override
        public int hashCode() {
                return Objects.hash(id);
        }
}
