package com.sist.nbgb.entity;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name="NBGB_REFERENCE_ANSWER")
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Builder
public class ReferenceAnswer
{
	@Id
	@Comment("문의 게시물 번호")   
	private Long refId;
	
	@Lob
	@Comment("문의 답변 내용")
	private String refAnswerContent;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ADMIN_ID")
	@Comment("문의 답변 작성자")
	private Admin adminId;
	
	@CreatedDate
	@Comment("문의 답변 등록일")
	private LocalDateTime refAnswerRegdate;
}