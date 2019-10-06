package org.nd.primeng.model;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;


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
    private LocalDateTime modifdate;
    
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

	public LocalDateTime getModifdate() {
		return modifdate;
	}

	public void setModifdate(LocalDateTime modifdate) {
		this.modifdate = modifdate;
	}

	



}
