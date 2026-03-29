package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.time.Instant;

public class UserGetDTO {

    private Long id;
    private String username;
    private Instant createdAt;
    private String token;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getToken(){
        return token;
    }
    
    public void setToken(String token){
        this.token = token; 
    }
    
}
