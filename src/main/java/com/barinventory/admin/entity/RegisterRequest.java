package com.barinventory.admin.entity;

public record RegisterRequest(String name, String email, String password, Role role,Long barId) {}