package com.pfa.chess;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface UsersRepository extends JpaRepository<Users, Integer> {

	List<Users> findAllByLoginId(String loginId);

	List<Users> findAllByUsername(String username);
}
