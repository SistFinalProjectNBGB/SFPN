package com.sist.nbgb.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.sist.nbgb.entity.Reference;
import java.util.List;

@Transactional
@Repository
public interface ReferenceRepository extends JpaRepository<Reference, Long> {
    List<Reference> findAllByOrderByRefRegdateDesc();
    
    List<Reference> findByRefTitleContainingOrRefContentContainingOrUserId_UserIdContaining(String title, String content, String userId);
    
    //상세 페이지(View)
    @Query("SELECT a, b.userNickname " +
		       "FROM Reference a, User b " +
		       "WHERE a.userId = b.userId " +
		       "AND a.refId = :refId ")
	List<Reference> findByView(@Param("refId") Long refId);
    
    List<Reference> findAllByUserId_userIdOrderByRefIdDesc(String userId);
}