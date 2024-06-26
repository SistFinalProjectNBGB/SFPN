package com.sist.nbgb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sist.nbgb.entity.OfflinePaymentApprove;

public interface OfflinePaymentApproveRepository extends JpaRepository<OfflinePaymentApprove, String>
{
	List<OfflinePaymentApprove> findAllByPartnerUserIdOrderByApprovedAtDesc(String userId);
	
	int deleteByPartnerOrderId(String orderId);
	
	Optional<OfflinePaymentApprove> findAllByPartnerOrderId(String orderId);
	
	@Modifying
	@Query("UPDATE OfflinePaymentApprove "
			+ "SET cid = :cid "
			+ ", tid = :tid "
			+ ", taxFreeAmount = 0 "
			+ ", approvedAt = SYSDATE "
			+ ", status = 'Y' "
			+ "WHERE partnerOrderId = :orderId")
	int updatePay(@Param("orderId") String orderId, @Param("tid") String tId, @Param("cid") String cid);
	
	@Query("SELECT COUNT(partner_order_id) "
			+ "FROM OfflinePaymentApprove "
			+ "WHERE itemCode = :classId "
			+ "AND bookingDate = :date "
			+ "AND bookingTime = :time ")
	Long countPeople(@Param("classId") String classId, @Param("date") String date, @Param("time") String time);
	
	//시간 찾기
	@Query("SELECT DISTINCT bookingTime "
			+ "FROM OfflinePaymentApprove "
			+ "WHERE itemCode = :offlineClassId "
			+ "AND bookingDate = :date")
	ArrayList<String> timeList(@Param("offlineClassId") String offlineClassId, @Param("date") String date);
	
	List<OfflinePaymentApprove> findByBookingDateAndBookingTimeAndItemCode(String bookingDate, String bookingTime, String itemCode);
	
	Optional<OfflinePaymentApprove> findByPartnerOrderId(String partnerOrderId);
}
