package examplesweb.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet(urlPatterns = { "/upload" })
public class FileUploadServlet extends HttpServlet {
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
	
		// multipart/form-data 방식의 요청은 req.getParameter로 읽을 수 없습니다.
		// String data1 = req.getParameter("data1");
		// System.out.println("----------------> " + data1);
		
		// -----------------------------------------------------------------------
		//request 객체를 분석해서 파일 업로드를 포함한 요청인지 확인 (multipart/form-data 형식 확인)
		// if (JakartaServletFileUpload.isMultipartContent(req) == false) {
		if (!JakartaServletFileUpload.isMultipartContent(req)) {
			resp.sendRedirect("09.fileupload.jsp");
			return;
		}

		//업로드한 파일을 저장할 경로를 저장할 변수
		//application.getRealPath('웹경로')
		//--> 가상경로(웹경로) -> 물리경로(컴퓨터경로)
		//--> http://..... -> C:/......
		ServletContext application = req.getServletContext(); 		// JSP의 application 내장 객체
		String path = application.getRealPath("/upload-files");		// 최종 파일 저장 경로
		Path uploadDir = Path.of(path); // 문자열 경로를 사용해서 경로를 관리하는 객체 생성
		
		//전송 데이터 각 요소를 분리해서 개별 객체를 만들때 사용할 처리기
		DiskFileItemFactory factory = DiskFileItemFactory.builder().get();

		//요청 정보를 읽을 파서(Parser) 생성 (요청을 읽고 요소별로 분리)
		JakartaServletFileUpload uploader = new JakartaServletFileUpload(factory);
		uploader.setMaxFileSize(1024 * 1024 * 10); //최대 파일 크기

		//요청 정보를 파싱하고 개별 객체의 목록을 반환
		List<FileItem> items = null;
		try {
			items = uploader.parseRequest(req); // 요청의 각 데이터를 분해 + 포장 -> 리스트로 반환
		} catch (FileUploadException e) {
			e.printStackTrace();
		}

		////////////////////////////////////////////////////////////////////////////////

		//목록에 담긴 데이터 사용
		for (FileItem item : items) {			
			if (item.isFormField()) { // form-data인 경우 (File이 아닌 일반 데이터인 경우)
				System.out.println("DATA " + item.getFieldName() + " : " 	// 데이터 이름 ( <input> 요소의 name 속성 값 )
										   + item.getString(StandardCharsets.UTF_8));		// 데이터 값	( <input> 요소의 value 속성 값 )
			} else { //file인 경우
				String fileName = item.getName(); //파일 이름 가져오기
				if (fileName != null && fileName.length() > 0) { //내용이 있는 경우
//					if (fileName.contains("\\")) { // iexplore 경우
//						//C:\AAA\BBB\CCC.png -> CCC.png 
//						fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
//					}					
					try {						
						InputStream in = item.getInputStream();
						Path target = uploadDir.resolve(fileName); // directory + fileName
						Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING); // 파일 쓰기
					} catch (Exception e) {						
						e.printStackTrace();
					} 
					item.delete(); // upload 과정에서 사용한 임시 파일 삭제
				}
			}
		}

		resp.sendRedirect("09.filelist.jsp");
		
		
	}

}