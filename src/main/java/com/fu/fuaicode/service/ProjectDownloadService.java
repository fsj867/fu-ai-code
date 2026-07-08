package com.fu.fuaicode.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {
    void downloadProjcetAsZip(String projectPath, String projectName, HttpServletResponse response);
}
