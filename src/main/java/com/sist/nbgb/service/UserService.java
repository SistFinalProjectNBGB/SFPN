package com.sist.nbgb.service;

import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sist.nbgb.dto.AttributeDto;
import com.sist.nbgb.dto.ClassLikeDTO;
import com.sist.nbgb.dto.KakaoPaymentCancelDto;
import com.sist.nbgb.dto.OfflinePaymentCancelDto;
import com.sist.nbgb.dto.OnlinePaymentCancelDto;
import com.sist.nbgb.dto.ReferenceDto2;
import com.sist.nbgb.dto.UserIdCheckDto;
import com.sist.nbgb.dto.UserInfoDto;
import com.sist.nbgb.entity.ClassLike;
import com.sist.nbgb.entity.OfflinePaymentApprove;
import com.sist.nbgb.entity.OfflinePaymentCancel;
import com.sist.nbgb.entity.OnlinePaymentApprove;
import com.sist.nbgb.entity.OnlinePaymentCancel;
import com.sist.nbgb.entity.Reference;
import com.sist.nbgb.entity.User;
import com.sist.nbgb.enums.Role;
import com.sist.nbgb.enums.Status;
import com.sist.nbgb.repository.ClassLikeRepository;
import com.sist.nbgb.repository.OfflinePaymentApproveRepository;
import com.sist.nbgb.repository.OfflinePaymentCancelRepository;
import com.sist.nbgb.repository.OfflineRepository;
import com.sist.nbgb.repository.OnlineClassRepository;
import com.sist.nbgb.repository.OnlinePaymentApproveRepository;
import com.sist.nbgb.repository.OnlinePaymentCancelRepository;
import com.sist.nbgb.repository.ReferenceAnswerRepository;
import com.sist.nbgb.repository.ReferenceRepository;
import com.sist.nbgb.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService 
{
	private final UserRepository userRepository;
	private final OnlinePaymentApproveRepository onlinePaymentApproveRepository;
	private final OfflinePaymentApproveRepository offlinePaymentApproveRepository;
	private final OnlinePaymentCancelRepository onlinePaymentCancelRepository;
	private final OfflinePaymentCancelRepository offlinePaymentCancelRepository;
	private final ClassLikeRepository classLikeRepository;
	private final OnlineClassRepository onlineClassRepository;
	private final OfflineRepository offlineClassRepository;
	
	private final ReferenceRepository referenceRepository;
	private final ReferenceAnswerRepository referenceAnswerRepository;
	
	private final PasswordEncoder passwordEncoder;
	
	
	public UserIdCheckDto findByUserId(UserIdCheckDto userIdCheckDto)
	{
		return userRepository.findByUserId(userIdCheckDto.getUserId())
				.map(UserIdCheckDto :: of)
				.orElseThrow(() -> new RuntimeException("로그인 유저 정보가 없습니다."));
	}
	
	public User findUserById(String userId)
	{
		return userRepository.findById(userId)
				.orElseThrow(() -> new RuntimeException("로그인 유저 정보가 없습니다."));
	}
	
	public UserInfoDto findByUserId(String userId)
	{
		return UserInfoDto.infoUser(userRepository.findAllByUserId(userId));
	}
	
	public AttributeDto findUserAttribute(String userId)
	{
		return AttributeDto.userAttribute(userRepository.findUserNicknameAndUserProviderByUserId(userId));
	}

	@Transactional
	public Object changeUserPassword(String userId, String userPassword)
	{
		Optional<User> user = userRepository.findByUserId(userId);
		
		try 
		{
			user.ifPresent(value -> value.setUserPassword(passwordEncoder.encode(userPassword)));
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return 200;
	}
	
	@Transactional
	public Object changeUserNickname(String userId, String userNickname, HttpServletRequest httpServletRequest)
	{
		Optional<User> user = userRepository.findByUserId(userId);
		
		try 
		{
			user.ifPresent(value -> value.setUserNickname(userNickname));
			
			AttributeDto attributeDto = findUserAttribute(userId);
			
			HttpSession session = httpServletRequest.getSession(true);
			session.setAttribute("attributeDto", attributeDto);
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		return 200;
	}
	
	@Transactional
	public Object changeUserEmail(String userId, String userEmail) 
	{
		Optional<User> user = userRepository.findByUserId(userId);
		
		try 
		{
			user.ifPresent(value -> value.setUserEmail(userEmail));
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return 200;
	}
	
	
	@Transactional
	public Object changeUserPhone(String userId, String userPhone) 
	{
		Optional<User> user = userRepository.findByUserId(userId);
		
		try 
		{
			user.ifPresent(value -> value.setUserPhone(userPhone));
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		return 200;
	}
	
	@Transactional
	public Object signoutUser(String userId)
	{
		Optional<User> user = userRepository.findByUserId(userId);
		
		try 
		{
			user.ifPresent(value -> value.setAuthority(Role.ROLE_RESIGN));
		} 
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		
		return 200;
	}
	
	
	public List<OnlinePaymentApprove> userOnlineApproveFindAll(String userId)
	{
		return onlinePaymentApproveRepository.findAllByPartnerUserIdOrderByApprovedAtDesc(userId);
	}
	
	public List<OfflinePaymentApprove> userOfflineApproveFindAll(String userId)
	{
		return offlinePaymentApproveRepository.findAllByPartnerUserIdOrderByApprovedAtDesc(userId);
	}
	
	public List<OnlinePaymentCancel> userOnlineCancelFindAll(String userId)
	{
		return onlinePaymentCancelRepository.findAllByPartnerUserIdOrderByCanceledAtDesc(userId);
	}
	
	public List<OfflinePaymentCancel> userOfflineCancelFindAll(String userId)
	{
		return offlinePaymentCancelRepository.findAllByPartnerUserIdOrderByCanceledAtDesc(userId);
	}

	public KakaoPaymentCancelDto userOnlineApproveFind(String partnerOrderId)
	{
		return  onlinePaymentApproveRepository.findByPartnerOrderId(partnerOrderId)
				.map(KakaoPaymentCancelDto :: onlinePayment)
				.orElse(null);
	}
	
	public KakaoPaymentCancelDto userOfflineApproveFind(String partnerOrderId)
	{
		return offlinePaymentApproveRepository.findByPartnerOrderId(partnerOrderId)
				.map(KakaoPaymentCancelDto :: offlinePayment)
				.orElse(null);
	}

	@Transactional
	public OnlinePaymentCancelDto userOnlineCancelInsert(OnlinePaymentCancelDto onlinePaymentCancelDto)
	{
		OnlinePaymentCancel onlinePaymentCancel = OnlinePaymentCancel
				.builder()
				.cid(onlinePaymentCancelDto.getCid())
				.tid(onlinePaymentCancelDto.getTid())
				.itemCode(onlinePaymentCancelDto.getItemCode())
				.itemName(onlinePaymentCancelDto.getItemName())
				.partnerOrderId(onlinePaymentCancelDto.getPartnerOrderId())
				.partnerUserId(onlinePaymentCancelDto.getPartnerUserId())
				.cancelTotalAmount(onlinePaymentCancelDto.getCancelTotalAmount())
				.cancelTaxFreeAmount(onlinePaymentCancelDto.getCancelTaxFreeAmount())
				.point(onlinePaymentCancelDto.getPoint())
				.canceledAt(onlinePaymentCancelDto.getCanceledAt().minusHours(9))
				.build();
		
		Long amount = onlinePaymentCancelDto.getCancelTotalAmount();
		Long plusPoint = amount / 100;
		
		if(onlinePaymentCancel.getPoint() > 0)
		{
			Optional<User> user = userRepository.findByUserId(onlinePaymentCancelDto.getPartnerUserId());
			user.ifPresent(value -> value.setUserPoint(value.getUserPoint() + onlinePaymentCancelDto.getPoint() - plusPoint));	
		}
		else
		{
			Optional<User> user = userRepository.findByUserId(onlinePaymentCancelDto.getPartnerUserId());
			user.ifPresent(value -> value.setUserPoint(value.getUserPoint() - plusPoint));	
		}
		
		Optional<OnlinePaymentApprove> onlinePaymentApprove = onlinePaymentApproveRepository.findByPartnerOrderId(onlinePaymentCancelDto.getPartnerOrderId());
		onlinePaymentApprove.ifPresent(value -> value.setStatus(Status.C));
				 	
		return onlinePaymentCancelDto.onlineCancel(onlinePaymentCancelRepository.save(onlinePaymentCancel));  
	}
	
	@Transactional
	public OfflinePaymentCancelDto userOfflineCancelInsert(OfflinePaymentCancelDto offlinePaymentCancelDto)
	{
		OfflinePaymentCancel offlinePaymentCancel = OfflinePaymentCancel
				.builder()
				.cid(offlinePaymentCancelDto.getCid())
				.tid(offlinePaymentCancelDto.getTid())
				.itemCode(offlinePaymentCancelDto.getItemCode())
				.itemName(offlinePaymentCancelDto.getItemName())
				.partnerOrderId(offlinePaymentCancelDto.getPartnerOrderId())
				.partnerUserId(offlinePaymentCancelDto.getPartnerUserId())
				.bookingDate(offlinePaymentCancelDto.getBookingDate())
				.bookingTime(offlinePaymentCancelDto.getBookingTime())
				.cancelTotalAmount(offlinePaymentCancelDto.getCancelTotalAmount())
				.cancelTaxFreeAmount(offlinePaymentCancelDto.getCancelTaxFreeAmount())
				.point(offlinePaymentCancelDto.getPoint())
				.canceledAt(offlinePaymentCancelDto.getCanceledAt().minusHours(9))
				.build();
		
		Long amount = offlinePaymentCancelDto.getCancelTotalAmount();
		Long plusPoint = amount / 100;
		
		if(offlinePaymentCancel.getPoint() > 0)
		{
			Optional<User> user = userRepository.findByUserId(offlinePaymentCancelDto.getPartnerUserId());
			user.ifPresent(value -> value.setUserPoint(value.getUserPoint() + offlinePaymentCancelDto.getPoint() - plusPoint));	
		}
		else
		{
			Optional<User> user = userRepository.findByUserId(offlinePaymentCancelDto.getPartnerUserId());
			user.ifPresent(value -> value.setUserPoint(value.getUserPoint() - plusPoint));	
		}
		
		Optional<OfflinePaymentApprove> offlinePaymentApprove = offlinePaymentApproveRepository.findByPartnerOrderId(offlinePaymentCancelDto.getPartnerOrderId());
		offlinePaymentApprove.ifPresent(value -> value.setStatus(Status.C));
				 	
		return offlinePaymentCancelDto.offlineCancel(offlinePaymentCancelRepository.save(offlinePaymentCancel));  
	}
	
	public List<ClassLike> classLike(String userId, String classIden)
	{
		return classLikeRepository.findAllByClassId_userIdAndClassId_classIdenOrderByClassId_classIdDesc(userId, classIden);
	}
	
	public void onlineClassTitle(List<ClassLikeDTO> classLikeDto)
	{
		for(int i = 0; i < classLikeDto.size(); i++)
		{
			classLikeDto.get(i).setOnlineClassTitle(onlineClassRepository.findOnlineClassTitleByOnlineClassId(classLikeDto.get(i).getClassId())); 
		}
	}
	
	@Transactional
	public Object onlineClassLikeDelete(ClassLikeDTO classLikekDto)
	{
		try 
		{
			classLikeRepository.deleteByClassId_classIdAndClassId_classIdenAndClassId_userId(classLikekDto.getClassId(), classLikekDto.getClassIden(), classLikekDto.getUserId());
		} 
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		return 200;
	}
	
	public void offlineClassTitle(List<ClassLikeDTO> classLikeDto)
	{
		for(int i = 0; i < classLikeDto.size(); i++)
		{
			classLikeDto.get(i).setOfflineClassTitle(offlineClassRepository.findOfflineClassTitleByOfflineClassId(classLikeDto.get(i).getClassId()));
		}
	}
	
	@Transactional
	public Object offlineClassLikeDelete(ClassLikeDTO classLikekDto)
	{
		try 
		{
			classLikeRepository.deleteByClassId_classIdAndClassId_classIdenAndClassId_userId(classLikekDto.getClassId(), classLikekDto.getClassIden(), classLikekDto.getUserId());
		} 
		catch (Exception e) 
		{
			throw new RuntimeException(e);
		}
		return 200;
	}
	
	public List<Reference> userReferenceList(String userId)
	{
		return referenceRepository.findAllByUserId_userIdOrderByRefIdDesc(userId);
	}
	
	public void referenceAnswerList(List<ReferenceDto2> referenceDto)
	{
		for(int i = 0; i < referenceDto.size(); i++)
		{
			referenceDto.get(i).setReferenceAnswer(referenceAnswerRepository.findByRefId(referenceDto.get(i).getRefId().getRefId()));
		}
	}
}
