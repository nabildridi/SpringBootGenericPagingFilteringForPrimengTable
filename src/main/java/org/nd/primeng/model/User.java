package org.nd.primeng.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "appusers")
public class User {
	
    @Id    
	@GeneratedValue
	private long id;
    
    @Column()
    private String username;    
    
    @Column()
    private String firstname;    
    
    @Column()
    private String lastname;

    @Column()
    private String email;

    @Column()
    private LocalDateTime accessdate;

    @Column()
    private Boolean modified;
    
    @Column()
    private Float account;
    
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getAccessdate() {
		return accessdate;
	}

	public void setAccessdate(LocalDateTime accessdate) {
		this.accessdate = accessdate;
	}

	public Boolean getModified() {
		return modified;
	}

	public void setModified(Boolean modified) {
		this.modified = modified;
	}

	public Float getAccount() {
		return account;
	}

	public void setAccount(Float account) {
		this.account = account;
	}	
}
