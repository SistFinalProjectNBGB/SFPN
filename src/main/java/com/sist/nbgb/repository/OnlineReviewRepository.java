package com.sist.nbgb.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.sist.nbgb.entity.Review;

public interface OnlineReviewRepository extends JpaRepository<Review, Long> {
	


}