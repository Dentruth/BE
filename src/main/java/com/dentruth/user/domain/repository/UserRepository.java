package com.dentruth.user.domain.repository;

import com.dentruth.user.domain.entity.User;
import com.dentruth.user.domain.entity.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndStatusIn(String email, List<UserStatus> statuses);

    Optional<User> findByIdAndStatusIn(UUID id, List<UserStatus> statuses);

}
