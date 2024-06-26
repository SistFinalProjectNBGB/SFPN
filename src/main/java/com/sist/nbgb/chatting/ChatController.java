package com.sist.nbgb.chatting;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.sist.nbgb.entity.Chat;
import com.sist.nbgb.entity.ChatMessage;
import com.sist.nbgb.entity.Instructors;
import com.sist.nbgb.entity.User;
import com.sist.nbgb.enums.Status;
import com.sist.nbgb.response.ChatResponse;
import com.sist.nbgb.service.OfflineReviewService;
import com.sist.nbgb.service.OfflineService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatController
{
    private final ChatService chatService;
    private final OfflineService offlineService;
    private final OfflineReviewService offlineReviewService;

    @RequestMapping("/chat/chatList")
    public String chatList(Model model)
    {
    	List<ChatDto> list = null;
    	
    	String userId = null;
    	
    	int a = 0;
    	
    	if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_USER]"))
        {
    		userId = SecurityContextHolder.getContext().getAuthentication().getName();
    		
    		list = chatService.listUserId(userId).stream()
    				.map(ChatDto::new)
    				.collect(Collectors.toList());
    		
    		a = 1;
        }
		else if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_INSTRUCTOR]"))
		{
			userId = SecurityContextHolder.getContext().getAuthentication().getName();
			
			list = chatService.listInstructorId(userId).stream()
    				.map(ChatDto::new)
    				.collect(Collectors.toList());
			
			a = 2;
		}
		else
		{
			return "/chat/chatAlert";
		}
    	
    	for(ChatDto lastChat : list)
    	{
    		ChatMessage lastMessage = chatService.lastChat(lastChat.getChatId());
    		
    		lastChat.setLastChat(lastMessage.getMessageContent());
    		
    		if(lastMessage.getMessageRead() == Status.N)
    		{
    			if(!lastMessage.getSendId().equals(userId))
    			{
    				lastChat.setMessageRead("N");
    			}
    			else
    			{
    				lastChat.setMessageRead("Y");
    			}
    		}
    		else
    		{
    			lastChat.setMessageRead("Y");
    		}
    		
    		if(a == 2)//로그인 객체가 강사일 경우 상대(일반사용자) 사진 구분
    		{
    			if(offlineReviewService.getImg(lastChat.getUserId().getUserId()) == "Y")
    			{
    				lastChat.setImg("Y");
				}
    			else
    			{
    				lastChat.setImg("N");
    			}
    		}
    		else if(a == 1)
    		{
    			lastChat.setImg("I");
    		}
    	}
    	
    	model.addAttribute("list", list);
    	
        List<ChattingRoom> roomList = chatService.findAllRoom();
        model.addAttribute("roomList",roomList);
        return "/chat/chatList";
    }


//    @PostMapping("/chat/createRoom")  //방을 만들었으면 해당 방으로 가야지.
//    public String createRoom(Model model, @RequestParam String instructorId)
//    {
//    	if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_USER]"))
//        {
//    		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
//        	
//        	Optional<UserResponse> user = offlineService.findByUserId(userId)
//    				.map(UserResponse::new);
//        	
//        	String userIId = user.get().getUserId();
//        	
//        	System.out.println(instructorId);
//			
//	        ChattingRoom room = chatService.createRoom(instructorId, userId);
//	        
//			model.addAttribute("user", userIId);
//	        model.addAttribute("chatId",room.getChatId());
//	        return "/chat/chatRoom";  //만든사람이 채팅방 1빠로 들어가게 됩니다
//        }
//		else
//		{
//			return "/offline/userAlert";
//		}
//    }
    
    @PostMapping("/chat/findChatRoom")
    @ResponseBody
    public ResponseEntity<ChatResponse> findChatRoom(@RequestParam("instructorId") String instructorId)
    {
    	ChatResponse chatResponse = new ChatResponse();
    	
    	if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_USER]"))
        {
    		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    		
    		Chat found = chatService.findByInstructorIdAndUserId(instructorId, userId);
    		
    		if(found != null)
    		{
    			chatResponse.setChatId(found.getChatId());
    			chatResponse.setCode(1);
    		}
    		else
    		{
    			ChattingRoom room = chatService.createRoom(instructorId, userId);
    			
    			chatResponse.setChatId(room.getChatId());
    			chatResponse.setCode(0);
    		}
        }
    	else
    	{
    		chatResponse.setCode(3);
    	}
    	
    	return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/chat/chatRoom/{chatId}")
    public String chatRoom(Model model, @PathVariable String chatId)
    {
    	Chat room = chatService.findChatBychatId(chatId);
    	
		if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_USER]"))
        {
    		String userId = SecurityContextHolder.getContext().getAuthentication().getName();
    	   		
    		if(userId.equals(room.getUserId().getUserId()))
    		{
    			Optional<User> user = offlineService.findByUserId(userId);
        		
        		if(offlineReviewService.getImg(userId) == "Y")
        		{
        			model.addAttribute("img", "UY");
        		}
        		else
        		{
        			model.addAttribute("img", "UN");
        		}
        		
        		chatService.updateRead(chatId, room.getInstructorId().getInstructorId());
        		
    			model.addAttribute("mem", "u");
    			
    			model.addAttribute("userId", userId);
    			model.addAttribute("user", user.get());
    		}
    		else
    		{
    			model.addAttribute("error", "r");
    			
    			return "/chat/chatAlert";
    		}
        }
		else if(SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString().equals("[ROLE_INSTRUCTOR]"))
		{
			String userId = SecurityContextHolder.getContext().getAuthentication().getName();
						
			if(userId.equals(room.getInstructorId().getInstructorId()))
    		{
				Optional<Instructors> user = offlineService.findByInstructorId(userId);
				
				if(offlineReviewService.getImg(room.getUserId().getUserId()) == "Y")
	    		{
	    			model.addAttribute("img", "IY");
	    		}
	    		else
	    		{
	    			model.addAttribute("img", "IN");
	    		}
				
				chatService.updateRead(chatId, room.getUserId().getUserId());
				
				model.addAttribute("mem", "i");
				
				model.addAttribute("userId", userId);
				model.addAttribute("user", user.get());
    		}
    		else
    		{
    			model.addAttribute("error", "r");
    			
    			return "/chat/chatAlert";
    		}
		}
		else
		{
			model.addAttribute("error", "o");
			
			return "/chat/chatAlert";
		}
    	
        
        List<ChatMessageDto> list = chatService.findMessageList(chatId).stream()
				.map(ChatMessageDto::new)
				.collect(Collectors.toList());
        
        model.addAttribute("chatId",chatId);
        model.addAttribute("list",list);
        model.addAttribute("room",room);
    	
        return "/chat/chatRoom";
    }
}