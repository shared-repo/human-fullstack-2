package com.demoweb.dto;

import java.util.Date;

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

}