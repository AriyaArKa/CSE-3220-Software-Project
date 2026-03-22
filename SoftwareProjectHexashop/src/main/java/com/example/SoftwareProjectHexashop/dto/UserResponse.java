package com.example.SoftwareProjectHexashop.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Set<String> roles;
}
