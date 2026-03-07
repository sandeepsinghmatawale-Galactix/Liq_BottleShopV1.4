package com.barinventory.admin.entity;

public record AuthResponse(
	    String token,
	    String role,
	    Long barId,    // null for ADMIN
	    String name
	) {}