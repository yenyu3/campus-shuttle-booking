import java.io.*;
import java.net.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import com.sun.net.httpserver.*;

public class SimpleApp {
    private static List<Schedule> schedules = new ArrayList<>();
    private static List<Booking> bookings = new ArrayList<>();
    private static long scheduleIdCounter = 1;
    private static long bookingIdCounter = 1;

    public static void main(String[] args) throws IOException {
        initializeData();
        
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        // 設定CORS
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/schedules", new ScheduleHandler());
        server.createContext("/api/bookings", new BookingHandler());
        server.createContext("/", new StaticFileHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("Server started at http://localhost:8080");
        System.out.println("Press Ctrl+C to stop server");
    }
    
    private static void initializeData() {
        LocalDate today = LocalDate.now();
        
        String[] routes = {"\u4E2D\u592E\u5927\u5B78-\u6843\u5712\u9AD8\u9435\u7AD9", "\u6843\u5712\u9AD8\u9435\u7AD9-\u4E2D\u592E\u5927\u5B78", "\u4E2D\u592E\u5927\u5B78-\u4E2D\u58E2\u706B\u8ECA\u7AD9", "\u4E2D\u58E2\u706B\u8ECA\u7AD9-\u4E2D\u592E\u5927\u5B78"};
        LocalTime[] times = {
            LocalTime.of(8, 0), LocalTime.of(9, 30), LocalTime.of(11, 0),
            LocalTime.of(13, 30), LocalTime.of(15, 0), LocalTime.of(16, 30)
        };

        // 生成一個月的班次資料
        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            for (String route : routes) {
                for (LocalTime time : times) {
                    schedules.add(new Schedule(scheduleIdCounter++, date, route, time, 20, 20));
                }
            }
        }
    }
    
    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if ("POST".equals(exchange.getRequestMethod())) {
                // 讀取請求內容
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                // 解析用戶名
                String body = requestBody.toString();
                String username = extractJsonValue(body, "username");
                
                System.out.println("Login request for user: " + username); // Debug
                
                if (username != null && !username.isEmpty()) {
                    String response = "{\"success\": true, \"studentId\": \"" + username + "\"}";
                    exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes("UTF-8"));
                    os.close();
                } else {
                    String response = "{\"success\": false, \"message\": \"登入失敗\"}";
                    exchange.sendResponseHeaders(400, response.getBytes("UTF-8").length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes("UTF-8"));
                    os.close();
                }
            }
        }
    }
    
    static class ScheduleHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String dateStr = params.get("date");
                String route = params.get("route");
                
                // 檢查日期是否在允許範圍內
                if (dateStr != null) {
                    LocalDate queryDate = LocalDate.parse(dateStr);
                    LocalDate today = LocalDate.now();
                    LocalDate maxDate = today.plusDays(30);
                    
                    if (queryDate.isBefore(today) || queryDate.isAfter(maxDate)) {
                        String response = "{\"error\": \"尚未開放預約\"}";
                        exchange.sendResponseHeaders(400, response.getBytes("UTF-8").length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes("UTF-8"));
                        os.close();
                        return;
                    }
                }
                
                List<Schedule> filtered = new ArrayList<>();
                for (Schedule s : schedules) {
                    if (dateStr != null && !s.date.toString().equals(dateStr)) continue;
                    if (route != null && !route.isEmpty() && !s.route.equals(route)) continue;
                    filtered.add(s);
                }
                
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < filtered.size(); i++) {
                    if (i > 0) json.append(",");
                    Schedule s = filtered.get(i);
                    json.append("{")
                        .append("\"id\":").append(s.id).append(",")
                        .append("\"date\":\"").append(s.date).append("\",")
                        .append("\"route\":\"").append(s.route).append("\",")
                        .append("\"departureTime\":\"").append(s.departureTime).append("\",")
                        .append("\"totalSeats\":").append(s.totalSeats).append(",")
                        .append("\"availableSeats\":").append(s.availableSeats)
                        .append("}");
                }
                json.append("]");
                
                String response = json.toString();
                exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            }
        }
    }
    
    static class BookingHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            setCORSHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1);
                return;
            }
            
            if ("GET".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                String studentId = path.substring(path.lastIndexOf("/") + 1);
                
                System.out.println("Getting bookings for student: " + studentId); // Debug
                
                List<Booking> userBookings = new ArrayList<>();
                for (Booking b : bookings) {
                    System.out.println("Checking booking: " + b.studentId + " vs " + studentId); // Debug
                    if (b.studentId.equals(studentId)) {
                        userBookings.add(b);
                    }
                }
                
                System.out.println("Found " + userBookings.size() + " bookings for " + studentId); // Debug
                
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < userBookings.size(); i++) {
                    if (i > 0) json.append(",");
                    Booking b = userBookings.get(i);
                    Schedule s = findScheduleById(b.scheduleId);
                    if (s != null) {
                        json.append("{")
                            .append("\"id\":").append(b.id).append(",")
                            .append("\"studentId\":\"").append(b.studentId).append("\",")
                            .append("\"seatNumber\":\"").append(b.seatNumber).append("\",")
                            .append("\"schedule\":{")
                            .append("\"id\":").append(s.id).append(",")
                            .append("\"date\":\"").append(s.date).append("\",")
                            .append("\"route\":\"").append(s.route).append("\",")
                            .append("\"departureTime\":\"").append(s.departureTime).append("\"")
                            .append("}")
                            .append("}");
                    }
                }
                json.append("]");
                
                String response = json.toString();
                exchange.sendResponseHeaders(200, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            } else if ("POST".equals(exchange.getRequestMethod())) {
                // 讀取請求內容
                StringBuilder requestBody = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        requestBody.append(line);
                    }
                }
                
                // 簡單解析JSON（實際應用中應使用JSON庫）
                String body = requestBody.toString();
                String studentId = extractJsonValue(body, "studentId");
                String scheduleIdStr = extractJsonValue(body, "scheduleId");
                String seatNumber = extractJsonValue(body, "seatNumber");
                
                if (studentId != null && scheduleIdStr != null && seatNumber != null) {
                    long scheduleId = Long.parseLong(scheduleIdStr);
                    
                    // 找到對應班次
                    Schedule schedule = findScheduleById(scheduleId);
                    if (schedule != null && schedule.availableSeats > 0) {
                        // 檢查座位是否已被預約
                        boolean seatTaken = false;
                        for (Booking b : bookings) {
                            if (b.scheduleId == scheduleId && b.seatNumber.equals(seatNumber)) {
                                seatTaken = true;
                                break;
                            }
                        }
                        
                        if (!seatTaken) {
                            // 建立預約
                            Booking newBooking = new Booking(bookingIdCounter++, studentId, scheduleId, seatNumber);
                            bookings.add(newBooking);
                            
                            System.out.println("Created booking for " + studentId + ", total bookings: " + bookings.size()); // Debug
                            
                            // 減少可用座位
                            schedule.availableSeats--;
                            
                            String response = "{\"id\": " + newBooking.id + ", \"success\": true}";
                            exchange.sendResponseHeaders(200, response.getBytes().length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes());
                            os.close();
                            return;
                        } else {
                            String response = "{\"error\": \"座位已被預約\"}";
                            exchange.sendResponseHeaders(400, response.getBytes("UTF-8").length);
                            OutputStream os = exchange.getResponseBody();
                            os.write(response.getBytes("UTF-8"));
                            os.close();
                            return;
                        }
                    }
                }
                
                String response = "{\"error\": \"預約失敗\"}";
                exchange.sendResponseHeaders(400, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            } else if ("DELETE".equals(exchange.getRequestMethod())) {
                String path = exchange.getRequestURI().getPath();
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);
                
                String bookingIdStr = path.substring(path.lastIndexOf("/") + 1);
                String studentId = params.get("studentId");
                
                if (bookingIdStr != null && studentId != null) {
                    long bookingId = Long.parseLong(bookingIdStr);
                    
                    // 找到並刪除預約
                    Booking toRemove = null;
                    for (Booking b : bookings) {
                        if (b.id == bookingId && b.studentId.equals(studentId)) {
                            toRemove = b;
                            break;
                        }
                    }
                    
                    if (toRemove != null) {
                        bookings.remove(toRemove);
                        
                        // 增加可用座位
                        Schedule schedule = findScheduleById(toRemove.scheduleId);
                        if (schedule != null) {
                            schedule.availableSeats++;
                        }
                        
                        String response = "{\"success\": true}";
                        exchange.sendResponseHeaders(200, response.getBytes().length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response.getBytes());
                        os.close();
                        return;
                    }
                }
                
                String response = "{\"error\": \"刪除失敗\"}";
                exchange.sendResponseHeaders(400, response.getBytes("UTF-8").length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes("UTF-8"));
                os.close();
            }
        }
    }
    
    static class StaticFileHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/")) path = "/index.html";
            
            File file = new File("." + path);
            if (file.exists() && file.isFile()) {
                String contentType = getContentType(path);
                exchange.getResponseHeaders().set("Content-Type", contentType);
                exchange.sendResponseHeaders(200, file.length());
                
                FileInputStream fis = new FileInputStream(file);
                OutputStream os = exchange.getResponseBody();
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
                os.close();
            } else {
                String response = "404 Not Found";
                exchange.sendResponseHeaders(404, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }
    
    private static void setCORSHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }
    
    private static Map<String, String> parseQuery(String query) {
        Map<String, String> result = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    try {
                        result.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                        result.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        return result;
    }
    
    private static Schedule findScheduleById(long id) {
        for (Schedule s : schedules) {
            if (s.id == id) return s;
        }
        return null;
    }
    
    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int startIndex = json.indexOf(searchKey);
        if (startIndex == -1) return null;
        
        startIndex += searchKey.length();
        while (startIndex < json.length() && (json.charAt(startIndex) == ' ' || json.charAt(startIndex) == '\t')) {
            startIndex++;
        }
        
        if (startIndex >= json.length()) return null;
        
        char firstChar = json.charAt(startIndex);
        if (firstChar == '"') {
            // String value
            startIndex++;
            int endIndex = json.indexOf('"', startIndex);
            if (endIndex == -1) return null;
            return json.substring(startIndex, endIndex);
        } else {
            // Number value
            int endIndex = startIndex;
            while (endIndex < json.length() && Character.isDigit(json.charAt(endIndex))) {
                endIndex++;
            }
            if (endIndex == startIndex) return null;
            return json.substring(startIndex, endIndex);
        }
    }
    
    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }
    
    static class Schedule {
        long id;
        LocalDate date;
        String route;
        LocalTime departureTime;
        int totalSeats;
        int availableSeats;
        
        Schedule(long id, LocalDate date, String route, LocalTime departureTime, int totalSeats, int availableSeats) {
            this.id = id;
            this.date = date;
            this.route = route;
            this.departureTime = departureTime;
            this.totalSeats = totalSeats;
            this.availableSeats = availableSeats;
        }
    }
    
    static class Booking {
        long id;
        String studentId;
        long scheduleId;
        String seatNumber;
        
        Booking(long id, String studentId, long scheduleId, String seatNumber) {
            this.id = id;
            this.studentId = studentId;
            this.scheduleId = scheduleId;
            this.seatNumber = seatNumber;
        }
    }
}