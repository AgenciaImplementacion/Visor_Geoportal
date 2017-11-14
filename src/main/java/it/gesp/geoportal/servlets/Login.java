package it.gesp.geoportal.servlets;

import it.gesp.geoportal.dao.entities.User;
import it.gesp.geoportal.locale.LocaleUtils;
import it.gesp.geoportal.services.LogService;
import it.gesp.geoportal.services.LoginService;
import it.gesp.geoportal.services.UserService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import it.gesp.geoportal.json.JsonFactory;
import it.gesp.geoportal.utils.ConfigUtils;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import javax.servlet.http.Cookie;
import static org.apache.commons.httpclient.params.HttpMethodParams.USER_AGENT;

public class Login extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = Logger.getLogger(Login.class);

    private ResourceBundle dbMessages = LocaleUtils.getDBMessages();

    public Login() {
        super();
    }

    protected void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doWork(request, response);
    }

    protected void doPost(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {
        doWork(request, response);
    }

    private void doLogout(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        HttpSession currentSession = request.getSession(false);
        if (currentSession != null) {
            User currentUser = LoginService
                    .getLoggedInUserFromSession(currentSession);
            if (currentUser != null) {
//				LogService.writeLog(currentUser, dbMessages.getString("LOGIN_CONTEXT"),
//						dbMessages.getString("LOGOUT_OPERATION"),
//						dbMessages.getString("LOGOUT_DESC"));
            }

            currentSession.invalidate();
        }

        String jsonRes = createLogoutResponse();

        PrintWriter w = response.getWriter();
        log.debug("Login Servlet response: " + jsonRes);
        ServletUtils.writeAndFlush(log, w, jsonRes);
        return;
    }

    private String loginValidate(Cookie[] cookies) {
        try {
            String url = "https://ide.proadmintierra.info/simplesaml/auth.php";

            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            String scookies = "";
            for (Cookie c : cookies) {
                scookies += c.getName() + "=" + c.getValue() + "; ";
            }
            //con.setRequestProperty("Cookie", scookies);
            con.setRequestProperty("Cookie", "JSESSIONID=B1206D0F6A74698A2AE3EF51416D5593; PHPSESSID=dac3qtdt4q9hg40fimfsqh0vh8; SESS8936a60e2094e7527ec6dd0de2b53e86=E6QIAph14unVLKUQMJdwRHLIUwYkXHUobovwXWRKl80; SimpleSAMLSessionID=642f6ef5d18d3fa089160dbeca065de8; SimpleSAMLAuthToken=_77ed29d064b208470fa0f83e5fbb603985532a34ca; has_js=1");

            //add request header
            con.setRequestProperty("User-Agent", USER_AGENT);

            int responseCode = con.getResponseCode();
            System.out.println("\nSending 'GET' request to URL : " + url);
            System.out.println("Response Code : " + responseCode);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            //print result
            return response.toString();
        } catch (MalformedURLException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void doWork(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession currentSession = request.getSession(true);
        if (ConfigUtils.get("login.WSO2") != null && Boolean.valueOf(ConfigUtils.get("login.WSO2").toString()) == true) {
            String data = this.loginValidate(request.getCookies());
            UserService us = new UserService();
            User currentUser = null;
            if (data == null) {
                try {
                    //currentUser = us.doLoginGuest();
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
                }
            } else {
                JsonParser parser = new JsonParser();
                JsonObject o = (JsonObject) parser.parse(data);
                try {
                    currentUser = us.doLoginUser(o.get("user").toString().replaceAll("\"", ""));
                } catch (Exception ex) {
                    java.util.logging.Logger.getLogger(Login.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            if (currentUser != null) {
                LoginService.setLoggedInUserInSession(currentSession,
                        currentUser);
                String jsonRes = createLoginResponse(currentUser);
                PrintWriter w = response.getWriter();
                log.debug("Login Servlet response: " + jsonRes);
                ServletUtils.writeAndFlush(log, w, jsonRes);
                return;
            } else {
                String jsonRes = createLoginResponse(null);
                PrintWriter w = response.getWriter();
                log.debug("Login Servlet response: " + jsonRes);
                ServletUtils.writeAndFlush(log, w, jsonRes);
            }
        } else {

            String currentLanguage = LoginService.getCurrentLanguageFromSessionOrDefault(currentSession);

            ResourceBundle userMessages = LocaleUtils.getUserMessages(currentLanguage);

            String oper = request.getParameter("oper");

            try {
                if (oper != null) {
                    log.debug("Login Servlet - Operation " + oper);
                } else {
                    log.debug("Login Servlet - NULL Operation parameter");
                }

                if ("logout".equals(oper)) {
                    log.debug("Logout requested");
                    doLogout(request, response);
                    return;
                }

                if ("login".equals(oper)) {
                    log.debug("Login requested");
                    String username = request.getParameter("username");
                    String hashedPassword = request.getParameter("password");
                    log.debug("Username and password retrieved");

                    UserService us = new UserService();
                    User currentUser = us.doLoginUser(username, hashedPassword);

                    if (currentUser == null) {
                        // Wrong username and password.

                        // Remove from session
                        LoginService.setLoggedInUserInSession(currentSession, null);
                        log.debug("Wrong username and password.");
                        GeoportalResponse gpr = new GeoportalResponse();
                        gpr.setSuccess(false);
                        gpr.setMsg(userMessages.getString("LOGIN_WRONG_USERNAME_PASSWORD"));
                        String jsonRes = new Gson().toJson(gpr);

                        PrintWriter w = response.getWriter();
                        log.debug("Login Servlet response: " + jsonRes);
                        ServletUtils.writeAndFlush(log, w, jsonRes);
                        return;

                    } else {
                        // Username and password correct
                        log.debug("User logged in successfully. Attach user info to the session.");
//					LogService.writeLog(currentUser,
//							dbMessages.getString("LOGIN_CONTEXT"),
//							dbMessages.getString("LOGIN_OPERATION"),
//							dbMessages.getString("LOGIN_DESC"));

                        LoginService.setLoggedInUserInSession(currentSession,
                                currentUser);

                        String jsonRes = createLoginResponse(currentUser);

                        PrintWriter w = response.getWriter();
                        log.debug("Login Servlet response: " + jsonRes);
                        ServletUtils.writeAndFlush(log, w, jsonRes);
                        return;
                    }
                } else if ("currentUser".equals(oper)) {
                    //log.debug("Get current user");

                    User currentUser = LoginService
                            .getLoggedInUserFromSession(currentSession);
                    String jsonRes = createLoginResponse(currentUser);

                    PrintWriter w = response.getWriter();
                    //log.debug("Login Servlet response: " + jsonRes);
                    ServletUtils.writeAndFlush(log, w, jsonRes);
                    return;

                } else if ("setLanguage".equals(oper)) {
                    PrintWriter w = response.getWriter();

                    String languageCode = request.getParameter("language");
                    if (LocaleUtils.isLanguageSupported(languageCode)) {
                        LoginService.setCurrentLanguageInSession(currentSession, languageCode);

                        String jsonRes = GeoportalResponse.createSuccessResponse(languageCode, true);
                        ServletUtils.writeAndFlush(log, w, jsonRes);
                    } else {
                        String jsonRes = GeoportalResponse.createErrorResponse("Language is not supported");
                        ServletUtils.writeAndFlush(log, w, jsonRes);
                    }
                }
            } catch (Exception x) {
                log.debug(x);
            }
        }
    }

    private String createLoginResponse(User user) {
        log.debug("Creating login response");
        String jsonRes = null;
        if (user != null) {

            GeoportalResponse gpr = new GeoportalResponse();
            gpr.setSuccess(true);
            List<String> permissionsJson = user.getPermissionCodList();

            Map<String, Object> map = new HashMap<String, Object>();
            map.put("username", user.getUsername());
            map.put("permissions", permissionsJson);
            gpr.setResult(map);
            jsonRes = new Gson().toJson(gpr);
        } else {
            //user is null
            jsonRes = GeoportalResponse.createErrorResponse("USER NOT LOGGED");
        }

        return jsonRes;
    }

    private String createLogoutResponse() {
        log.debug("Creating logout response");
        GeoportalResponse gpr = new GeoportalResponse();
        gpr.setSuccess(true);
        String jsonRes = new Gson().toJson(gpr);
        return jsonRes;
    }
}
