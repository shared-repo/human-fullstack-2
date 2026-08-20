package com.demoweb.service;

import org.springframework.stereotype.Service;

import com.demoweb.common.Util;
import com.demoweb.dto.MemberDto;
import com.demoweb.mapper.MemberMapper;

@Service("memberService") // @Component의 별칭 : Service 클래스에 적용
public class MemberServiceImpl implements MemberService {
	
	private final MemberMapper memberMapper;
	public MemberServiceImpl(MemberMapper memberMapper) {
		this.memberMapper = memberMapper;
	}

	@Override
	public void registerMember(MemberDto member) {
		
		String hashedPasswd = Util.getHashedString(member.getPasswd(), "SHA-256");
		member.setPasswd(hashedPasswd);
		
		memberMapper.insertMember(member);		
	}
	
	@Override
	public MemberDto login(String memberId, String passwd) {

		MemberDto member = memberMapper.selectByMemberId(memberId);

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
