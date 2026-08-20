package com.demoweb.dto;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data // 자동으로 모든 필드의 getter, setter 생성
public class BoardDto {

	private int boardNo;
	private String title;
	private String writer;
	private String content;
	private Date writeDate;
	private Date modifyDate;
	private int readCount;
	private boolean deleted;
	
	// Board 테이블과 BoardAttach 테이블 사이의 1 : Many 관계를 구현하는 필드
	private List<BoardAttachDto> boardAttachList;

}