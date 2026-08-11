package com.demoweb.listener;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@WebListener
public class DemoWebListener
    implements HttpSessionListener, ServletContextListener {

    private static final String TOTAL_VISITOR_COUNT = "totalVisitorCount";
    private static final String CURRENT_VISITOR_COUNT = "currentVisitorCount";

    private static final String FILE_NAME = "/WEB-INF/visitor-count.dat";

    /**
     * 애플리케이션 시작
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {

        ServletContext application = sce.getServletContext();

        int totalCount = 0;

        String path = application.getRealPath(FILE_NAME);
        File file = new File(path);

        if (file.exists()) {

            try (DataInputStream in =
                    new DataInputStream(new FileInputStream(file))) {

                totalCount = in.readInt();

            } catch (Exception ex) {
                ex.printStackTrace();
            }

        }

        application.setAttribute(TOTAL_VISITOR_COUNT, totalCount);
        application.setAttribute(CURRENT_VISITOR_COUNT, 0);
    }

    /**
     * 세션 시작
     */
    @Override
    public void sessionCreated(HttpSessionEvent se) {

        ServletContext application =
            se.getSession().getServletContext();

        synchronized (application) {

            int totalCount =
                (Integer)application.getAttribute(TOTAL_VISITOR_COUNT);

            int currentCount =
                (Integer)application.getAttribute(CURRENT_VISITOR_COUNT);

            totalCount++;
            currentCount++;

            application.setAttribute(TOTAL_VISITOR_COUNT, totalCount);
            application.setAttribute(CURRENT_VISITOR_COUNT, currentCount);
        }

    }

    /**
     * 세션 종료
     */
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {

        ServletContext application =
            se.getSession().getServletContext();

        synchronized (application) {

            int currentCount =
                (Integer)application.getAttribute(CURRENT_VISITOR_COUNT);

            if (currentCount > 0) {
                currentCount--;
            }

            application.setAttribute(CURRENT_VISITOR_COUNT, currentCount);
        }

    }

    /**
     * 애플리케이션 종료
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {

        ServletContext application = sce.getServletContext();

        String path = application.getRealPath(FILE_NAME);

        File file = new File(path);

        try {

            file.getParentFile().mkdirs();

            try (DataOutputStream out =
                    new DataOutputStream(new FileOutputStream(file))) {

                int totalCount =
                    (Integer)application.getAttribute(TOTAL_VISITOR_COUNT);

                out.writeInt(totalCount);

            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

}