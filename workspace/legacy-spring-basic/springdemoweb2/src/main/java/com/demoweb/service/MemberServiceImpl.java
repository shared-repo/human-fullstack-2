package com.demoweb.service;

import org.springframework.stereotype.Service;

import com.demoweb.common.Util;
import com.demoweb.dao.MemberDao;
import com.demoweb.dao.MemberDaoImpl;
import com.demoweb.dto.MemberDto;

@Service("memberService") // @Component의 별칭 : Service 클래스에 적용
public class MemberServiceImpl implements MemberService {
	
	private MemberDao memberDao; // 다른 레이어의 클래스 타입 변수는 보통 필드로 선언합니다.
	public MemberServiceImpl(MemberDao memberDao) {
		this.memberDao = memberDao;
	}

	@Override
	public void registerMember(MemberDto member) {
		
		String hashedPasswd = Util.getHashedString(member.getPasswd(), "SHA-256");
		member.setPasswd(hashedPasswd);
		
		memberDao.insertMember(member);
		
	}
	
	@Override
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
