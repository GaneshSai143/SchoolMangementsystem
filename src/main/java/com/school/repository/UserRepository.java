package com.school.repository;

import com.school.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import com.school.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findAllByRole(UserRole role);
    List<User> findAllByRoleAndSchoolId(UserRole role, Long schoolId);
}