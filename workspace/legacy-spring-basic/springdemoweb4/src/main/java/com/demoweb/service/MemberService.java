package com.demoweb.service;

import com.demoweb.dto.MemberDto;

public interface MemberService {

	void registerMember(MemberDto member);

	MemberDto login(String memberId, String passwd);

}