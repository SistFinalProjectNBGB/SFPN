package com.sist.nbgb.controller;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sist.nbgb.dto.OnlineClassFileResponseDTO;
import com.sist.nbgb.dto.OnlineClassLogDTO;
import com.sist.nbgb.dto.OnlineClassLogIdDTO;
import com.sist.nbgb.dto.OnlineClassLogReqDTO;
import com.sist.nbgb.dto.OnlineClassView;
import com.sist.nbgb.dto.OnlinePaymentApproveDto;
import com.sist.nbgb.dto.OnlinePaymentClassListDTO;
import com.sist.nbgb.entity.OnlineClass;
import com.sist.nbgb.entity.OnlineClassFile;
import com.sist.nbgb.entity.OnlineClassLog;
import com.sist.nbgb.entity.User;
import com.sist.nbgb.enums.Role;
import com.sist.nbgb.enums.Status;
import com.sist.nbgb.repository.OnlineClassLogRepository;
import com.sist.nbgb.repository.UserRepository;
import com.sist.nbgb.service.OnlineClassPlayService;
import com.sist.nbgb.service.OnlineClassService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Controller
public class OnlineClassPlayController 
{
	private final Logger logger = LoggerFactory.getLogger(OnlineClassPlayController.class);
	
	private final OnlineClassService onlineClassService;
	private final OnlineClassPlayService onlineClassPlayService;
	private final UserRepository userRepository;
	private final OnlineClassLogRepository onlineClassLogRepository;

	//온라인 강의 재생 목록
	@GetMapping("/onlinePlayList")
	public String onlinePlayList(Model model, Principal principal, @RequestParam(value="classId", required=false) Long onlineClassId,
			@RequestParam(value="orderId", required=false) String partnerOrderId) throws IOException{
		
		logger.info("[OnlineClassPlayController] onlinePlayList");
		User user = userRepository.findFirstByUserId(principal.getName());
		if(user.getAuthority().equals(Role.ROLE_USER)) {
			//강의 조회
			OnlinePaymentClassListDTO onlineClass = onlineClassPlayService.userLectureInfo(partnerOrderId, user.getUserId(), onlineClassId);
			//강의 재생목록 조회
			List<OnlineClassFileResponseDTO> classList = onlineClassPlayService.selectClassList(onlineClassId)
					.stream().map(OnlineClassFileResponseDTO::new)
					.collect(Collectors.toList());
			List<OnlineClassLog> userList = onlineClassPlayService.selectClassLog(user, onlineClassId);
			
			Map<Integer, String> map = new HashMap<>();
			for (OnlineClassLog log : userList) {
				map.put(Integer.parseInt(String.valueOf(log.getOnlineClassFile().getOnlineClassFileId().getOnlineFileId())),
						String.valueOf(Math.round(Double.valueOf(log.getOnlineLogCurr()) /Double.valueOf(log.getOnlineClassFile().getOnlineClassFileId().getOnlineFileLength()) * 100.0)));
			}
			model.addAttribute("onlineClass", onlineClass);
			model.addAttribute("classList", classList);
			model.addAttribute("log", map);
			
			return "/onlineClass/onlineClassPlayList";
		}else {
			return "login/login";
		}
	}
	
	//온라인 강의 재생 페이지
	@GetMapping("/onlinePlay")
	public String onlinePlay(Model model, Principal principal,
							@RequestParam(value="classId", required=false) Long onlineClassId,
							@RequestParam(value="fileId", required=false) Long onlineFileId) throws IOException{
		
		logger.info("[OnlineClassPlayController] onlinePlay");
		logger.info("강의 번호 : " + onlineClassId + ", 강의 회차 : " + onlineFileId);
		User user = userRepository.findFirstByUserId(principal.getName());
		
		//강의 조회
		OnlineClass onlineClass = onlineClassService.findById(onlineClassId);
		//강의 자료 조회
		OnlineClassFile classFile = onlineClassPlayService.selectClass(onlineClassId, onlineFileId);
		String videoFileName = classFile.getOnlineFileName();
		//강의 재생목록 조회
		List<OnlineClassFileResponseDTO> classList = onlineClassPlayService.selectClassList(onlineClassId)
											.stream().map(OnlineClassFileResponseDTO::new)
											.collect(Collectors.toList());
		Long classCnt = onlineClassPlayService.countClass(onlineClassId);
		//강의 로그 조회
		OnlineClassLogIdDTO selectLogId = OnlineClassLogIdDTO.builder()
				.userId(user.getUserId()).onlineClassId(Long.valueOf(onlineClassId)).onlineFileId(Long.valueOf(onlineFileId)).build();
		if(onlineClassLogRepository.findByOnlineClassLogId(selectLogId.toEntity()) != null) {
			OnlineClassLog searchLog = onlineClassLogRepository.findByOnlineClassLogId(selectLogId.toEntity());
			model.addAttribute("index", searchLog.getOnlineLogCurr());
		}else {
			model.addAttribute("index", 0);
		}
		
		model.addAttribute("onlineClass", new OnlineClassView(onlineClass));
		model.addAttribute("classList", classList);
		model.addAttribute("classFile", new OnlineClassFileResponseDTO(classFile));
		model.addAttribute("classCnt", classCnt);
		model.addAttribute("videoFileName", videoFileName);
		
		return "/onlineClass/onlineClassPlay";
	}
	
	//온라인 강의 로그 추가(수강시작)
	@ResponseBody
	@PostMapping("/onlinePlay")
	public ResponseEntity<OnlineClassLogIdDTO> onlinePlayLog(Principal principal, @RequestParam(value="onlineClassId", required=false) String onlineClassId,
			@RequestParam(value="onlineFileId", required=false) String onlineFileId, @RequestParam(value="onlineLogCurr", required=false) String onlineLogCurr) throws IOException{
		
		logger.info("[OnlineClassPlayController] onlinePlayLog");
		User user = userRepository.findFirstByUserId(principal.getName());
		
		OnlineClassFile classFile = onlineClassPlayService.selectClass(Long.valueOf(onlineClassId), Long.valueOf(onlineFileId));
		OnlineClassLogIdDTO firstLogId = OnlineClassLogIdDTO.builder()
				.userId(user.getUserId()).onlineClassId(Long.valueOf(onlineClassId)).onlineFileId(Long.valueOf(onlineFileId)).build();
			
		OnlineClassLogDTO firstLog = null;
		if(onlineClassLogRepository.findByOnlineClassLogId(firstLogId.toEntity()) == null) {
			firstLog = OnlineClassLogDTO.builder()
					.onlineClassLogId(firstLogId.toEntity())
					.userId(user)
					.onlineClassFile(classFile)
					.onlineLogCurr(Long.valueOf(0))
					.status(Status.N)
					.build();
			onlineClassPlayService.firstLogSave(firstLog);
			onlineClassPlayService.userPayInfoUpdate(onlineClassId, user.getUserId());
		}else {
			OnlinePaymentApproveDto payInfo = new OnlinePaymentApproveDto(onlineClassPlayService.userPayInfo(onlineClassId, user.getUserId()));
			OnlineClassLog logInfo = onlineClassPlayService.userLogInfo(user, Long.valueOf(onlineClassId));
			if(payInfo.getApprovedAt().isAfter(logInfo.getOnlineLogDate())) {
				onlineClassPlayService.userPayInfoUpdate(onlineClassId, user.getUserId());
			}
		}
		
		logger.info("======강의 로그 : 아이디 = " + user.getUserId() + ", 강의번호 = " + onlineClassId + ", 강의회차 = " + onlineFileId);
		logger.info("======강의 로그 상세 : 영상 총 길이 = " + classFile.getOnlineClassFileId().getOnlineFileLength() + ", 현재 시간 = " + LocalDateTime.now());
		
		return ResponseEntity.ok().body(firstLogId);
	}
	
	//온라인 강의 로그 업데이트
	@ResponseBody
	@PutMapping("/onlinePlay")
	public ResponseEntity<OnlineClassLogReqDTO> onlineUpdateLog(Principal principal, @RequestParam(value="onlineClassId", required=false) String onlineClassId,
			@RequestParam(value="onlineFileId", required=false) String onlineFileId, @RequestParam(value="onlineLogCurr", required=false) String onlineLogCurr){
		
		logger.info("[OnlineClassPlayController] onlineUpdateLog");
		//회원조회
		User user = userRepository.findFirstByUserId(principal.getName());
				
		OnlineClassFile classFile = onlineClassPlayService.selectClass(Long.valueOf(onlineClassId), Long.valueOf(onlineFileId));
		
		OnlineClassLogIdDTO updateLogId = OnlineClassLogIdDTO.builder()
				.userId(user.getUserId()).onlineClassId(Long.valueOf(onlineClassId)).onlineFileId(Long.valueOf(onlineFileId)).build();
		
		OnlineClassLogReqDTO updateLog = null;
		long goal = classFile.getOnlineClassFileId().getOnlineFileLength();
		int percent = (int)Math.round(Double.valueOf(onlineLogCurr) /Double.valueOf(goal) * 100.0);
		if(percent < 95) {
				updateLog = new OnlineClassLogReqDTO(Long.valueOf(onlineLogCurr), Status.N);
				onlineClassPlayService.logUpdate(updateLogId, updateLog);
		}else {
			updateLog = new OnlineClassLogReqDTO(Long.valueOf(onlineLogCurr), Status.Y);
			onlineClassPlayService.logUpdate(updateLogId, updateLog);
		}
		
		logger.info("======강의 로그 : 아이디 = " + user.getUserId() + ", 강의번호 = " + onlineClassId + ", 강의회차 = " + onlineFileId);
		logger.info("======강의 로그 상세 : 영상 총 길이 = " + classFile.getOnlineClassFileId().getOnlineFileLength() + ", 현재 시간 = " + LocalDateTime.now());
		return ResponseEntity.ok().body(updateLog);
	}
}
