package com.domisa.domisa_backend.domain.user.repository;

import com.domisa.domisa_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
