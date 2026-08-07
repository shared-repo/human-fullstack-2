package com.demoweb.service;

import com.demoweb.common.Util;
import com.demoweb.dao.MemberDao;
import com.demoweb.dto.MemberDto;

public class MemberService {
	
	private MemberDao memberDao = new MemberDao(); // 다른 레이어의 클래스 타입 변수는 보통 필드로 선언합니다.

	public void registerMember(MemberDto member) {
		
		String hashedPasswd = Util.getHashedString(member.getPasswd(), "SHA-256");
		member.setPasswd(hashedPasswd);
		
		memberDao.insertMember(member);
		
	}
	
	public MemberDto login(String memberId, String passwd) {

	    MemberDto member = memberDao.selectByMemberId(memberId);

	    if (member == null) {
	        return null;
	    }

	    passwd = Util.getHashedString(passwd, "SHA-256");
	    if (!member.getPasswd().equals(passwd)) {
	        return null;
	    }

	    return member;
	}

}
