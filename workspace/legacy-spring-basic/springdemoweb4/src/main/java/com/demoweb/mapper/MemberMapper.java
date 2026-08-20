package com.demoweb.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.demoweb.dto.MemberDto;

@Mapper
public interface MemberMapper {
	
	void insertMember(MemberDto member);

	// @Param("memberId") -> mapper.xml에서 ${memberId}에 연결
	MemberDto selectByMemberId(@Param("memberId") String memberId); 

}
