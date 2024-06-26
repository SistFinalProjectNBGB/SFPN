package com.sist.nbgb.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.sist.nbgb.dto.ClassLikeDTO;
import com.sist.nbgb.dto.OfflineApproveResponse;
import com.sist.nbgb.dto.OfflineReadyResponse;
import com.sist.nbgb.dto.OnlinePayDto;
import com.sist.nbgb.dto.OnlinePaymentApproveDto;
import com.sist.nbgb.dto.OnlinePostDTO;
import com.sist.nbgb.dto.OnlineReviewLikeDTO;
import com.sist.nbgb.dto.OnlineUpdateDTO;
import com.sist.nbgb.dto.UserInfoDto;
import com.sist.nbgb.entity.ClassId;
import com.sist.nbgb.entity.ClassLike;
import com.sist.nbgb.entity.Instructors;
import com.sist.nbgb.entity.OnlineCategory;
import com.sist.nbgb.entity.OnlineClass;
import com.sist.nbgb.entity.OnlineClassFile;
import com.sist.nbgb.entity.OnlineClassFileId;
import com.sist.nbgb.entity.OnlinePaymentApprove;
import com.sist.nbgb.entity.Review;
import com.sist.nbgb.entity.ReviewComment;
import com.sist.nbgb.entity.User;
import com.sist.nbgb.enums.Status;
import com.sist.nbgb.repository.ClassLikeRepository;
import com.sist.nbgb.repository.InstructorsRepository;
import com.sist.nbgb.repository.OfflineUserRepository;
import com.sist.nbgb.repository.OnlineCategoryRepository;
import com.sist.nbgb.repository.OnlineClassFileRepository;
import com.sist.nbgb.repository.OnlineClassRepository;
import com.sist.nbgb.repository.OnlinePaymentApproveRepository;
import com.sist.nbgb.repository.OnlineReviewCommentRepository;
import com.sist.nbgb.repository.OnlineReviewRepository;
import com.sist.nbgb.repository.ReviewLikeRepository;
import com.sist.nbgb.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class OnlineClassService {
	private final OnlineClassRepository onlineClassRepository;
	private final OnlineCategoryRepository onlineCategoryRepository;
	private final ClassLikeRepository classLikeRepository;
	private final OnlinePaymentApproveRepository onlinePaymentApproveRepository;
	private final OnlineReviewRepository reviewRepository;
	private final OnlineReviewCommentRepository reviewCommentRepository;
	private final UserRepository userRepository;
	private final ReviewLikeRepository reviewLikeRepository;
	private final InstructorsRepository instRepository;
	private final OnlineClassFileRepository fileRepository;
	private final FFmpegManager ffmpegManager;
	private final OfflineUserRepository offUserRepository;
	private final UserService userService;
	
    @Value("${online.video.file.dir}")
    private String fileDir;
	
    static final String cid = "TC0ONETIME";
	static final String admin_Key = "b5da1907f4cf9df4cafd9ebb58dfcf1e";
	private OfflineReadyResponse payReady;
	
	/*온라인클래스 리스트*/
	//카테고리명 조회
	public List<OnlineCategory> categoryFind() {	
		return onlineCategoryRepository.findAll();
	}
	
	//온라인 클래스 리스트
	public List<OnlineClass> findAll(Status onlineClassApprove){		
		return onlineClassRepository.findByOnlineClassApproveOrderByOnlineClassIdDesc(onlineClassApprove);
	}
	
	//카테고리별 조회
	public List<OnlineClass> findCategoryList(long onlineCategoryId, Status onlineClassApprove){		
		return onlineClassRepository.findByOnlineCategoryId_onlineCategoryIdAndOnlineClassApprove(onlineCategoryId, onlineClassApprove);
	}
	
	//검색기능
	public List<OnlineClass> findSearchList(String searchKeyword, Status onlineClassApprove){	
		return onlineClassRepository.findByOnlineClassTitleContainingAndOnlineClassApprove(searchKeyword, onlineClassApprove);
	}
	
	//카테고리 내 검색 
	public List<OnlineClass> findCategorySearchList( String searchKeyword, long onlineCategoryId, Status onlineClassApprove){	
		return onlineClassRepository
				.findByOnlineClassTitleContainingAndOnlineCategoryId_onlineCategoryIdAndOnlineClassApprove(searchKeyword, onlineCategoryId, onlineClassApprove);
	}
	
	//정렬기준 - 인기순(조회순)
	public List<OnlineClass> findByOnlineClassApproveOrderByOnlineClassViews(Status onlineClassApprove){
		return onlineClassRepository.findByOnlineClassApproveOrderByOnlineClassViewsDesc(onlineClassApprove);
	}

	//정렬기준 - 가격 낮은 순
	public List<OnlineClass> findByOnlineClassApproveOrderByOnlineClassPriceAsc(Status onlineClassApprove){
		return onlineClassRepository.findByOnlineClassApproveOrderByOnlineClassPriceAsc(onlineClassApprove);
	}
	
	//정렬기준 - 가격 높은 순
	public List<OnlineClass> findByOnlineClassApproveOrderByOnlineClassPriceDesc(Status onlineClassApprove){
		return onlineClassRepository
				.findByOnlineClassApproveOrderByOnlineClassPriceDesc(onlineClassApprove);
	}
	
	
	
	/*온라인클래스 상세페이지*/
	//view
	public OnlineClass findById(long onlineClassId) {
		return onlineClassRepository.findById(onlineClassId)
			.orElseThrow(() -> new IllegalArgumentException("not found: " + onlineClassId));
	}
	
	//조회수 증가
	@Transactional
	public int updateViews(long onlineClassId) {
		return onlineClassRepository.updateViews(onlineClassId);
	}
		
	//좋아요 수 조회
	public long findLikeCnt(long onlineClassId) {
		return classLikeRepository.countByClassId_classId(onlineClassId);
	}
	
	//내가 좋아요 했는지 확인
	public long findLikeMe(Long classId, String classIden, String userId) {
		return classLikeRepository.countByClassId_classIdAndClassId_classIdenAndClassId_userId(classId, classIden, userId);
	}
	
	//게시글 찜 등록
	@Transactional
	public ClassLikeDTO saveLike(ClassLikeDTO likeDto) {
		OnlineClass tmpClass = onlineClassRepository.findFirstByonlineClassId(likeDto.getClassId());
		ClassId classId = ClassId.builder()
				.classId(tmpClass.getOnlineClassId())
				.classIden("on")
				.build();
		User user = userRepository.findFirstByUserId(likeDto.getUserId()); //아이디 받아오기
		
		ClassLike like = ClassLike.builder()
				.classId(classId)
				.userId(user)
				.build();
		
		classLikeRepository.save(like);
		
		return likeDto;
	}
	
	//게시글 찜 취소
	@Transactional
	public int removeLike(ClassLikeDTO likeDto) {
		if(findLikeMe(likeDto.getClassId(), likeDto.getClassIden(),likeDto.getUserId()) >= 1) {
			return classLikeRepository.deleteByClassId_classIdAndClassId_classIdenAndClassId_userId(likeDto.getClassId(), likeDto.getClassIden(), likeDto.getUserId());	
		}
		
		return 0;
	}	
	
	//결제 날짜
	public List<LocalDateTime> findApproveAt(String itemCode, String partnerUserId) {
		return onlinePaymentApproveRepository.findApproveAt(itemCode, partnerUserId, PageRequest.of(0, 1));		
	}

	//후기 목록 조회
	public List<Review> findOnReview(Long classId, String classIden, Status reviewStatus) {
		return reviewRepository.findByClassIdAndClassIdenAndReviewStatusOrderByReviewRegdateDesc(classId, classIden, reviewStatus);
	}
	
	//후기 개수
	public long findReviewCnt(Long classId, String classIden, Status reviewStatus) {
		return reviewRepository.countByClassIdAndClassIdenAndReviewStatus(classId, classIden, reviewStatus);
	}
	
	//후기 목록 페이징
	public Page<Review> getList(int page, Long classId, String classIden, Status reviewStatus){
		Pageable pageable = PageRequest.of(page, 2, Sort.by(Sort.Direction.DESC, "reviewRegdate"));
		return this.reviewRepository.findAllByClassIdAndClassIdenAndReviewStatus(pageable, classId, classIden, reviewStatus);
	}
	
	//후기 작성자 회원 이미지 여부
	public String getImg(String userId) {
		String img = "N";
		UserInfoDto userInfoDto = userService.findByUserId(userId);
    	
		String path = "C:\\project\\sts4\\SFPN\\src\\main\\resources\\static\\images\\user";
	    String filename = userInfoDto.getUserId() + ".png"; // 기본 파일명
	    String filepath = path + "/" + filename;
		
        File file = new File(filepath);
        
        if(file.exists())
		{
        	img = "Y";
		}
        
        return img;
	}
	
	//별점 평균
	public int starAvg(Long classId) {
		return reviewRepository.starAvg(classId);
	}
	
	//후기 추천 여부
	public long findReviewLikeMe(long reviewId, String userId) {
		return reviewLikeRepository.countByReviewLikeId_reviewIdAndReviewLikeId_userId(reviewId, userId);
	}
	
	//후기 추천 수 증가
	@Transactional
	public int updateReviewLike(long reviewId) {
		return reviewRepository.updateReviewLikeCnt(reviewId);
	}
	
	//후기 추천 갯수
	public int countReviewLike(long reviewId) {
		return reviewRepository.countReviewLike(reviewId);
	}
	
	//후기 추천 등록
	@Transactional
	public OnlineReviewLikeDTO saveReviewLike(OnlineReviewLikeDTO likeDto) {
		log.info("likeDto.getReviewId()" + likeDto.getReviewId());
		
		//User user = userRepository.findFirstByUserId(likeDto.getUserId()); //아이디 받아오기
		
		reviewLikeRepository.insertReviewLike(likeDto.getReviewId(), likeDto.getUserId());
		
		return likeDto;
	}
	
	//후기 댓글 목록
	public List<ReviewComment> findOnlineComment(@Param("onlineClassId") Long onlineClassId){
		return reviewCommentRepository.findOnlineComment(onlineClassId);
	}
	
	//강사 정보
	public Instructors findInst(String userId) {
		Instructors inst = null;
		inst = instRepository.findFirstByInstructorId(userId); //아이디 받아오기
		
		return inst;
	}
	
	//강사 권한 반환
	public boolean hasInstRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream().filter(o -> o.getAuthority().equals("ROLE_INSTRUCTOR")).findAny().isPresent();
    }
	
	public boolean hasAdminRole()
	{
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        return authorities.stream().filter(o -> o.getAuthority().equals("ROLE_ADMIN")).findAny().isPresent();
	}
	
	/* 온라인 클래스 등록 */
	//온라인 클래스 등록
	@Transactional
	public OnlinePostDTO onlinePost(OnlinePostDTO onlinePostDto) {
		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
		Instructors id = instRepository.findFirstByInstructorId(userId); //아이디 받아오기
		//카테고리 id로 카테고리 검색
		OnlineCategory cate = onlineCategoryRepository.findFirstByOnlineCategoryId(onlinePostDto.getOnlineCategoryId());

		OnlineClass onlineClass = OnlineClass.builder()
				.onlineClassTitle(onlinePostDto.getOnlineClassTitle())
				.onlineClassContent(onlinePostDto.getOnlineClassContent())
				.onlineClassRegdate(LocalDateTime.now())
				.onlineCategoryId(cate)
				.instructorId(id)
				.onlineClassPrice(onlinePostDto.getOnlineClassPrice())
				.onlineClassPeriod(onlinePostDto.getOnlineClassPeriod())
				.onlineClassApprove(Status.N)
				.rejection(null)
				.onlineClassViews((long) 0)
				.build();
		
		 // 저장 후에 onlineClassId 값을 가져옵니다.
	    OnlineClass savedOnlineClass = onlineClassRepository.save(onlineClass);
	    Long onlineClassId = savedOnlineClass.getOnlineClassId();

	    // 반환할 DTO에 onlineClassId 값을 설정한 후에 반환합니다.
	    onlinePostDto.setOnlineClassId(onlineClassId);
	    
	    return onlinePostDto;
	}
	
	//동영상 파일 업로드
	 public Long saveFile(MultipartFile files, Long classId, List<String> fileContent) throws IOException {
        if (files.isEmpty()) {
            return null;
        }

        // 원래 파일 이름 추출
        String origName = files.getOriginalFilename();

        // 확장자 추출
        String extension = origName.substring(origName.lastIndexOf("."));
        String origName2 = origName.substring(0, origName.lastIndexOf("."));
        
        OnlineClass id = onlineClassRepository.findFirstByonlineClassId(classId);
        
        Long fileCnt = fileRepository.countByOnlineClassFileId_onlineClassId(classId);

        long len = 180;
        OnlineClassFileId fileId = OnlineClassFileId.builder()
        		.onlineClassId(classId)
        		.onlineFileId(fileCnt+1)
        		.onlineFileLength(len)
        		.build();
        
        // 확장자 결합
        String savedName = classId + "_" + (fileCnt+1) + "강_" + origName;// + extension;

        // 파일을 불러올 때 사용할 파일 경로
        String savedPath = fileDir + savedName;
        
        log.info("파일 등록 " + (fileCnt+1) + "번쨰");
        int idx = fileCnt.intValue();
        OnlineClassFile file = OnlineClassFile.builder()
                .onlineClassId(id)
                .onlineClassFileId(fileId)
                .onlineFileOrgName(origName2)
                .onlineFileName(savedName)
                .onlineFileExt(extension)
                .onlineFileSize(files.getSize())
                .onlineFileRegdate(LocalDateTime.now())
                .onlineFileContent(fileContent.get(idx))
                .build();

        // 실제로 로컬에 저장
        files.transferTo(new File(savedPath));
        //썸네일 저장
        ffmpegManager.getThumbnail(savedPath);
        
        // 데이터베이스에 파일 정보 저장 
        OnlineClassFile savedFile = fileRepository.save(file);

        return savedFile.getOnlineClassId().getOnlineClassId();
    }
	 
	 /*강의 수정 관련*/
	 //온라인 클래스 첨부파일 리스트
	 public List<OnlineClassFile> findFileList(long onlineClassId) {
		return fileRepository.findAllByOnlineClassFileIdOnlineClassIdOrderByOnlineFileName(onlineClassId);
	}
	
	//온라인 클래스 수정
	@Transactional
	public Long onlineUpdate(final Long id, final OnlineUpdateDTO params) {
		 OnlineClass entity = onlineClassRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("not found: " + id));
	     entity.update(params.getOnlineClassTitle(), params.getOnlineClassContent(), params.getOnlineCategoryId(), params.getOnlineClassPeriod(), params.getOnlineClassPrice(), params.getRejection());

	    return id;
	}
	
	//게시글 수정 시 동영상 설명 변경
	@Transactional
	public Long contentUpdate(final Long id, final Long fileId, final String content) {
		 OnlineClassFile entity = fileRepository.findByOnlineClassFileIdOnlineClassIdAndOnlineClassFileIdOnlineFileId(id, fileId);
	     entity.contentUpdate(content);

	    return id;
	}
	
	//게시글 수정 시 동영상 수정 
	@Transactional
	public Long updateFile(MultipartFile files, Long classId, List<Integer> idxList, int i) throws IOException {
        if (files.isEmpty()) {
            return null;
        }
        
        //파일 번호
        int tmpNum = idxList.get(i);
        long fileNum = tmpNum;
        
        // 원래 파일 이름 추출
        String origName = files.getOriginalFilename();

        // 확장자 추출
        String extension = origName.substring(origName.lastIndexOf("."));
        String origName2 = origName.substring(0, origName.lastIndexOf("."));
        
        OnlineClass id = onlineClassRepository.findFirstByonlineClassId(classId);
        long len = 180;
        
        //기존 파일이 존재하면 수정
        OnlineClassFile entity = null;
        entity = fileRepository.findByOnlineClassFileIdOnlineClassIdAndOnlineClassFileIdOnlineFileId(classId, fileNum);
        
        if(entity != null) {
        	log.info("파일이 기존에 있으면");
	        String savedNameEx = entity.getOnlineFileName();
	        String savedPathEx = fileDir + savedNameEx;
	        
	        String imageNameEx = savedNameEx.substring(0, savedNameEx.lastIndexOf(".")) + ".png";
	        String imagePathEx = fileDir + imageNameEx;
	        
	        log.info("파일 삭제@@@@@@@@@@@@@@ " + savedNameEx);
	
	        File nfile2 = new File(savedPathEx);
	        if(nfile2.exists()) {
	        	nfile2.delete();
	        }
	        
	        File ifile2 = new File(imagePathEx);
	        if(ifile2.exists()) {
	        	ifile2.delete();
	        }
        	
        	// 확장자 결합
	        String savedName = classId + "_" + (fileNum) + "강_" + origName;// + extension;
	
	        // 파일을 불러올 때 사용할 파일 경로
	        String savedPath = fileDir + savedName;
	        
	        log.info("파일 수정 " + (fileNum) + "번쨰");
	        String findPath = savedPath + extension;
	        log.info("findPath" + findPath);
	        
	        //새로운 파일과 동일한 영상 있으면 삭제
	        File nfile = new File(savedPath);
	        if(nfile.exists()) {
	        	nfile.delete();
	        }
	        
	        String imageName = savedName.substring(0, savedName.lastIndexOf(".")) + ".png";
	        String imagePath = fileDir + imageName;
	        log.info("image"+imagePath);
	        File ifile = new File(imagePath);
	        if(ifile.exists()) {
	        	ifile.delete();
	        }
	        
	        // 실제로 로컬에 저장
	        files.transferTo(new File(savedPath));
	        //썸네일 저장
	        ffmpegManager.getThumbnail(savedPath);
	        
	        // 데이터베이스에 파일 정보 저장 여기
	        entity.update(origName2, savedName, extension, files.getSize());
        } else {
        	//기존파일이 존재하지 않으면 생성
        	Long fileCnt = fileRepository.countByOnlineClassFileId_onlineClassId(classId);

            OnlineClassFileId fileId = OnlineClassFileId.builder()
            		.onlineClassId(classId)
            		.onlineFileId(fileCnt+1)
            		.onlineFileLength(len)
            		.build();
            
            // 확장자 결합
            String savedName = classId + "_" + (fileCnt+1) + "강_" + origName;// + extension;

            // 파일을 불러올 때 사용할 파일 경로
            String savedPath = fileDir + savedName;
            
            log.info("파일 등록 " + (fileCnt+1) + "번쨰");
            OnlineClassFile file = OnlineClassFile.builder()
                    .onlineClassId(id)
                    .onlineClassFileId(fileId)
                    .onlineFileOrgName(origName2)
                    .onlineFileName(savedName)
                    .onlineFileExt(extension)
                    .onlineFileSize(files.getSize())
                    .onlineFileRegdate(LocalDateTime.now())
                    .build();

            // 실제로 로컬에 저장
            files.transferTo(new File(savedPath));
            //썸네일 저장
            ffmpegManager.getThumbnail(savedPath);
            
            // 데이터베이스에 파일 정보 저장
            fileRepository.save(file);
        }
       
        return classId;
    }
	
	//동영상 첨부파일 삭제
	@Transactional
	public int deleteFile(Long onlineClassId, Long fileId) throws IOException {
		OnlineClassFile entity = fileRepository.findByOnlineClassFileIdOnlineClassIdAndOnlineClassFileIdOnlineFileId(onlineClassId, fileId);
		
		 if(entity != null) {
			log.info("파일이 기존에 있으면");
	        String savedName = entity.getOnlineFileName();
	        String savedPath = fileDir + savedName;
	        
	        String imageName = savedName.substring(0, savedName.lastIndexOf(".")) + ".png";
	        String imagePath = fileDir + imageName;
	        
	        log.info("파일 삭제@@@@@@@@@@@@@@ " + savedName);
	
	        File nfile = new File(savedPath);
	        if(nfile.exists()) {
	        	nfile.delete();
	        }
	        
	        File ifile = new File(imagePath);
	        if(ifile.exists()) {
	        	ifile.delete();
	        }
		 }
		
		return fileRepository.deleteFile(onlineClassId, fileId);
	}
	
	//user 찾기
	public Optional<User> findByUserId(String userId) {
		return userRepository.findByUserId(userId);
	}
	
	//게시글 삭제(동영상 파일/썸네일/DB) 모두 삭제
	@Transactional
	public void postDelete(Long onlineClassId) {
		 File dirFile = new File(fileDir);
	     File[] fileList = dirFile.listFiles();
	     
	     for(File file: fileList) {
            if (file.getName().startsWith(onlineClassId+"_")) {
                log.info("파일삭제:"+file.getName());
            	file.delete();
            }
	     }
	     
	     int result = fileRepository.deleteByOnlineClassFileId_onlineClassId(onlineClassId);
		 log.info(result+"건의 파일을 DB에서 삭제했습니다.");
		 
	     String path = "C:/project/sts4/SFPN/src/main/resources/static/images/onlineThumbnail";
		 String fileName = onlineClassId + ".jpg";
		 String filepath = path + "/" + fileName;
	     
		 File imageFile = new File(filepath);
		 if(imageFile.exists()) {
			 imageFile.delete();
			 log.info("메인 이미지 삭제:"+filepath);
		 }
		 
		 onlineClassRepository.deleteById(onlineClassId);
	}
	
	/*pay*/
	//결제 QR
	public OfflineReadyResponse payReady(OnlinePaymentApproveDto onPayDto) {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		
		HttpHeaders httpHeaders = new HttpHeaders();
		
		String auth = "KakaoAK " + admin_Key;
		
		System.out.println("service : " + onPayDto.getTotalAmount());
		
		httpHeaders.set("Authorization", auth);
		httpHeaders.set("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		
		parameters.add("cid", cid);
        parameters.add("partner_order_id", onPayDto.getPartnerOrderId());
        parameters.add("partner_user_id", onPayDto.getPartnerUserId());
        parameters.add("item_code", onPayDto.getItemCode());
        parameters.add("item_name", onPayDto.getItemName());
        parameters.add("quantity", "1");
        parameters.add("total_amount", String.valueOf(onPayDto.getTotalAmount()));
        parameters.add("vat_amount", "0");
        parameters.add("tax_free_amount", "0");
        parameters.add("approval_url", "http://localhost:8008/online/paySuccess"); // 성공 시 redirect url
        parameters.add("cancel_url", "http://localhost:8008/online/payCancel"); // 취소 시 redirect url
        parameters.add("fail_url", "http://localhost:8008/online/payFail");
        parameters.add("point", String.valueOf(onPayDto.getPoint()));
        
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, httpHeaders);
        
        RestTemplate restTemplate = new RestTemplate();
        
        try 
		{
        	payReady = restTemplate.postForObject(
	        		new URI("https://kapi.kakao.com/v1/payment/ready"),
	        		requestEntity, 
	        		OfflineReadyResponse.class);
	        
	        payReady.setTid(payReady.getTid());
		}
		catch(URISyntaxException e) 
		{
			System.out.println(e);
		}
        
        return payReady;
	}
	
	//결제 완료
	public OfflineApproveResponse ApproveResponse(OnlinePayDto payDto, String pgToken) 
	 {
		 OfflineApproveResponse payApprove = null;
		 
		 RestTemplate restTemplate = new RestTemplate();
		 
		 HttpHeaders httpHeaders = new HttpHeaders();
			
		 String auth = "KakaoAK " + admin_Key;
		
		 httpHeaders.set("Authorization", auth);
		 httpHeaders.set("Content-type", "application/x-www-form-urlencoded;charset=utf-8");
		 
		 MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
		 
		 System.out.println("service : " + payDto.getOrderId());
		 
		 //Optional<OnlinePaymentApprove> approveDto = onlinePaymentApproveRepository.findAllByPartnerOrderId(payDto.getOrderId());
			
		 parameters.add("cid", cid);
		 parameters.add("tid", payDto.getTid());
         parameters.add("partner_order_id", payDto.getOrderId());
         parameters.add("partner_user_id", payDto.getPartnerUserId());
         //parameters.add("partner_user_id", approveDto.get().getPartnerUserId());
         parameters.add("pg_token", pgToken);
       
        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, httpHeaders);
       
        payApprove = restTemplate.postForObject(
        		"https://kapi.kakao.com/v1/payment/approve",
        		requestEntity, 
        		OfflineApproveResponse.class);
        
        return payApprove;
	 }
	
	//결제 찾기
	public Optional<OnlinePaymentApprove> findAllByPartnerOrderId(String orderId) {
		return onlinePaymentApproveRepository.findAllByPartnerOrderId(orderId);
	}
	
	//기존 결제 내역 찾기
	public List<OnlinePaymentApprove> findPay(String itemCode, String partnerUserId) {
		return onlinePaymentApproveRepository.findPay(itemCode, partnerUserId);
	}
	
	//재결제시 기존 결제 상태 R로 업데이트
	@Transactional
	public int updatePayStatus(String itemCode, String partnerUserId) {
		return onlinePaymentApproveRepository.updatePayStatus(itemCode, partnerUserId);
	}
	
	//결제 저장
	@Transactional
	public OnlinePaymentApproveDto payUpload(OfflineApproveResponse payDto, String point) {
		log.info("payDto.getApproved_at():"+payDto.getApproved_at());
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
		LocalDateTime dateTime = LocalDateTime.parse(payDto.getApproved_at(), formatter);
		log.info("dateTime:"+dateTime);

		//재수강
		if(!findPay(payDto.getItem_code(), payDto.getPartner_user_id()).isEmpty()) {
			if(updatePayStatus(payDto.getItem_code(), payDto.getPartner_user_id()) > 0){
				log.info("결제상태 재결제로 업데이트");
			}
		}
		
		OnlinePaymentApprove onlinePaymentApprove = OnlinePaymentApprove.builder()
				.cid(payDto.getCid())
				.tid(payDto.getTid())
				.partnerOrderId(payDto.getPartner_order_id())
				.partnerUserId(payDto.getPartner_user_id())
				.itemCode(payDto.getItem_code())
				.itemName(payDto.getItem_name())
				.point(Long.parseLong(point))
				.totalAmount(Long.valueOf(payDto.getAmount().getTotal()))
				.taxFreeAmount(Long.valueOf(0))
				.approvedAt(dateTime)
				.status(Status.N)
				.build();
		
		onlinePaymentApproveRepository.save(onlinePaymentApprove);
		OnlinePaymentApproveDto approveDto = new OnlinePaymentApproveDto();
		
	    return approveDto;
	}
	
	@Transactional
	public int updatePoint(String userId, Long point)
	{
		return offUserRepository.updatePoint(userId, point);
	}

}
	

