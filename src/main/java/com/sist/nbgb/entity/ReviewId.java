package com.sist.nbgb.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
@EqualsAndHashCode
@Builder
public class ReviewId implements Serializable
{
	private static final long serialVersionUID = 1L;

	private Long reviewId;

}